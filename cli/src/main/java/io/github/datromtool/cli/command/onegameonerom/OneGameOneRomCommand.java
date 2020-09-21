package io.github.datromtool.cli.command.onegameonerom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.cli.GitVersionProvider;
import io.github.datromtool.cli.option.FilteringOptions;
import io.github.datromtool.cli.option.PostFilteringOptions;
import io.github.datromtool.cli.option.SortingOptions;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.io.ArchiveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

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
    public Integer call() {
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
        try {
            Filter filter = filteringOptions != null
                    ? filteringOptions.toFilter()
                    : null;
            PostFilter postFilter = postFilteringOptions != null
                    ? postFilteringOptions.toPostFilter()
                    : null;
            SortingPreference sortingPreference = sortingOptions != null
                    ? sortingOptions.toSortingPreference()
                    : null;
        } catch (Exception e) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    "Error processing arguments",
                    e);
        }
        return 0;
    }

}
