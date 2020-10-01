package io.github.datromtool.cli.command.onegameonerom;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.cli.ArchiveCompletionCandidates;
import io.github.datromtool.cli.converter.ArchiveTypeConverter;
import io.github.datromtool.io.ArchiveType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@Data
@NoArgsConstructor
@JsonInclude(NON_DEFAULT)
public final class InputOutputOptions {

    //FIXME this doesn't work
    @CommandLine.Spec(CommandLine.Spec.Target.MIXEE)
    private CommandLine.Model.CommandSpec spec;

    @Data
    @NoArgsConstructor
    @JsonInclude(NON_DEFAULT)
    public static class OutputOptions {


        @Data
        @NoArgsConstructor
        @JsonInclude(NON_DEFAULT)
        public static class GroupingOptions {

            @NonNull
            @CommandLine.Option(
                    names = "--archive",
                    paramLabel = "FORMAT",
                    description = "Archive format for output (default: uncompressed). "
                            + "Options: ${COMPLETION-CANDIDATES}",
                    converter = ArchiveTypeConverter.class,
                    completionCandidates = ArchiveCompletionCandidates.class,
                    showDefaultValue = CommandLine.Help.Visibility.NEVER)
            private ArchiveType archiveType;

            @CommandLine.Option(
                    names = "--force-subfolder",
                    description = "Create subfolders even for entries with one file")
            private boolean forceSubfolder;
        }

        @CommandLine.Option(
                names = "--out-dir",
                paramLabel = "PATH",
                description = "Output directory for the resulting files",
                required = true)
        private Path outputDir;

        @CommandLine.Option(
                names = "--alphabetic",
                description = "Group resulting files in subfolders based on their names")
        private boolean alphabetic;

        @CommandLine.ArgGroup
        private GroupingOptions groupingOptions;

    }

    private List<Path> inputDirs = ImmutableList.of();

    @CommandLine.Option(
            names = "--in-dir",
            paramLabel = "PATH",
            description = "Base directory for scanning ROM files",
            required = true)
    public void setInputDirs(List<Path> inputDirs) {
        for (Path dir : inputDirs) {
            if (!Files.isDirectory(dir)) {
                throw new CommandLine.ParameterException(
                        spec.commandLine(),
                        String.format("Cannot access'%s': no such directory", dir));
            }
        }
        this.inputDirs = inputDirs;
    }

    @CommandLine.ArgGroup(heading = "File output options\n", exclusive = false)
    private OutputOptions outputOptions;

}
