package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.datromtool.cli.OutputMode;
import io.github.datromtool.cli.OutputModeCompletionCandidates;
import io.github.datromtool.cli.converter.OutputModeConverter;
import lombok.Data;
import lombok.NoArgsConstructor;
import picocli.CommandLine;

import java.nio.file.Path;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@Data
@NoArgsConstructor
@JsonInclude(NON_DEFAULT)
public final class TextOptions {

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
            converter = OutputModeConverter.class,
            completionCandidates = OutputModeCompletionCandidates.class)
    private OutputMode outputMode;
}
