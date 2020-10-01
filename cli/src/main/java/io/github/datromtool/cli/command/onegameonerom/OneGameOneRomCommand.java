package io.github.datromtool.cli.command.onegameonerom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.GameFilterer;
import io.github.datromtool.GameParser;
import io.github.datromtool.GameSorter;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.cli.GitVersionProvider;
import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.command.onegameonerom.InputOutputOptions.OutputOptions;
import io.github.datromtool.cli.option.FilteringOptions;
import io.github.datromtool.cli.option.PostFilteringOptions;
import io.github.datromtool.cli.option.SortingOptions;
import io.github.datromtool.cli.option.TextOptions;
import io.github.datromtool.cli.progressbar.CommandLineCopierProgressBar;
import io.github.datromtool.cli.progressbar.CommandLineScannerProgressBar;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.domain.datafile.Clrmamepro;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.datafile.Game;
import io.github.datromtool.domain.datafile.Header;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.io.ArchiveType;
import io.github.datromtool.io.FileCopier;
import io.github.datromtool.io.FileScanner;
import io.github.datromtool.io.OutputMode;
import io.github.datromtool.io.ScanResultMatcher;
import io.github.datromtool.sorting.GameComparator;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.NONE;

@Data
@NoArgsConstructor
@JsonInclude(NON_DEFAULT)
@CommandLine.Command(
        name = "1g1r",
        description = "Operate in 1G1R mode",
        sortOptions = false,
        abbreviateSynopsis = true,
        versionProvider = GitVersionProvider.class,
        mixinStandardHelpOptions = true)
public final class OneGameOneRomCommand implements Callable<Integer> {

    @CommandLine.Spec
    @JsonIgnore
    @Getter(NONE)
    @Setter(NONE)
    private CommandLine.Model.CommandSpec commandSpec;

    @CommandLine.Parameters(
            description = "DAT file to use when generating the 1G1R set",
            arity = "1..*",
            paramLabel = "DAT_FILE")
    private List<DatafileArgument> datafiles = ImmutableList.of();

    @CommandLine.ArgGroup(heading = "Input options\n", exclusive = false)
    private InputOutputOptions inputOutputOptions;

    @CommandLine.ArgGroup(heading = "Text output options\n", exclusive = false)
    private TextOptions textOptions;

    @CommandLine.ArgGroup(heading = "Filtering options\n", exclusive = false)
    private FilteringOptions filteringOptions;

    @CommandLine.ArgGroup(heading = "Post-filtering options\n", exclusive = false)
    private PostFilteringOptions postFilteringOptions;

    @CommandLine.ArgGroup(heading = "Sorting options\n", exclusive = false)
    private SortingOptions sortingOptions;

