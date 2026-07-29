package io.github.datromtool.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.GameFilterer;
import io.github.datromtool.GameParser;
import io.github.datromtool.GameSorter;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.*;
import io.github.datromtool.domain.datafile.logiqx.Clrmamepro;
import io.github.datromtool.domain.datafile.logiqx.Datafile;
import io.github.datromtool.domain.datafile.logiqx.Game;
import io.github.datromtool.domain.datafile.logiqx.Header;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.domain.retool.CloneList;
import io.github.datromtool.domain.retool.RetoolMetadata;
import io.github.datromtool.exception.ExecutionException;
import io.github.datromtool.exception.InvalidDatafileException;
import io.github.datromtool.exception.WrappedExecutionException;
import io.github.datromtool.io.ArchiveType;
import io.github.datromtool.io.FileCopier;
import io.github.datromtool.io.FileScanner;
import io.github.datromtool.io.ScanResultMatcher;
import io.github.datromtool.retool.CloneListMatcher;
import io.github.datromtool.retool.MetadataEnricher;
import io.github.datromtool.sorting.GameComparator;
import io.github.datromtool.sorting.GameNameComparator;
import java.util.Locale;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@Slf4j
public final class OneGameOneRom {

    private final Filter filter;
    private final PostFilter postFilter;
    private final SortingPreference sortingPreference;
    // Public getter (not just a private field) so a CLI-level test can pin that the mode it
    // parsed is the one that actually reached this constructor, rather than only re-reading the
    // command's own field (which a hardcoded-at-the-call-site regression wouldn't affect).
    @Getter
    private final GameParser.DivergenceDetection divergenceDetection;
    // Issue #19 step 2: optional Retool input sources. Both null (the 4-arg constructor below)
    // reproduces byte-identical pre-issue-#19 behavior; wiring these from CLI/profile options is
    // step 3's job; nothing in `cli` calls the 6-arg constructor yet.
    @Nullable
    private final CloneList cloneList;
    @Nullable
    private final RetoolMetadata retoolMetadata;

    public OneGameOneRom(
            @Nonnull Filter filter,
            @Nonnull PostFilter postFilter,
            @Nonnull SortingPreference sortingPreference,
            @Nonnull GameParser.DivergenceDetection divergenceDetection) {
        this(filter, postFilter, sortingPreference, divergenceDetection, null, null);
    }

    public OneGameOneRom(
            @Nonnull Filter filter,
            @Nonnull PostFilter postFilter,
            @Nonnull SortingPreference sortingPreference,
            @Nonnull GameParser.DivergenceDetection divergenceDetection,
            @Nullable CloneList cloneList,
            @Nullable RetoolMetadata retoolMetadata) {
        this.filter = Objects.requireNonNull(filter, "filter is marked non-null but is null");
        this.postFilter = Objects.requireNonNull(postFilter, "postFilter is marked non-null but is null");
        this.sortingPreference =
                Objects.requireNonNull(sortingPreference, "sortingPreference is marked non-null but is null");
        this.divergenceDetection =
                Objects.requireNonNull(divergenceDetection, "divergenceDetection is marked non-null but is null");
        this.cloneList = cloneList;
        this.retoolMetadata = retoolMetadata;
    }

    public void generate(
            @Nonnull AppConfig appConfig,
            @Nonnull Collection<Datafile> datafiles,
            @Nonnull Collection<Path> inputDirs,
            @Nonnull FileOutputOptions fileOutputOptions,
            @Nonnull List<FileScanner.Listener> fileScannerListeners,
            @Nonnull List<FileCopier.Listener> fileCopierListeners)
            throws InvalidDatafileException, ExecutionException {
        try {
            validate(inputDirs, fileOutputOptions);
            ImmutableList<ParsedGame> parsedGames = applyCloneList(parseGames(datafiles));
            validate(parsedGames);
            ImmutableMap<String, ImmutableList<ParsedGame>> filteredAndGrouped =
                    filterAndGroup(parsedGames);
            ImmutableMap<String, ImmutableList<ScanResultMatcher.GameMatchList>> presentGames =
                    getPresentGames(
                            appConfig,
                            datafiles,
                            inputDirs,
                            fileOutputOptions.archiveType(),
                            fileScannerListeners,
                            filteredAndGrouped);
            ImmutableSet<FileCopier.Spec> specs = createCopySpecs(fileOutputOptions, presentGames);
            FileCopier fileCopier = new FileCopier(appConfig.getCopier(), fileCopierListeners);
            fileCopier.copy(specs);
        } catch (InvalidDatafileException e) {
            throw e;
        } catch (WrappedExecutionException e) {
            throw e.getCause();
        } catch (Exception e) {
            throw new ExecutionException("Unexpected error", e);
        }
    }

