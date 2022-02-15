package io.github.datromtool.cli.command;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.cli.GitVersionProvider;
import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.option.*;
import io.github.datromtool.cli.progressbar.CommandLineProgressBar;
import io.github.datromtool.command.OneGameOneRom;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.data.TextOutputOptions;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.exception.ExecutionException;
import io.github.datromtool.exception.InvalidDatafileException;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.Ansi;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static java.lang.String.format;
import static lombok.AccessLevel.NONE;

@Slf4j
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
    private InputOptions inputOptions;

    @CommandLine.ArgGroup
    private OutputOptions outputOptions;

    @CommandLine.ArgGroup(heading = "Filtering options\n", exclusive = false)
    private FilteringOptions filteringOptions;

    @CommandLine.ArgGroup(heading = "Post-filtering options\n", exclusive = false)
    private PostFilteringOptions postFilteringOptions;

    @CommandLine.ArgGroup(heading = "Sorting options\n", exclusive = false)
    private SortingOptions sortingOptions;

    @CommandLine.ArgGroup(heading = "Performance options\n", exclusive = false)
    private PerformanceOptions performanceOptions;

    @CommandLine.ArgGroup(heading = "Diagnostic options\n", exclusive = false)
    private DiagnosticOptions diagnosticOptions;

    @Override
    public Integer call() {
        if (diagnosticOptions != null && diagnosticOptions.isDebug()) {
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.DEBUG);
        }
        if (outputOptions != null
                && outputOptions.getFileOptions() != null
                && outputOptions.getFileOptions().getOutputDir() != null
                && (inputOptions == null || inputOptions.getInputDirs().isEmpty())) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    format(
                            "%s requires %s",
                            OutputOptions.FileOptions.OUT_DIR_OPTION,
                            InputOptions.IN_DIR_OPTION));
        }
        Filter filter = filteringOptions != null
                ? filteringOptions.toFilter()
                : Filter.builder().build();
        PostFilter postFilter = postFilteringOptions != null
                ? postFilteringOptions.toPostFilter()
                : PostFilter.builder().build();
        SortingPreference sortingPreference = sortingOptions != null
                ? sortingOptions.toSortingPreference()
                : SortingPreference.builder().build();
        OneGameOneRom oneGameOneRom = new OneGameOneRom(filter, postFilter, sortingPreference);
        List<Datafile> realDataFiles = datafiles.stream()
                .map(DatafileArgument::getDatafile)
                .collect(ImmutableList.toImmutableList());
        AppConfig appConfig = SerializationHelper.getInstance().loadAppConfig();
        if (performanceOptions != null) {
            appConfig = appConfig.withScanner(performanceOptions.merge(appConfig.getScanner()));
            appConfig = appConfig.withCopier(performanceOptions.merge(appConfig.getCopier()));
        }
        try {
            CommandLineProgressBar fileScannerListener = new CommandLineProgressBar("Scanning", "Scanning input directories...");
            if (outputOptions != null && outputOptions.getFileOptions() != null) {
                CommandLineProgressBar fileCopierListener = new CommandLineProgressBar("Copying", "Copying selected files...");
                oneGameOneRom.generate(
                        appConfig,
                        realDataFiles,
                        inputOptions.getInputDirs(),
                        outputOptions.getFileOptions().toFileOutputOptions(),
                        fileScannerListener,
                        fileCopierListener);
            } else {
                TextOutputOptions textOutputOptions =
                        outputOptions != null && outputOptions.getTextOptions() != null
                                ? outputOptions.getTextOptions().toTextOutputOptions()
                                : null;
                List<Path> inputDirs = inputOptions != null
                        ? inputOptions.getInputDirs()
                        : null;
                oneGameOneRom.generate(
                        appConfig,
                        realDataFiles,
                        inputDirs,
                        textOutputOptions,
                        fileScannerListener,
                        list -> list.forEach(System.out::println));
            }
        } catch (InvalidDatafileException e) {
            log.debug("Got invalid DAT exception", e);
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    format("Invalid DAT file: %s", e.getMessage()));
        } catch (ExecutionException e) {
            System.err.print(Ansi.ansi().eraseScreen());
            log.error("Got execution exception", e);
            System.err.printf("Execution error caught. Check logs for details: %s%n", e.getCause());
            return 1;
        } finally {
            Path currentDir = Paths.get("").toAbsolutePath().normalize().resolve("datromtool.log");
            System.err.println();
            System.err.println("Finished");
            System.err.printf("Check the generated log file for details: %s%n", currentDir);
        }
        return 0;
    }

}