    @Override
    public Integer call() throws Exception {
        if (textOptions != null && outputOptions().isPresent()) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    "Cannot use text and file output options at the same time");
        }
        ArchiveType toType = fileGroupingOptions()
                .map(OutputOptions.GroupingOptions::getArchiveType)
                .orElse(null);
        Path outputDir = outputOptions()
                .map(OutputOptions::getOutputDir)
                .orElse(null);
        Filter filter = filteringOptions != null
                ? filteringOptions.toFilter()
                : Filter.builder().build();
        PostFilter postFilter = postFilteringOptions != null
                ? postFilteringOptions.toPostFilter()
                : PostFilter.builder().build();
        SortingPreference sortingPreference = sortingOptions != null
                ? sortingOptions.toSortingPreference()
                : SortingPreference.builder().build();
        if (textOptions != null
                && textOptions.getOutputMode() != null
                && datafiles.size() > 1
                && detectorsStream(datafiles).distinct().count() > 1) {
            throw new CommandLine.ExecutionException(
                    commandSpec.commandLine(),
                    "Cannot combine multiple DATs with different header detectors");
        }
        GameParser gameParser = new GameParser(
                SerializationHelper.getInstance().loadRegionData(),
                GameParser.DivergenceDetection.ONE_WAY);
        ImmutableList<ParsedGame> parsedGames = datafiles.stream()
                .map(DatafileArgument::getDatafile)
                .map(gameParser::parse)
                .flatMap(Collection::stream)
                .collect(ImmutableList.toImmutableList());
        if (parsedGames.isEmpty()) {
            throw new CommandLine.ExecutionException(
                    commandSpec.commandLine(),
                    "Cannot generate 1G1R set. Reason: DAT files contain no valid entries");
        }
        if (parsedGames.stream().allMatch(ParsedGame::isParent)) {
            throw new CommandLine.ExecutionException(
                    commandSpec.commandLine(),
                    "Cannot generate 1G1R set. Reason: DAT files lack Parent/Clone information");
        }
        GameFilterer gameFilterer = new GameFilterer(filter, postFilter);
        GameSorter gameSorter = new GameSorter(new GameComparator(sortingPreference));
        ImmutableList<ParsedGame> filtered = gameFilterer.filter(parsedGames);
        ImmutableMap<String, ImmutableList<ParsedGame>> filteredGamesByParent =
                gameSorter.sortAndGroupByParent(filtered);
        ImmutableMap<String, ImmutableList<ParsedGame>> postFilteredGamesByParent =
                gameFilterer.postFilter(filteredGamesByParent);
        if (inputOutputOptions == null && textOptions == null) {
            printTopItems(
                    parsedGameStream(postFilteredGamesByParent),
                    null);
        } else if (inputOutputOptions == null) {
            Path outputFile = textOptions.getOutputFile();
            OutputMode mode = textOptions.getOutputMode();
            if (mode == null) {
                printTopItems(parsedGameStream(postFilteredGamesByParent), outputFile);
            } else {
                datafiles.stream()
                        .findFirst()
                        .map(DatafileArgument::getDatafile)
                        .ifPresent(datafile -> printTopItems(
                                datafile,
                                mode,
                                parsedGameStream(postFilteredGamesByParent),
                                outputFile));
            }
        } else if (!inputOutputOptions.getInputDirs().isEmpty()) {
            ImmutableList<Detector> detectors = loadDetectors(datafiles);
            AppConfig appConfig = SerializationHelper.getInstance().loadAppConfig();
            FileScanner scanner = new FileScanner(
                    appConfig,
                    datafiles.stream()
                            .map(DatafileArgument::getDatafile)
                            .collect(Collectors.toList()),
                    detectors,
                    new CommandLineScannerProgressBar());
            ImmutableList<FileScanner.Result> scanResults =
                    scanner.scan(inputOutputOptions.getInputDirs());
            ScanResultMatcher matcher = new ScanResultMatcher(scanResults);
            ImmutableMap<String, ImmutableList<ScanResultMatcher.GameMatchList>> presentGames =
                    matcher.match(postFilteredGamesByParent, toType);
            if (outputDir == null) {
                if (textOptions == null) {
                    printTopItems(parsedScannedGameStream(presentGames), null);
                } else {
                    Path outputFile = textOptions.getOutputFile();
                    OutputMode mode = textOptions.getOutputMode();
                    if (mode == null) {
                        printTopItems(parsedScannedGameStream(presentGames), outputFile);
                    } else {
                        datafiles.stream()
                                .findFirst()
                                .map(DatafileArgument::getDatafile)
                                .ifPresent(datafile -> printTopItems(
                                        datafile,
                                        mode,
                                        parsedScannedGameStream(presentGames),
                                        outputFile));
                    }
                }
            } else {
                boolean isAlphabetic = outputOptions()
                        .map(OutputOptions::isAlphabetic)
                        .orElse(false);
                boolean isForceSubfolder = fileGroupingOptions()
                        .map(OutputOptions.GroupingOptions::isForceSubfolder)
                        .orElse(false);
                ImmutableSet<FileCopier.Spec> specs = createCopySpecs(
                        toType,
                        presentGames,
                        outputDir,
                        isAlphabetic,
                        isForceSubfolder);
                FileCopier fileCopier =
                        new FileCopier(appConfig, false, new CommandLineCopierProgressBar());
                fileCopier.copy(specs);
            }
        }
        return 0;
    }

    private Stream<Stream<ParsedGame>> parsedScannedGameStream(
            Map<String, ? extends Collection<ScanResultMatcher.GameMatchList>> presentGames) {
        return presentGames.values().stream()
                .map(Collection::stream)
                .map(s -> s.map(ScanResultMatcher.GameMatchList::getParsedGame));
    }

    private Stream<Stream<ParsedGame>> parsedGameStream(
            Map<String, ? extends Collection<ParsedGame>> map) {
        return map.values().stream().map(Collection::stream);
    }

    private Optional<OutputOptions> outputOptions() {
        return Optional.ofNullable(inputOutputOptions)
                .map(InputOutputOptions::getOutputOptions);
    }

    private Optional<OutputOptions.GroupingOptions> fileGroupingOptions() {
        return outputOptions()
                .map(OutputOptions::getGroupingOptions);
    }

    private void printTopItems(
            @Nonnull Datafile datafile,
            @Nonnull OutputMode mode,
            @Nonnull Stream<Stream<ParsedGame>> parsedGameStream,
            @Nullable Path outputFile) {
        Stream<Game> gameStream = parsedGameStream
                .map(Stream::findFirst)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ParsedGame::getGame)
                .sorted(Comparator.comparing(Game::getName))
                .map(g -> g.withCloneOf(null));
        Header header = datafile.getHeader();
        ImmutableList<Game> games = gameStream.collect(ImmutableList.toImmutableList());
        if (header != null) {
            header = header.withName(String.format("%s (1G1R)", header.getName()))
                    .withDescription(String.format("%s (1G1R)", header.getDescription()));
        }
        Datafile newDat = datafile.withHeader(header).withGames(games);
        ImmutableList<String> output;
        try {
            SerializationHelper helper = SerializationHelper.getInstance();
            switch (mode) {
                case XML:
                    output = helper.writeAsXml(newDat);
                    break;
                case JSON:
                    output = ImmutableList.of(helper.getJsonMapper().writeValueAsString(newDat));
                    break;
                case YAML:
                    output = helper.writeAsYaml(newDat);
                    break;
                default:
                    throw new IllegalArgumentException(String.format(
                            "Cannot handle mode %s",
                            mode));
            }
        } catch (JsonProcessingException e) {
            throw new CommandLine.ExecutionException(
                    commandSpec.commandLine(),
                    String.format("Could not write to output file: %s", e.getMessage()),
                    e);
        }
        if (outputFile == null) {
            output.forEach(System.out::println);
        } else {
            createDirectory(outputFile.getParent());
            try {
                Files.write(outputFile, output);
            } catch (IOException e) {
                throw new CommandLine.ExecutionException(
                        commandSpec.commandLine(),
                        String.format("Could not write to output file: %s", e.getMessage()),
                        e);
            }
        }
    }

    private void printTopItems(
            @Nonnull Stream<Stream<ParsedGame>> postFilteredGamesByParent,
            @Nullable Path outputFile) {
        Stream<String> gameStream = postFilteredGamesByParent
                .map(Stream::findFirst)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ParsedGame::getGame)
                .map(Game::getName)
                .sorted();
        if (outputFile == null) {
            gameStream.forEach(System.out::println);
        } else {
            createDirectory(outputFile.getParent());
            try {
                Files.write(outputFile, gameStream.collect(Collectors.toList()));
            } catch (IOException e) {
                throw new CommandLine.ExecutionException(
                        commandSpec.commandLine(),
                        String.format("Could not create output file: %s", e.getMessage()),
                        e);
            }
        }
    }

    private ImmutableSet<FileCopier.Spec> createCopySpecs(
            ArchiveType toType,
            ImmutableMap<String, ImmutableList<ScanResultMatcher.GameMatchList>> presentGames,
            Path outputDir,
            boolean isAlphabetic,
            boolean isForceSubfolder) {
        return presentGames.values().stream()
                .map(Collection::stream)
                .map(Stream::findFirst)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(gameMatchList -> {
                    Game game = gameMatchList.getParsedGame().getGame();
                    Path baseDir =
                            createBaseDirectory(game, outputDir, isAlphabetic, isForceSubfolder);
                    ImmutableList<ScanResultMatcher.RomMatch> matches =
                            gameMatchList.getRomMatches();
                    Map<Path, List<ScanResultMatcher.RomMatch>> perFile = matches.stream()
                            .collect(Collectors.groupingBy(p -> p.getResult().getPath()));
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
                }).collect(ImmutableSet.toImmutableSet());
    }

    private Stream<FileCopier.Spec> simpleCopyOrExtractionStream(
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
                                        .to(baseDir.resolve(rom.getName()))
                                        .build());
                    } else {
                        // Extraction
                        return Stream.of(FileCopier.ExtractionSpec.builder()
                                .from(from)
                                .fromType(fromType)
                                .internalSpecs(list.stream()
                                        .map(m -> FileCopier.ExtractionSpec.InternalSpec.builder()
                                                .from(m.getResult().getArchivePath())
                                                .to(baseDir.resolve(m.getRom().getName()))
                                                .build())
                                        .collect(ImmutableSet.toImmutableSet()))
                                .build());
                    }
                });
    }

    private Stream<FileCopier.Spec> compressionOrArchiveCopyStream(
            Game game,
            Path baseDir,
            Collection<ScanResultMatcher.RomMatch> matches,
            Map<Path, ? extends Collection<ScanResultMatcher.RomMatch>> matchesPerFile,
            ArchiveType toType) {
        Path to = baseDir.resolve(String.format(
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
                .collect(Collectors.toList());
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
                                    .to(p.getRom().getName())
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
                                .to(m.getRom().getName())
                                .build())
                        .collect(ImmutableSet.toImmutableSet()))
                .build();
    }

    private Path createBaseDirectory(
            Game game,
            Path baseDir,
            boolean isAlphabetic,
            boolean isForceSubfolder) {
        if (isAlphabetic) {
            char firstLetter = game.getName().toLowerCase().charAt(0);
            if (firstLetter >= 'a' && firstLetter <= 'z') {
                baseDir = baseDir.resolve(String.valueOf(firstLetter));
            } else {
                baseDir = baseDir.resolve("#");
            }
        }
        if (isForceSubfolder || game.getRoms().size() > 1) {
            baseDir = baseDir.resolve(game.getName());
        }
        createDirectory(baseDir);
        return baseDir;
    }

    private void createDirectory(@Nullable Path path) {
        try {
            if (path != null) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new CommandLine.ExecutionException(
                    commandSpec.commandLine(),
                    String.format(
                            "Could not create output directory: %s",
                            e.getMessage()),
                    e);
        }
    }

    private ImmutableList<Detector> loadDetectors(List<DatafileArgument> datafiles) {
        return detectorsStream(datafiles)
                .map(name -> {
                    try {
                        return SerializationHelper.getInstance().loadDetector(name);
                    } catch (NoSuchFileException e) {
                        throw new CommandLine.ParameterException(
                                commandSpec.commandLine(),
                                String.format(
                                        "Could not load detector file: File not found: %s",
                                        e.getMessage()),
                                e);
                    } catch (Exception e) {
                        throw new CommandLine.ExecutionException(
                                commandSpec.commandLine(),
                                String.format(
                                        "Could not load detector file: %s: %s",
                                        e.getClass().getSimpleName(),
                                        e.getMessage()),
                                e);
                    }
                }).collect(ImmutableList.toImmutableList());
    }

    private static Stream<String> detectorsStream(List<DatafileArgument> datafiles) {
        return datafiles.stream()
                .map(DatafileArgument::getDatafile)
                .map(Datafile::getHeader)
                .filter(Objects::nonNull)
                .map(Header::getClrmamepro)
                .filter(Objects::nonNull)
                .map(Clrmamepro::getHeaderFile)
                .filter(Objects::nonNull);
    }

}
