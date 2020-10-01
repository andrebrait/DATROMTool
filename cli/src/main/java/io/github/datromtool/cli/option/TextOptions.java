package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.datromtool.cli.converter.OutputModeConverter;
import io.github.datromtool.io.OutputMode;
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
            completionCandidates = OutputModeConverter.class)
    private OutputMode outputMode;
}
