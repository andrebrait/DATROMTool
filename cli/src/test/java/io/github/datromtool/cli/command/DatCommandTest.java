package io.github.datromtool.cli.command;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage matrix row 6 for issue #16: {@code dat} invoked with no subcommand behaves exactly
 * like the top-level {@code datrom} group — picocli's default handling prints
 * "Missing required subcommand" plus usage on stderr and exits with the usage code, keeping
 * stdout clean for machine-readable output.
 */
class DatCommandTest {

    @Test
    void noSubcommandPrintsMissingSubcommandOnStderrAndExitsUsage() {
        ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        System.setOut(new PrintStream(capturedOut));
        System.setErr(new PrintStream(capturedErr));
        int exitCode;
        try {
            exitCode = new CommandLine(new DatCommand()).execute();
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        assertEquals(CommandLine.ExitCode.USAGE, exitCode, "exit code must be picocli's USAGE code");
        String err = capturedErr.toString();
        assertTrue(err.contains("Missing required subcommand"),
                "stderr must name the missing subcommand requirement, got:\n" + err);
        assertTrue(err.contains("Usage:") && err.contains("convert"),
                "stderr must include usage help mentioning 'convert', got:\n" + err);
        assertEquals("", capturedOut.toString(),
                "stdout must stay clean when no subcommand is given");
    }
}