    public void generate(
            @Nonnull AppConfig appConfig,
            @Nonnull Collection<Datafile> datafiles,
            @Nullable Collection<Path> inputDirs,
            @Nullable TextOutputOptions textOutputOptions,
            @Nonnull List<FileScanner.Listener> fileScannerListeners,
            @Nonnull Consumer<Collection<String>> textOutputConsumer)
            throws InvalidDatafileException, ExecutionException {
        try {
            validate(textOutputOptions);
            validateDetectors(datafiles, textOutputOptions);
            ImmutableList<ParsedGame> parsedGames = applyCloneList(parseGames(datafiles));
            validate(parsedGames);
            ImmutableMap<String, ImmutableList<ParsedGame>> filteredAndGrouped =
                    filterAndGroup(parsedGames);
            if (inputDirs == null || inputDirs.isEmpty()) {
                sendToOutput(
                        datafiles,
                        textOutputOptions,
                        textOutputConsumer,
                        parsedGameStream(filteredAndGrouped));
            } else {
                ImmutableMap<String, ImmutableList<ScanResultMatcher.GameMatchList>> presentGames =
                        getPresentGames(
                                appConfig,
                                datafiles,
                                inputDirs,
                                null,
                                fileScannerListeners,
                                filteredAndGrouped);
                sendToOutput(
                        datafiles,
                        textOutputOptions,
                        textOutputConsumer,
                        parsedScannedGameStream(presentGames));
            }
        } catch (InvalidDatafileException e) {
            throw e;
        } catch (WrappedExecutionException e) {
            throw e.getCause();
        } catch (Exception e) {
            throw new ExecutionException("Unexpected error", e);
        }
    }

    /**
     * Filters, groups, and post-filters an already clone-list-annotated game list (see
     * {@link #applyCloneList}, which callers run first - issue #19 step 3 moved that step ahead
     * of {@link #validate(Collection)} so that check can see clone-list-assigned groups too; see
     * that method's Javadoc).
     */
    private ImmutableMap<String, ImmutableList<ParsedGame>> filterAndGroup(
            Collection<ParsedGame> parsedGames) {
        GameFilterer gameFilterer = new GameFilterer(filter, postFilter);
        GameComparator comparator = new GameComparator(sortingPreference, cloneList != null);
        GameSorter gameSorter = new GameSorter(comparator);
        ImmutableList<ParsedGame> filtered = gameFilterer.filter(parsedGames);
        ImmutableMap<String, ImmutableList<ParsedGame>> filteredGamesByParent =
                gameSorter.sortAndGroupByParent(filtered);
        return gameFilterer.postFilter(filteredGamesByParent);
    }

    /**
     * Issue #19 step 2: annotates each game with its Retool clone list group/priority (see
     * {@link CloneListMatcher}), when a clone list is present. {@code null} {@link #cloneList}
     * (every run before issue #19, and every run whose {@code cli}/profile input supplied no
     * {@code --clonelist}) short-circuits to an identity copy - byte-identical to pre-issue-#19
     * behavior. Issue #19 step 3 moved the call site of this method ahead of {@link
     * #validate(Collection)} (previously it ran later, inside {@link #filterAndGroup}) - see that
     * method's Javadoc for why.
     *
     * <p>Review round: {@link CloneListMatcher} matches each game independently, purely by its
     * own DAT name - so a clone list title that only matches one member of a DAT-declared
     * parent/clone family (e.g. the family's parent, but not its clone) leaves the rest of that
     * family unmatched. {@link io.github.datromtool.GameSorter#sortAndGroupByParent} then keys
     * the matched member by its (namespaced) clone list group and the unmatched rest by their
     * raw DAT parent name, splitting one family into two 1G1R groups - the unmatched half
     * orphaned from the matched half. {@link #unifyClonelistGroupsAcrossDatFamilies} closes that
     * gap by propagating a family's winning match to every member before grouping ever runs.
     */
    private ImmutableList<ParsedGame> applyCloneList(Collection<ParsedGame> parsedGames) throws IOException {
        if (cloneList == null) {
            return ImmutableList.copyOf(parsedGames);
        }
        RegionData regionData = SerializationHelper.getInstance().loadRegionData();
        CloneListMatcher matcher = new CloneListMatcher(cloneList, regionData, sortingPreference);
        ImmutableList<ParsedGame> matched = parsedGames.stream()
                .flatMap(g -> matcher.match(g)
                        // An ignored group removes its titles from the run entirely, so the game
                        // never reaches grouping, 1G1R selection, or the output.
                        .map(mr -> mr.ignored()
                                ? Stream.<ParsedGame>empty()
                                : Stream.of(g.toBuilder()
                                        .clonelistGroup(mr.group())
                                        .clonelistPriority(mr.priority())
                                        .build()))
                        .orElseGet(() -> Stream.of(g)))
                .collect(ImmutableList.toImmutableList());
        return unifyClonelistGroupsAcrossDatFamilies(matched);
    }

