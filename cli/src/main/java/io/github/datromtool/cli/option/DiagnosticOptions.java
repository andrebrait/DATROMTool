package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import picocli.CommandLine;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@Data
@NoArgsConstructor
@JsonInclude(NON_DEFAULT)
public class DiagnosticOptions {

    @CommandLine.Option(
            names = "--debug",
            description = "Enabled debug logging")
    private boolean debug = false;
}
