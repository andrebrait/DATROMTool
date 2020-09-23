package io.github.datromtool.cli.command.onegameonerom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.GameFilterer;
import io.github.datromtool.GameParser;
import io.github.datromtool.GameSorter;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.cli.GitVersionProvider;
import io.github.datromtool.cli.option.FilteringOptions;
import io.github.datromtool.cli.option.PostFilteringOptions;
import io.github.datromtool.cli.option.SortingOptions;
import io.github.datromtool.cli.progressbar.CommandLineScannerProgressBar;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.Pair;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.domain.datafile.Clrmamepro;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.datafile.Game;
import io.github.datromtool.domain.datafile.Header;
import io.github.datromtool.domain.datafile.Rom;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.io.ArchiveType;
import io.github.datromtool.io.FileCopier;
import io.github.datromtool.io.FileScanner;
import io.github.datromtool.sorting.GameComparator;
import io.github.datromtool.util.ArgumentException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import picocli.CommandLine;

import javax.annotation.Nullable;
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
import static io.github.datromtool.cli.command.onegameonerom.InputOutputOptions.INPUT_DIR_OPTION;
import static io.github.datromtool.cli.command.onegameonerom.InputOutputOptions.OUTPUT_DIR_OPTION;
import static lombok.AccessLevel.NONE;
import static lombok.AccessLevel.PRIVATE;