    /**
     * A DAT parent/clone family's winning clone list match (review round), by group name and
     * effective priority - see {@link #unifyClonelistGroupsAcrossDatFamilies}'s Javadoc for how
     * a family's winner is picked when more than one member matched.
     */
    private record FamilyMatch(@Nonnull String group, int priority) {
    }

    /**
     * Propagates a matched clone list group/priority across its whole DAT parent/clone family
     * (review round: partial clone list matches splitting a DAT family - see
     * {@link #applyCloneList}'s Javadoc for the mechanism). Families are computed from the DAT's
     * own parent/clone data ({@link ParsedGame#getParentName()}), independent of any clone list
     * match, so this runs correctly even though some family members may already carry a clone
     * list group assignment from {@link CloneListMatcher} and others may not.
     *
     * <p>A DAT family whose members matched more than one distinct clone list group is possible
     * (a clone list authored loosely, or independent titles each happening to match a different
     * member of the same family). This picks a single winner deterministically, so the outcome
     * never depends on input iteration order: the family's own <b>parent</b>'s match wins
     * outright if the parent matched one; otherwise, the matched group with the lowest (most
     * preferred - upstream's "1 is highest priority" convention) {@link
     * ParsedGame#getClonelistPriority()} wins, ties broken lexicographically by group name. Every
     * family member whose own group differs from the winner (including members that matched
     * nothing at all) is rewritten to the winning group/priority pair; a member that already
     * carries the winning group is left untouched.
     */
    private static ImmutableList<ParsedGame> unifyClonelistGroupsAcrossDatFamilies(
            Collection<ParsedGame> matched) {
        Map<String, List<ParsedGame>> families = matched.stream()
                .collect(Collectors.groupingBy(
                        ParsedGame::getParentName,
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<String, FamilyMatch> winnerByFamily = new HashMap<>();
        for (Map.Entry<String, List<ParsedGame>> entry : families.entrySet()) {
            familyWinner(entry.getValue()).ifPresent(winner -> winnerByFamily.put(entry.getKey(), winner));
        }
        return matched.stream()
                .map(g -> {
                    FamilyMatch winner = winnerByFamily.get(g.getParentName());
                    if (winner == null || winner.group().equals(g.getClonelistGroup())) {
                        return g;
                    }
                    return g.toBuilder()
                            .clonelistGroup(winner.group())
                            .clonelistPriority(winner.priority())
                            .build();
                })
                .collect(ImmutableList.toImmutableList());
    }

    private static Optional<FamilyMatch> familyWinner(List<ParsedGame> family) {
        for (ParsedGame g : family) {
            if (g.isParent() && g.getClonelistGroup() != null) {
                return Optional.of(new FamilyMatch(g.getClonelistGroup(), g.getClonelistPriority()));
            }
        }
        return family.stream()
                .filter(g -> g.getClonelistGroup() != null)
                .map(g -> new FamilyMatch(g.getClonelistGroup(), g.getClonelistPriority()))
                .min(Comparator.comparingInt(FamilyMatch::priority).thenComparing(FamilyMatch::group));
    }

    private static void sendToOutput(
            @Nonnull Collection<Datafile> datafiles,
            @Nullable TextOutputOptions textOutputOptions,
            @Nonnull Consumer<Collection<String>> textOutputConsumer,
            Stream<Stream<ParsedGame>> streamStream) {
        // Use only the header from the first DAT file
        datafiles.stream().findFirst().ifPresent(datafile -> {
            try {
                outputTopItems(
                        datafile,
                        streamStream,
                        textOutputOptions,
                        textOutputConsumer);
            } catch (ExecutionException e) {
                throw new WrappedExecutionException(e);
            }
        });
    }

    private static ImmutableMap<String, ImmutableList<ScanResultMatcher.GameMatchList>> getPresentGames(
            @Nonnull AppConfig appConfig,
            @Nonnull Collection<Datafile> datafiles,
            @Nonnull Collection<Path> inputDirs,
            @Nullable ArchiveType toType,
            @Nonnull List<FileScanner.Listener> fileScannerListeners,
            @Nonnull Map<String, ? extends Collection<ParsedGame>> filteredAndGrouped) {
        ImmutableList<Detector> detectors = loadDetectors(datafiles);
        FileScanner scanner = new FileScanner(
                appConfig.getScanner(),
                datafiles,
                detectors,
                fileScannerListeners);
        ImmutableList<FileScanner.Result> scanResults = scanner.scan(inputDirs);
        ScanResultMatcher matcher = new ScanResultMatcher(scanResults);
        return matcher.match(filteredAndGrouped, toType);
    }

    private static Stream<Stream<ParsedGame>> parsedGameStream(
            Map<String, ? extends Collection<ParsedGame>> map) {
        return map.values().stream().map(Collection::stream);
    }

    private static Stream<Stream<ParsedGame>> parsedScannedGameStream(
            Map<String, ? extends Collection<ScanResultMatcher.GameMatchList>> presentGames) {
        return presentGames.values().stream()
                .map(Collection::stream)
                .map(s -> s.map(ScanResultMatcher.GameMatchList::getParsedGame));
    }

    private ImmutableList<ParsedGame> parseGames(Collection<Datafile> datafiles) throws IOException {
        GameParser gameParser = new GameParser(
                SerializationHelper.getInstance().loadRegionData(),
                divergenceDetection);
        ImmutableList<ParsedGame> parsed = datafiles.stream()
                .map(gameParser::parse)
                .flatMap(Collection::stream)
                .collect(ImmutableList.toImmutableList());
        // Issue #19 step 2: null retoolMetadata (every run before issue #19, and every run in
        // this step since nothing in `cli` wires it up yet) is a no-op copy - see
        // MetadataEnricher's Javadoc.
        return MetadataEnricher.enrich(parsed, retoolMetadata);
    }

    private static void validate(
            @Nullable Collection<Path> inputDirs,
            @Nullable FileOutputOptions fileOutputOptions) {
        if (fileOutputOptions != null
                && fileOutputOptions.outputDir() != null
                && inputDirs != null
                && inputDirs.isEmpty()) {
            throw new IllegalArgumentException(
                    "An output directory requires an input directory");
        }
    }

    private static void validate(@Nullable TextOutputOptions textOutputOptions) {
        if (textOutputOptions != null
                && textOutputOptions.outputMode() == null
                && textOutputOptions.outputFile() == null) {
            throw new IllegalArgumentException(
                    "TextOutputOption must contain at least one non-null value");
        }
    }

    private static void validateDetectors(
            @Nonnull Collection<Datafile> datafiles,
            @Nullable TextOutputOptions textOutputOptions)
            throws InvalidDatafileException {
        if (textOutputOptions != null
                && textOutputOptions.outputMode() != null
                && datafiles.size() > 1
                && detectorsStream(datafiles).distinct().count() > 1) {
            throw new InvalidDatafileException(
                    "Cannot combine multiple DATs with different header detectors");
        }
    }

    /**
     * @param parsedGames already clone-list-annotated (see {@link #applyCloneList}) - a game
     *                     matched into a clone list group ({@link ParsedGame#getClonelistGroup()}
     *                     non-{@code null}) counts as having grouping information even when the
     *                     DAT itself declares none, since issue #19's entire point is that a
     *                     clone list supplies 1G1R grouping for DATs that lack native
     *                     Parent/Clone data (e.g. the Atari 2600 "Air Raiders" cross-region-rename
     *                     group). Before issue #19 step 3, this check ran on the raw,
     *                     pre-clone-list parsed games, which made the clone list feature
     *                     unusable for exactly its intended case - every game in such a DAT is
     *                     {@link ParsedGame#isParent()} (no native clone data at all), so the
     *                     "lack of Parent/Clone information" error fired unconditionally,
     *                     regardless of any clone list supplied.
     */
    private static void validate(Collection<ParsedGame> parsedGames)
            throws InvalidDatafileException {
        if (parsedGames.isEmpty()) {
            throw new InvalidDatafileException(
                    "Cannot generate 1G1R set. Reason: DAT files contain no valid entries");
        }
        if (parsedGames.stream().allMatch(g -> g.isParent() && g.getClonelistGroup() == null)) {
            throw new InvalidDatafileException(
                    "Cannot generate 1G1R set. Reason: DAT files lack Parent/Clone information");
        }
    }

    private static void outputTopItems(
            @Nonnull Datafile datafile,
            @Nonnull Stream<Stream<ParsedGame>> filteredAndGrouped,
            @Nullable TextOutputOptions textOutputOptions,
            @Nonnull Consumer<Collection<String>> textOutputConsumer) throws ExecutionException {
        if (textOutputOptions == null) {
            textOutputConsumer.accept(getSortedGameNames(filteredAndGrouped));
        } else {
            Path outputFile = textOutputOptions.outputFile();
            OutputMode outputMode = textOutputOptions.outputMode();
            if (outputFile == null && outputMode == null) {
                textOutputConsumer.accept(getSortedGameNames(filteredAndGrouped));
            } else if (outputFile == null) {
                textOutputConsumer.accept(getDatOutput(datafile, outputMode, filteredAndGrouped));
            } else if (outputMode == null) {
                writeToOutput(outputFile, getSortedGameNames(filteredAndGrouped));
            } else {
                writeToOutput(outputFile, getDatOutput(datafile, outputMode, filteredAndGrouped));
            }
        }
    }

    private static void writeToOutput(
            @Nonnull Path outputFile,
            @Nonnull ImmutableList<String> datOutput) throws ExecutionException {
        createDirectory(outputFile.getParent());
        try {
            Files.write(outputFile, datOutput);
        } catch (IOException e) {
            throw new ExecutionException(format(
                    "Could not create output file: %s",
                    e.getMessage()), e);
        }
    }

    @Nonnull
    private static ImmutableList<String> getSortedGameNames(
            @Nonnull Stream<Stream<ParsedGame>> filteredAndGrouped) {
        return getTopCandidates(filteredAndGrouped)
                .map(ParsedGame::getGame)
                .map(Game::getName)
                .sorted(GameNameComparator.INSTANCE)
                .collect(ImmutableList.toImmutableList());
    }

    @NonNull
    private static Stream<ParsedGame> getTopCandidates(@NonNull Stream<Stream<ParsedGame>> filteredAndGrouped) {
        return filteredAndGrouped
                .map(Stream::findFirst)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @Nonnull
    private static ImmutableList<String> getDatOutput(
            @Nonnull Datafile datafile,
            @Nonnull OutputMode outputMode,
            @Nonnull Stream<Stream<ParsedGame>> filteredAndGrouped) throws ExecutionException {
        Datafile newDat = prepareDat(datafile, filteredAndGrouped);
        return toOutputRepresentation(outputMode, newDat);
    }

    @Nonnull
    private static Datafile prepareDat(@Nonnull Datafile datafile, Stream<Stream<ParsedGame>> filteredAndGrouped) {
        Stream<Game> gameStream = getTopCandidates(filteredAndGrouped)
                .map(ParsedGame::getGame)
                .sorted(Comparator.comparing(Game::getName, GameNameComparator.INSTANCE))
                .map(g -> g.withCloneOf(null));
        Header header = datafile.getHeader();
        ImmutableList<Game> games = gameStream.collect(ImmutableList.toImmutableList());
        if (header != null) {
            header = header.withName(format("%s (1G1R)", header.getName()))
                    .withDescription(format("%s (1G1R)", header.getDescription()));
        }
        return datafile.withHeader(header).withGames(games);
    }

    @Nonnull
    private static ImmutableList<String> toOutputRepresentation(
            @Nonnull OutputMode outputMode,
            @Nonnull Datafile newDat) throws ExecutionException {
        try {
            SerializationHelper helper = SerializationHelper.getInstance();
            return switch (outputMode) {
                case XML -> helper.writeAsXml(newDat);
                case JSON -> helper.writeAsJson(newDat);
                case YAML -> helper.writeAsYaml(newDat);
            };
        } catch (JacksonException e) {
            throw new ExecutionException(
                    format("Could not write to output file: %s", e.getMessage()),
                    e);
        }
    }

    private static void createDirectory(@Nullable Path path) throws ExecutionException {
        if (path != null) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new ExecutionException(
                        format("Could not create output directory: %s", e.getMessage()),
                        e);
            }
        }
    }

    private static Stream<String> detectorsStream(Collection<Datafile> datafiles) {
        return datafiles.stream()
                .map(Datafile::getHeader)
                .filter(Objects::nonNull)
                .map(Header::getClrmamepro)
                .filter(Objects::nonNull)
                .map(Clrmamepro::headerFile)
                .filter(Objects::nonNull);
    }

    private static ImmutableSet<FileCopier.Spec> createCopySpecs(
            @Nonnull FileOutputOptions fileOutputOptions,
            @Nonnull Map<String, ? extends List<ScanResultMatcher.GameMatchList>> presentGames) {
        return presentGames.values().stream()
                .map(Collection::stream)
                .map(Stream::findFirst)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(gameMatchList -> buildSpecStream(fileOutputOptions, gameMatchList))
                .collect(ImmutableSet.toImmutableSet());
    }

    private static Stream<FileCopier.Spec> buildSpecStream(
            @Nonnull FileOutputOptions fileOutputOptions,
            @Nonnull ScanResultMatcher.GameMatchList gameMatchList) {
        try {
            Game game = gameMatchList.getParsedGame().getGame();
            // TODO: implement category-based subfolders (region, type, name, etc.)
            // TODO: what if it belongs to multiple regions? What about World?
            Path baseDir = createBaseDirectory(game, fileOutputOptions);
            ImmutableList<ScanResultMatcher.RomMatch> matches =
                    gameMatchList.getRomMatches();
            Map<Path, List<ScanResultMatcher.RomMatch>> perFile = matches.stream()
                    .collect(Collectors.groupingBy(p -> p.getResult().getPath()));
            ArchiveType toType = fileOutputOptions.archiveType();
            if (toType == null) {
                // Simple copy/extraction
                return simpleCopyOrExtractionStream(baseDir, perFile);
            } else {
                // Compression/archive copy
                return compressionOrArchiveCopyStream(
                        game,
                        baseDir,
                        matches,
                        perFile,
                        toType);
            }
        } catch (ExecutionException e) {
            throw new WrappedExecutionException(e);
        }
    }

    private static Stream<FileCopier.Spec> simpleCopyOrExtractionStream(
            Path baseDir,
            Map<Path, ? extends Collection<ScanResultMatcher.RomMatch>> matchesPerFile) {
        return matchesPerFile.entrySet().stream()
                .flatMap(e -> {
                    Path from = e.getKey();
                    Collection<ScanResultMatcher.RomMatch> list = e.getValue();
                    ArchiveType fromType = list.stream()
                            .map(ScanResultMatcher.RomMatch::getResult)
                            .map(FileScanner.Result::getArchiveType)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);
                    if (fromType == null) {
                        // Simple copy
                        return list.stream()
                                .map(ScanResultMatcher.RomMatch::getRom)
                                .map(rom -> FileCopier.CopySpec.builder()
                                        .from(from)
                                        .to(baseDir.resolve(rom.name()))
                                        .build());
                    } else {
                        // Extraction
                        return Stream.of(FileCopier.ExtractionSpec.builder()
                                .from(from)
                                .fromType(fromType)
                                .internalSpecs(list.stream()
                                        .map(m -> FileCopier.ExtractionSpec.InternalSpec.builder()
                                                .from(m.getResult().getArchivePath())
                                                .to(baseDir.resolve(m.getRom().name()))
                                                .build())
                                        .collect(ImmutableMap.toImmutableMap(FileCopier.ExtractionSpec.InternalSpec::getFrom, Function.identity(), (a, _) -> a)))
                                .build());
                    }
                });
    }

