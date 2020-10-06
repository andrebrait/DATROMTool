package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.datromtool.cli.converter.ArchiveTypeConverter;
import io.github.datromtool.cli.converter.OutputModeConverter;
import io.github.datromtool.data.FileOutputOptions;
import io.github.datromtool.data.OutputMode;
import io.github.datromtool.data.TextOutputOptions;
import io.github.datromtool.io.ArchiveType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import picocli.CommandLine;

import java.nio.file.Path;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@JsonInclude(NON_NULL)
public final class OutputOptions {

    @CommandLine.ArgGroup(heading = "File output options\n", exclusive = false)
    private FileOptions fileOptions;

    @CommandLine.ArgGroup(heading = "Text output options\n", exclusive = false)
    private TextOptions textOptions;

    @Data
    @NoArgsConstructor
    @JsonInclude(NON_NULL)
    public static final class TextOptions {

        @CommandLine.Option(
                names = "--out-file",
                paramLabel = "PATH",
                description = "Output text file (default: print to console)")
        private Path outputFile;

        @CommandLine.Option(
                names = "--out-mode",
                paramLabel = "MODE",
                description = "Output mode (default: print each entry's name). "
                        + "Options: ${COMPLETION-CANDIDATES}",
                completionCandidates = OutputModeConverter.class)
        private OutputMode outputMode;

        public TextOutputOptions toTextOutputOptions() {
            return TextOutputOptions.builder()
                    .outputFile(outputFile)
                    .outputMode(outputMode)
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(NON_DEFAULT)
    public static class FileOptions {

        public static final String OUT_DIR_OPTION = "--out-dir";

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
                names = OUT_DIR_OPTION,
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

        public FileOutputOptions toFileOutputOptions() {
            FileOutputOptions.FileOutputOptionsBuilder
                    builder = FileOutputOptions.builder();
            builder = builder
                    .outputDir(outputDir)
                    .alphabetic(alphabetic);
            if (groupingOptions != null) {
                builder = builder
                        .archiveType(groupingOptions.getArchiveType())
                        .forceSubfolder(groupingOptions.isForceSubfolder());
            }
            return builder.build();
        }

    }
}
