package io.github.datromtool.cli.command.onegameonerom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.io.ArchiveType;
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

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
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
            printTopItems(postFilteredGamesByParent);
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
            ImmutableMap<Pair<Long, String>, FileScanner.Result> resultsForCrc = scanResults
                    .stream()
                    .collect(ImmutableMap.toImmutableMap(
                            o -> Pair.of(o.getUnheaderedSize(), o.getDigest().getCrc()),
                            Function.identity()));
            ImmutableMap<String, FileScanner.Result> resultsForMd5 = scanResults.stream()
                    .collect(ImmutableMap.toImmutableMap(
                            o -> o.getDigest().getMd5(),
                            Function.identity()));
            ImmutableMap<String, FileScanner.Result> resultsForSha1 = scanResults.stream()
                    .collect(ImmutableMap.toImmutableMap(
                            o -> o.getDigest().getSha1(),
                            Function.identity()));
            ImmutableMap<String, ImmutableList<ParsedGame>> presentGames =
                    postFilteredGamesByParent.entrySet().stream()
                            .map(e -> Pair.of(
                                    e.getKey(),
                                    e.getValue().stream()
                                            .filter(pg -> isPresentInScannedFiles(
                                                    resultsForCrc,
                                                    resultsForMd5,
                                                    resultsForSha1,
                                                    pg))
                                            .collect(ImmutableList.toImmutableList())))
                            .filter(p -> !p.getRight().isEmpty())
                            .collect(ImmutableMap.toImmutableMap(Pair::getLeft, Pair::getRight));
            if (inputOutputOptions.getOutputDir() == null) {
                printTopItems(presentGames);
            }
        }
        return 0;
    }

    // TODO Move this to core and add logging. Extract almost everything from this class to core.
    private boolean isPresentInScannedFiles(
            ImmutableMap<Pair<Long, String>, FileScanner.Result> resultsForCrc,
            ImmutableMap<String, FileScanner.Result> resultsForMd5,
            ImmutableMap<String, FileScanner.Result> resultsForSha1,
            ParsedGame pg) {
        return pg.getGame()
                .getRoms()
                .stream()
                .allMatch(r -> resultsForSha1.containsKey(r.getSha1())
                        || resultsForMd5.containsKey(r.getMd5())
                        || resultsForCrc.containsKey(Pair.of(r.getSize(), r.getCrc())));
    }

    private void printTopItems(ImmutableMap<String, ImmutableList<ParsedGame>> postFilteredGamesByParent) {
        postFilteredGamesByParent.values()
                .stream()
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

    private ImmutableList<Detector> loadDetectors(ImmutableList<Datafile> datafiles) {
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
