package io.github.datromtool.cli.command.onegameonerom;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.datromtool.cli.ArchiveCompletionCandidates;
import io.github.datromtool.io.ArchiveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import picocli.CommandLine;

import java.nio.file.Path;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@Data
@Jacksonized
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
@JsonInclude(NON_DEFAULT)
public final class InputOutputOptions {

    @CommandLine.Option(
            names = {"-i", "--input-dir"},
            paramLabel = "PATH",
            description = "Base directory for scanning ROM files")
    private Path inputDir;

    @CommandLine.Option(
            names = {"-o", "--output-dir"},
            paramLabel = "PATH",
            description = "Output directory for the resulting files")
    private Path outputDir;

    @CommandLine.Option(
            names = {"-af", "--archive-format"},
            paramLabel = "FORMAT",
            description = "Output archive format (Default: ${DEFAULT-VALUE})\n"
                    + "Valid values: ${COMPLETION-CANDIDATES}",
            completionCandidates = ArchiveCompletionCandidates.class,
            showDefaultValue = CommandLine.Help.Visibility.NEVER,
            defaultValue = "NONE")
    private ArchiveType outputFormat;

}