    private static Stream<FileCopier.Spec> compressionOrArchiveCopyStream(
            Game game,
            Path baseDir,
            Collection<ScanResultMatcher.RomMatch> matches,
            Map<Path, ? extends Collection<ScanResultMatcher.RomMatch>> matchesPerFile,
            ArchiveType toType) {
        Path to = baseDir.resolve(format(
                "%s.%s",
                game.getName(),
                toType.getAlias()));
        Stream<FileCopier.ArchiveCopySpec> archiveCopies =
                buildArchiveCopySpecs(matchesPerFile, toType, to);
        Stream<FileCopier.CompressionSpec> compressions =
                buildCompressionSpecs(matches, toType, to);
        return Stream.concat(archiveCopies, compressions);
    }

    @Nonnull
    private static Stream<FileCopier.ArchiveCopySpec> buildArchiveCopySpecs(
            Map<Path, ? extends Collection<ScanResultMatcher.RomMatch>> matchesPerFile,
            ArchiveType toType,
            Path to) {
        return matchesPerFile
                .entrySet().stream()
                .map(e -> buildArchiveCopySpec(toType, to, e))
                .filter(Objects::nonNull);
    }

    @Nonnull
    private static Stream<FileCopier.CompressionSpec> buildCompressionSpecs(
            Collection<ScanResultMatcher.RomMatch> matches,
            ArchiveType toType,
            Path to) {
        List<ScanResultMatcher.RomMatch> forCompression = matches
                .stream()
                .filter(m -> m.getResult().getArchiveType() == null)
                .toList();
        Stream<FileCopier.CompressionSpec> compressions;
        if (forCompression.isEmpty()) {
            compressions = Stream.empty();
        } else {
            // Only compression
            compressions = Stream.of(FileCopier.CompressionSpec.builder()
                    .to(to)
                    .toType(toType)
                    .internalSpecs(forCompression.stream()
                            .map(p -> FileCopier.CompressionSpec.InternalSpec.builder()
                                    .from(p.getResult().getPath())
                                    .to(p.getRom().name())
                                    .build())
                            .collect(ImmutableSet.toImmutableSet()))
                    .build());
        }
        return compressions;
    }

