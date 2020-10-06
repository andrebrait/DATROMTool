package io.github.datromtool.cli.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.cli.GitVersionProvider;
import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.option.FilteringOptions;
import io.github.datromtool.cli.option.InputOptions;
import io.github.datromtool.cli.option.OutputOptions;
import io.github.datromtool.cli.option.PostFilteringOptions;
import io.github.datromtool.cli.option.SortingOptions;
import io.github.datromtool.cli.progressbar.CommandLineCopierProgressBar;
import io.github.datromtool.cli.progressbar.CommandLineScannerProgressBar;
import io.github.datromtool.command.OneGameOneRom;
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
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
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

    @Override
    public Integer call() {
        if (outputOptions != null
                && outputOptions.getFileOptions() != null
                && outputOptions.getFileOptions().getOutputDir() != null
                && (inputOptions == null || inputOptions.getInputDirs().isEmpty())) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format(
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
        try {
            if (outputOptions != null && outputOptions.getFileOptions() != null) {
                oneGameOneRom.generate(
                        realDataFiles,
                        inputOptions.getInputDirs(),
                        outputOptions.getFileOptions().toFileOutputOptions(),
                        new CommandLineScannerProgressBar(),
                        new CommandLineCopierProgressBar());
            } else {
                TextOutputOptions textOutputOptions =
                        outputOptions != null && outputOptions.getTextOptions() != null
                                ? outputOptions.getTextOptions().toTextOutputOptions()
                                : null;
                List<Path> inputDirs = inputOptions != null
                        ? inputOptions.getInputDirs()
                        : null;
                oneGameOneRom.generate(
                        realDataFiles,
                        inputDirs,
                        textOutputOptions,
                        new CommandLineScannerProgressBar(),
                        list -> list.forEach(System.out::println));
            }
        } catch (InvalidDatafileException e) {
            log.debug("Got invalid DAT exception", e);
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format("Invalid DAT file: %s", e.getMessage()));
        } catch (ExecutionException e) {
            log.debug("Got execution exception", e);
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    String.format("Execution error: %s", e.getMessage()));
        }
        return 0;
    }

}
