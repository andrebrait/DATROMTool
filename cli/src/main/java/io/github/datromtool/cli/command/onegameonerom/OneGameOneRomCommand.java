package io.github.datromtool.cli.command.onegameonerom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.GameFilterer;
import io.github.datromtool.GameParser;
import io.github.datromtool.GameSorter;
import io.github.datromtool.Patterns;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.cli.GitVersionProvider;
import io.github.datromtool.cli.option.FilteringOptions;
import io.github.datromtool.cli.option.PostFilteringOptions;
import io.github.datromtool.cli.option.SortingOptions;
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
import io.github.datromtool.io.ScanResultMatcher;
import io.github.datromtool.sorting.GameComparator;
import io.github.datromtool.util.ArgumentException;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static io.github.datromtool.cli.command.onegameonerom.InputOutputOptions.ARCHIVE_FORMAT_OPTION;
import static io.github.datromtool.cli.command.onegameonerom.InputOutputOptions.FORCE_SUBFOLDER_OPTION;
import static io.github.datromtool.cli.command.onegameonerom.InputOutputOptions.GROUP_BY_FIRST_LETTER_OPTION;
import static io.github.datromtool.cli.command.onegameonerom.InputOutputOptions.INPUT_DIR_OPTION;
import static io.github.datromtool.cli.command.onegameonerom.InputOutputOptions.OUTPUT_DIR_OPTION;
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
    private List<Path> datFiles = ImmutableList.of();

    @CommandLine.ArgGroup(heading = "Input/output options\n", exclusive = false)
    private InputOutputOptions inputOutputOptions;

    @CommandLine.ArgGroup(heading = "Filtering options\n", exclusive = false)
    private FilteringOptions filteringOptions;

    @CommandLine.ArgGroup(heading = "Post-filtering options\n", exclusive = false)
    private PostFilteringOptions postFilteringOptions;

    @CommandLine.ArgGroup(heading = "Sorting options\n", exclusive = false)
    private SortingOptions sortingOptions;

    @Override
    public Integer call() throws Exception {
        ArchiveType toType = inputOutputOptions.getArchiveType();
        if (inputOutputOptions != null) {
            if (inputOutputOptions.getOutputDir() != null
                    && inputOutputOptions.getInputDirs().isEmpty()) {
                throw new CommandLine.ParameterException(
                        commandSpec.commandLine(),
                        String.format(
                                "Option %s requires %s",
                                OUTPUT_DIR_OPTION,
                                INPUT_DIR_OPTION));
            }
            if (inputOutputOptions.getOutputDir() == null && inputOutputOptions.isGroupByFirstLetter()) {
                throw new CommandLine.ParameterException(
                        commandSpec.commandLine(),
                        String.format(
                                "Option %s requires %s",
                                GROUP_BY_FIRST_LETTER_OPTION,
                                OUTPUT_DIR_OPTION));
            }
            if (toType != null) {
                if (inputOutputOptions.getOutputDir() == null) {
                    throw new CommandLine.ParameterException(
                            commandSpec.commandLine(),
                            String.format(
                                    "Option %s requires %s",
                                    ARCHIVE_FORMAT_OPTION,
                                    OUTPUT_DIR_OPTION));
                }
                if (inputOutputOptions.isForceSubfolder()) {
                    throw new CommandLine.ParameterException(
                            commandSpec.commandLine(),
                            String.format(
                                    "Option %s cannot be used with %s",
                                    FORCE_SUBFOLDER_OPTION,
                                    ARCHIVE_FORMAT_OPTION));
                }
            }
        }
        Filter filter;
        PostFilter postFilter;
        SortingPreference sortingPreference;
        try {
            filter = filteringOptions != null
                    ? filteringOptions.toFilter()
                    : Filter.builder().build();
            postFilter = postFilteringOptions != null
                    ? postFilteringOptions.toPostFilter()
                    : PostFilter.builder().build();
            sortingPreference = sortingOptions != null
                    ? sortingOptions.toSortingPreference()
                    : SortingPreference.builder().build();
        } catch (ArgumentException e) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format(
                            "Could not process arguments: %s: %s: %s",
                            e.getMessage(),
                            e.getCause().getClass().getSimpleName(),
                            e.getCause().getMessage()),
                    e);
        } catch (NoSuchFileException e) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format(
                            "Could not process arguments: File not found: %s",
                            e.getMessage()),
                    e);
        } catch (IOException e) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format(
                            "Could not process arguments: %s: %s",
                            e.getClass().getSimpleName(),
                            e.getMessage()),
                    e);
        }
        ImmutableList<Datafile> datafiles = loadDatFiles();
        GameParser gameParser = new GameParser(
                SerializationHelper.getInstance().loadRegionData(),
                GameParser.DivergenceDetection.ONE_WAY);
        ImmutableList<ParsedGame> parsedGames = datafiles.stream()
                .map(gameParser::parse)
                .flatMap(Collection::stream)
                .collect(ImmutableList.toImmutableList());
        GameFilterer gameFilterer = new GameFilterer(filter, postFilter);
        GameSorter gameSorter = new GameSorter(new GameComparator(sortingPreference));
        ImmutableList<ParsedGame> filtered = gameFilterer.filter(parsedGames);
        ImmutableMap<String, ImmutableList<ParsedGame>> filteredGamesByParent =
                gameSorter.sortAndGroupByParent(filtered);
        ImmutableMap<String, ImmutableList<ParsedGame>> postFilteredGamesByParent =
                gameFilterer.postFilter(filteredGamesByParent);
        if (inputOutputOptions == null) {
            printTopItems(postFilteredGamesByParent.values());
        } else if (!inputOutputOptions.getInputDirs().isEmpty()) {
            ImmutableList<Detector> detectors = loadDetectors(datafiles);
            AppConfig appConfig = SerializationHelper.getInstance().loadAppConfig();
            FileScanner scanner = new FileScanner(
                    appConfig,
                    datafiles,
                    detectors,
                    new CommandLineScannerProgressBar());
            ImmutableList<FileScanner.Result> scanResults =
                    scanner.scan(inputOutputOptions.getInputDirs());
            ScanResultMatcher matcher = new ScanResultMatcher(scanResults);
            ImmutableMap<String, ImmutableList<ScanResultMatcher.GameMatchList>> presentGames =
                    matcher.match(postFilteredGamesByParent, toType);
            if (inputOutputOptions.getOutputDir() == null) {
                printTopItems(presentGames);
            } else {
                ImmutableSet<FileCopier.Spec> specs = createCopySpecs(toType, presentGames);
                FileCopier fileCopier =
                        new FileCopier(appConfig, false, new CommandLineCopierProgressBar());
                fileCopier.copy(specs);
            }
        }
        return 0;
    }

    private void printTopItems(
            Map<String, ? extends Collection<ScanResultMatcher.GameMatchList>> presentGames) {
        printTopItems(presentGames.values()
                .stream()
                .map(l -> l.stream()
                        .map(ScanResultMatcher.GameMatchList::getParsedGame)
                        .collect(ImmutableList.toImmutableList()))
                .collect(ImmutableList.toImmutableList()));
    }

    private ImmutableSet<FileCopier.Spec> createCopySpecs(
            ArchiveType toType,
            ImmutableMap<String, ImmutableList<ScanResultMatcher.GameMatchList>> presentGames) {
        return presentGames.values().stream()
                .map(Collection::stream)
                .map(Stream::findFirst)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(gameMatchList -> {
                    Game game = gameMatchList.getParsedGame().getGame();
                    Path baseDir = createBaseDirectory(game);
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
                toType.getFileExtension()));
        Stream<FileCopier.ArchiveCopySpec> archiveCopies = matchesPerFile
                .entrySet().stream()
                .map(e -> {
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
                }).filter(Objects::nonNull);
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
        return Stream.concat(archiveCopies, compressions);
    }

    private Path createBaseDirectory(Game game) {
        Path baseDir = inputOutputOptions.getOutputDir();
        if (inputOutputOptions.isGroupByFirstLetter()) {
            String firstLetter = game.getName().substring(0, 1).toLowerCase();
            if (Patterns.ALPHABETICAL.matcher(firstLetter).matches()) {
                baseDir = baseDir.resolve(firstLetter);
            } else {
                baseDir = baseDir.resolve("#");
            }
        }
        if (inputOutputOptions.isForceSubfolder() || game.getRoms().size() > 1) {
            baseDir = baseDir.resolve(game.getName());
        }
        createDirectory(baseDir);
        return baseDir;
    }

    private void createDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format(
                            "Could not create output directory: %s",
                            e.getMessage()),
                    e);
        }
    }

    private static void printTopItems(
            Collection<? extends Collection<ParsedGame>> postFilteredGamesByParent) {
        postFilteredGamesByParent.stream()
                .map(Collection::stream)
                .map(Stream::findFirst)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ParsedGame::getGame)
                .map(Game::getName)
                .sorted()
                .forEach(System.out::println);
    }

    private ImmutableList<Datafile> loadDatFiles() {
        return datFiles.stream()
                .map(f -> {
                    try {
                        return SerializationHelper.getInstance().loadXml(f, Datafile.class);
                    } catch (NoSuchFileException e) {
                        throw new CommandLine.ParameterException(
                                commandSpec.commandLine(),
                                String.format(
                                        "Could not load DAT: File not found: %s",
                                        e.getMessage()),
                                e);
                    } catch (IOException e) {
                        throw new CommandLine.ParameterException(
                                commandSpec.commandLine(),
                                String.format(
                                        "Could not load DAT: %s: %s",
                                        e.getClass().getSimpleName(),
                                        e.getMessage()),
                                e);
                    }
                }).collect(ImmutableList.toImmutableList());
    }

    private ImmutableList<Detector> loadDetectors(List<Datafile> datafiles) {
        return datafiles.stream()
                .map(Datafile::getHeader)
                .filter(Objects::nonNull)
                .map(Header::getClrmamepro)
                .filter(Objects::nonNull)
                .map(Clrmamepro::getHeaderFile)
                .filter(Objects::nonNull)
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
                        throw new CommandLine.ParameterException(
                                commandSpec.commandLine(),
                                String.format(
                                        "Could not load detector file: %s: %s",
                                        e.getClass().getSimpleName(),
                                        e.getMessage()),
                                e);
                    }
                }).collect(ImmutableList.toImmutableList());
    }

}
