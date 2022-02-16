package io.github.datromtool.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.GameFilterer;
import io.github.datromtool.GameParser;
import io.github.datromtool.GameSorter;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.*;
import io.github.datromtool.domain.datafile.Clrmamepro;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.datafile.Game;
import io.github.datromtool.domain.datafile.Header;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.exception.ExecutionException;
import io.github.datromtool.exception.InvalidDatafileException;
import io.github.datromtool.exception.WrappedExecutionException;
import io.github.datromtool.io.ArchiveType;
import io.github.datromtool.io.FileCopier;
import io.github.datromtool.io.FileScanner;
import io.github.datromtool.io.ScanResultMatcher;
import io.github.datromtool.sorting.GameComparator;
import io.github.datromtool.sorting.GameNameComparator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public final class OneGameOneRom {

    @NonNull
    private final Filter filter;
    @NonNull
    private final PostFilter postFilter;
    @NonNull
    private final SortingPreference sortingPreference;

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
            ImmutableList<ParsedGame> parsedGames = parseGames(datafiles);
            validate(parsedGames);
            ImmutableMap<String, ImmutableList<ParsedGame>> filteredAndGrouped =
                    filterAndGroup(parsedGames);
            ImmutableMap<String, ImmutableList<ScanResultMatcher.GameMatchList>> presentGames =
                    getPresentGames(
                            appConfig,
                            datafiles,
                            inputDirs,
                            fileOutputOptions.getArchiveType(),
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
            ImmutableList<ParsedGame> parsedGames = parseGames(datafiles);
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

    private ImmutableMap<String, ImmutableList<ParsedGame>> filterAndGroup(
            Collection<ParsedGame> parsedGames) {
        GameFilterer gameFilterer = new GameFilterer(filter, postFilter);
        GameSorter gameSorter = new GameSorter(new GameComparator(sortingPreference));
        ImmutableList<ParsedGame> filtered = gameFilterer.filter(parsedGames);
        ImmutableMap<String, ImmutableList<ParsedGame>> filteredGamesByParent =
                gameSorter.sortAndGroupByParent(filtered);
        return gameFilterer.postFilter(filteredGamesByParent);
    }

    private static void sendToOutput(
            @Nonnull Collection<Datafile> datafiles,
            @Nullable TextOutputOptions textOutputOptions,
            @Nonnull Consumer<Collection<String>> textOutputConsumer,
            Stream<Stream<ParsedGame>> streamStream) {
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

    private static ImmutableList<ParsedGame> parseGames(Collection<Datafile> datafiles) throws IOException {
        GameParser gameParser = new GameParser(
                SerializationHelper.getInstance().loadRegionData(),
                GameParser.DivergenceDetection.ONE_WAY);
        return datafiles.stream()
                .map(gameParser::parse)
                .flatMap(Collection::stream)
                .collect(ImmutableList.toImmutableList());
    }

    private static void validate(
            @Nullable Collection<Path> inputDirs,
            @Nullable FileOutputOptions fileOutputOptions) {
        if (fileOutputOptions != null
                && fileOutputOptions.getOutputDir() != null
                && inputDirs != null
                && inputDirs.isEmpty()) {
            throw new IllegalArgumentException(
                    "An output directory requires an input directory");
        }
    }

    private static void validate(@Nullable TextOutputOptions textOutputOptions) {
        if (textOutputOptions != null
                && textOutputOptions.getOutputMode() == null
                && textOutputOptions.getOutputFile() == null) {
            throw new IllegalArgumentException(
                    "TextOutputOption must contain at least one non-null value");
        }
    }

    private static void validateDetectors(
            @Nonnull Collection<Datafile> datafiles,
            @Nullable TextOutputOptions textOutputOptions)
            throws InvalidDatafileException {
        if (textOutputOptions != null
                && textOutputOptions.getOutputMode() != null
                && datafiles.size() > 1
                && detectorsStream(datafiles).distinct().count() > 1) {
            throw new InvalidDatafileException(
                    "Cannot combine multiple DATs with different header detectors");
        }
    }

    private static void validate(Collection<ParsedGame> parsedGames)
            throws InvalidDatafileException {
        if (parsedGames.isEmpty()) {
            throw new InvalidDatafileException(
                    "Cannot generate 1G1R set. Reason: DAT files contain no valid entries");
        }
        if (parsedGames.stream().allMatch(ParsedGame::isParent)) {
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
        } else if (textOutputOptions.getOutputMode() == null) {
            writeToOutput(textOutputOptions.getOutputFile(), getSortedGameNames(filteredAndGrouped));
        } else if (textOutputOptions.getOutputFile() == null) {
            textOutputConsumer.accept(getDatOutput(datafile, textOutputOptions.getOutputMode(), filteredAndGrouped));
        } else {
            writeToOutput(
                    textOutputOptions.getOutputFile(),
                    getDatOutput(datafile, textOutputOptions.getOutputMode(), filteredAndGrouped));
        }
    }

    private static void writeToOutput(
            @Nonnull Path outputFile,
            @Nonnull ImmutableList<String> datOutput) throws ExecutionException {
        createDirectory(outputFile.getParent());
        try {
            Files.write(outputFile, datOutput);
        } catch (IOException e) {
            throw new ExecutionException(String.format(
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
        Stream<Game> gameStream = getTopCandidates(filteredAndGrouped)
                .map(ParsedGame::getGame)
                .sorted(Comparator.comparing(Game::getName, GameNameComparator.INSTANCE))
                .map(g -> g.withCloneOf(null));
        Header header = datafile.getHeader();
        ImmutableList<Game> games = gameStream.collect(ImmutableList.toImmutableList());
        if (header != null) {
            header = header.withName(String.format("%s (1G1R)", header.getName()))
                    .withDescription(String.format("%s (1G1R)", header.getDescription()));
        }
        Datafile newDat = datafile.withHeader(header).withGames(games);
        try {
            SerializationHelper helper = SerializationHelper.getInstance();
            switch (outputMode) {
                case XML:
                    return helper.writeAsXml(newDat);
                case JSON:
                    return helper.writeAsJson(newDat);
                case YAML:
                    return helper.writeAsYaml(newDat);
                default:
                    throw new IllegalArgumentException(String.format(
                            "Cannot handle mode %s",
                            outputMode));
            }
        } catch (JsonProcessingException e) {
            throw new ExecutionException(
                    String.format("Could not write to output file: %s", e.getMessage()),
                    e);
        }
    }

    private static void createDirectory(@Nullable Path path) throws ExecutionException {
        if (path != null) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new ExecutionException(
                        String.format("Could not create output directory: %s", e.getMessage()),
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
                .map(Clrmamepro::getHeaderFile)
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
            Path baseDir = createBaseDirectory(game, fileOutputOptions);
            ImmutableList<ScanResultMatcher.RomMatch> matches =
                    gameMatchList.getRomMatches();
            Map<Path, List<ScanResultMatcher.RomMatch>> perFile = matches.stream()
                    .collect(Collectors.groupingBy(p -> p.getResult().getPath()));
            ArchiveType toType = fileOutputOptions.getArchiveType();
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

    private static Stream<FileCopier.Spec> compressionOrArchiveCopyStream(
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

    @Nonnull
    private static Path createBaseDirectory(
            @Nonnull Game game,
            @Nonnull FileOutputOptions fileOutputOptions) throws ExecutionException {
        Path baseDir = fileOutputOptions.getOutputDir();
        if (fileOutputOptions.isAlphabetic()) {
            char firstLetter = game.getName().toLowerCase().charAt(0);
            if (firstLetter >= 'a' && firstLetter <= 'z') {
                baseDir = baseDir.resolve(String.valueOf(firstLetter));
            } else {
                baseDir = baseDir.resolve("#");
            }
        }
        if (fileOutputOptions.isForceSubfolder() || game.getRoms().size() > 1) {
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
                    String.format(
                            "Could not load detector file: File not found: %s",
                            e.getMessage()),
                    e);
        } catch (Exception e) {
            throw new ExecutionException(
                    String.format(
                            "Could not load detector file: %s: %s",
                            e.getClass().getSimpleName(),
                            e.getMessage()),
                    e);
        }
    }

}