    @Nullable
    private static FileCopier.ArchiveCopySpec buildArchiveCopySpec(
            ArchiveType toType,
            Path to,
            Map.Entry<Path, ? extends Collection<ScanResultMatcher.RomMatch>> e) {
        Path from = e.getKey();
        Collection<ScanResultMatcher.RomMatch> list = e.getValue();
        ArchiveType fromType = list.stream()
                .map(ScanResultMatcher.RomMatch::getResult)
                .map(FileScanner.Result::getArchiveType)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        // Skip compressions
        if (fromType == null) {
            return null;
        }
        // Only archive copy
        return FileCopier.ArchiveCopySpec.builder()
                .from(from)
                .fromType(fromType)
                .to(to)
                .toType(toType)
                .internalSpecs(list.stream()
                        .map(m -> FileCopier.ArchiveCopySpec.InternalSpec.builder()
                                .from(m.getResult().getArchivePath())
                                .to(m.getRom().name())
                                .build())
                        .collect(ImmutableMap.toImmutableMap(FileCopier.ArchiveCopySpec.InternalSpec::getFrom, Function.identity(), (a, _) -> a)))
                .build();
    }

    @Nonnull
    private static Path createBaseDirectory(
            @Nonnull Game game,
            @Nonnull FileOutputOptions fileOutputOptions) throws ExecutionException {
        Path baseDir = fileOutputOptions.outputDir();
        if (fileOutputOptions.alphabetic()) {
            char firstLetter = game.getName().toLowerCase(Locale.ROOT).charAt(0);
            if (firstLetter >= 'a' && firstLetter <= 'z') {
                baseDir = baseDir.resolve(String.valueOf(firstLetter));
            } else {
                baseDir = baseDir.resolve("#");
            }
        }
        if (fileOutputOptions.forceSubfolder() || game.getRoms().size() > 1) {
            baseDir = baseDir.resolve(game.getName());
        }
        createDirectory(baseDir);
        return baseDir;
    }

    private static ImmutableList<Detector> loadDetectors(Collection<Datafile> datafiles) {
        return detectorsStream(datafiles)
                .map(OneGameOneRom::loadDetectorAndWrapException)
                .collect(ImmutableList.toImmutableList());
    }

    private static Detector loadDetectorAndWrapException(String name) {
        try {
            return loadDetector(name);
        } catch (ExecutionException e) {
            throw new WrappedExecutionException(e);
        }
    }

    private static Detector loadDetector(String name) throws ExecutionException {
        try {
            return SerializationHelper.getInstance().loadDetector(name);
        } catch (NoSuchFileException e) {
            throw new ExecutionException(
                    format(
                            "Could not load detector file: File not found: %s",
                            e.getMessage()),
                    e);
        } catch (Exception e) {
            throw new ExecutionException(
                    format(
                            "Could not load detector file: %s: %s",
                            e.getClass().getSimpleName(),
                            e.getMessage()),
                    e);
        }
    }

}