@Data
@Jacksonized
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
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
            if (inputOutputOptions.getArchiveType() != ArchiveType.NONE) {
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
            printTopItems(postFilteredGamesByParent.values().asList());
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
            ImmutableMap<Pair<Long, String>, ImmutableList<FileScanner.Result>> resultsForCrc =
                    scanResults
                            .stream()
                            .collect(Collectors.collectingAndThen(
                                    Collectors.groupingBy(
                                            o -> Pair.of(
                                                    o.getUnheaderedSize(),
                                                    o.getDigest().getCrc()),
                                            ImmutableList.toImmutableList()),
                                    ImmutableMap::copyOf));
            ImmutableMap<String, ImmutableList<FileScanner.Result>> resultsForMd5 =
                    scanResults.stream()
                            .collect(Collectors.collectingAndThen(
                                    Collectors.groupingBy(
                                            o -> o.getDigest().getMd5(),
                                            ImmutableList.toImmutableList()),
                                    ImmutableMap::copyOf));
            ImmutableMap<String, ImmutableList<FileScanner.Result>> resultsForSha1 =
                    scanResults.stream()
                            .collect(Collectors.collectingAndThen(
                                    Collectors.groupingBy(
                                            o -> o.getDigest().getSha1(),
                                            ImmutableList.toImmutableList()),
                                    ImmutableMap::copyOf));
            ImmutableMap<String,
                    ImmutableList<Pair<ParsedGame, ImmutableList<Pair<Rom, FileScanner.Result>>>>>
                    presentGames =
                    postFilteredGamesByParent.entrySet().stream()
                            .map(e -> Pair.of(
                                    e.getKey(),
                                    e.getValue().stream()
                                            .map(pg -> Pair.of(
                                                    pg,
                                                    getResults(
                                                            resultsForCrc,
                                                            resultsForMd5,
                                                            resultsForSha1,
                                                            pg)))
                                            .filter(p -> !p.getRight().isEmpty())
                                            .collect(ImmutableList.toImmutableList())))
                            .filter(p -> !p.getRight().isEmpty())
                            .collect(ImmutableMap.toImmutableMap(Pair::getLeft, Pair::getRight));
            if (inputOutputOptions.getOutputDir() == null) {
                printTopItems(presentGames.values()
                        .stream()
                        .map(l -> l.stream()
                                .map(Pair::getLeft)
                                .collect(ImmutableList.toImmutableList()))
                        .collect(ImmutableList.toImmutableList()));
            } else {
                try {
                    Files.createDirectories(inputOutputOptions.getOutputDir());
                } catch (IOException e) {
                    throw new CommandLine.ParameterException(
                            commandSpec.commandLine(),
                            String.format(
                                    "Could not create output directory: %s",
                                    e.getMessage()),
                            e);
                }
                ImmutableList<FileCopier.CopyDefinition> copyDefinitions = presentGames.values()
                        .stream()
                        .map(Collection::stream)
                        .map(Stream::findFirst)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(p -> {
                            ImmutableMap<Path, ImmutableList<FileScanner.Result>> resultsPerFile =
                                    p.getRight().stream()
                                            .collect(Collectors.collectingAndThen(
                                                    Collectors.groupingBy(
                                                            FileScanner.Result::getPath,
                                                            ImmutableList.toImmutableList()),
                                                    ImmutableMap::copyOf));
                            return resultsPerFile.entrySet().stream()
                                    .map(e -> FileCopier.CopyDefinition.builder()
                                            .from(e.getKey())
                                            .fromType(e.getValue()
                                                    .iterator()
                                                    .next()
                                                    .getArchiveType())
                                            .archiveCopyDefinitions(e.getValue().stream()
                                                    .map(FileScanner.Result::getArchivePath)
                                                    .filter(Objects::nonNull)
                                                    .map(s -> FileCopier.ArchiveCopyDefinition.builder()
                                                            .source(s)
                                                            .destination(pg))
                                                    .collect(ImmutableSet.toImmutableSet())))
                        })
            }
        }
        return 0;
    }

    // TODO Move this to core and add logging. Extract almost everything from this class to core.
    private ImmutableList<Pair<Rom, FileScanner.Result>> getResults(
            Map<Pair<Long, String>, ? extends List<FileScanner.Result>> resultsForCrc,
            Map<String, ? extends List<FileScanner.Result>> resultsForMd5,
            Map<String, ? extends List<FileScanner.Result>> resultsForSha1,
            ParsedGame parsedGame) {
        ImmutableList<Pair<Rom, ImmutableList<FileScanner.Result>>> results = parsedGame.getGame()
                .getRoms()
                .stream()
                .map(r -> Pair.of(
                        r,
                        getResultFromMaps(
                                resultsForCrc,
                                resultsForMd5,
                                resultsForSha1,
                                r)))
                .filter(p -> !p.getRight().isEmpty())
                .collect(ImmutableList.toImmutableList());
        if (results.size() < parsedGame.getGame().getRoms().size()) {
            // TODO Log a warning (not all files were found)
            return ImmutableList.of();
        }
        // TODO Prefer all from the same archive or all uncompressed
        // FIXME FILTER HERE
        ImmutableList<Pair<Rom, FileScanner.Result>> filteredResults =
                results.stream()
                        .map(p -> Pair.of(p.getLeft(), p.getRight().iterator().next()))
                        .collect(ImmutableList.toImmutableList());
        // TODO If the destination is uncompressed, prefer uncompressed as much as possible
        if (inputOutputOptions.getArchiveType() != ArchiveType.NONE) {
            if (isMultipleOriginTypes(filteredResults) || isFromMultipleArchives(filteredResults)) {
                // TODO Log a warning (cannot merge several archives into one)
                return ImmutableList.of();
            }
        }
        return results;
    }

    private static boolean isMultipleOriginTypes(
            ImmutableList<Pair<Rom, FileScanner.Result>> results) {
        return results.stream()
                .map(Pair::getRight)
                .map(FileScanner.Result::getArchiveType)
                .distinct()
                .count() > 1;
    }

    private static boolean isFromMultipleArchives(
            ImmutableList<Pair<Rom, FileScanner.Result>> results) {
        return results.stream()
                .map(Pair::getRight)
                .filter(r -> r.getArchiveType() != ArchiveType.NONE)
                .map(FileScanner.Result::getPath)
                .distinct()
                .count() > 1;
    }

    @Nullable
    private static ImmutableList<FileScanner.Result> getResultFromMaps(
            Map<Pair<Long, String>, ? extends List<FileScanner.Result>> resultsForCrc,
            Map<String, ? extends List<FileScanner.Result>> resultsForMd5,
            Map<String, ? extends List<FileScanner.Result>> resultsForSha1,
            Rom rom) {
        List<FileScanner.Result> results = resultsForSha1.get(rom.getSha1());
        if (results == null) {
            results = resultsForMd5.get(rom.getMd5());
        }
        if (results == null) {
            results = resultsForCrc.get(Pair.of(rom.getSize(), rom.getCrc()));
        }
        if (results == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(results);
    }

    private static void printTopItems(List<? extends List<ParsedGame>> postFilteredGamesByParent) {
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
