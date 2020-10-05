package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.cli.converter.ArchiveTypeConverter;
import io.github.datromtool.cli.converter.ExistingDirectoryConverter;
import io.github.datromtool.data.FileInputOutput;
import io.github.datromtool.io.ArchiveType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@Data
@NoArgsConstructor
@JsonInclude(NON_DEFAULT)
public final class InputOutputOptions {

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
                    completionCandidates = ArchiveTypeConverter.class,
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

    @CommandLine.Option(
            names = "--in-dir",
            paramLabel = "PATH",
            description = "Base directory for scanning ROM files",
            converter = ExistingDirectoryConverter.class,
            required = true)
    private List<Path> inputDirs = ImmutableList.of();

    @CommandLine.ArgGroup(heading = "File output options\n", exclusive = false)
    private OutputOptions outputOptions;

    public FileInputOutput toFileInputOutput() {
        FileInputOutput.FileInputOutputBuilder fileInputOutputBuilder = FileInputOutput.builder()
                .inputDirs(inputDirs);
        if (outputOptions != null) {
            fileInputOutputBuilder = fileInputOutputBuilder
                    .outputDir(outputOptions.getOutputDir())
                    .alphabetic(outputOptions.isAlphabetic());
            if (outputOptions.getGroupingOptions() != null) {
                fileInputOutputBuilder = fileInputOutputBuilder
                        .archiveType(outputOptions.getGroupingOptions().getArchiveType())
                        .forceSubfolder(outputOptions.getGroupingOptions().isForceSubfolder());
            }
        }
        return fileInputOutputBuilder.build();
    }

}
