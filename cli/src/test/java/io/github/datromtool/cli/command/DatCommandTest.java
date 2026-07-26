package io.github.datromtool.cli.command;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage matrix row 6 for issue #16: {@code dat} invoked with no subcommand prints its usage
 * help and exits non-zero, rather than surfacing picocli's default {@code ExecutionException}
 * ("is not a Method, Runnable or Callable") for a group command with no business logic of its own.
 */
class DatCommandTest {

    @Test
    void noSubcommandPrintsUsageAndExitsNonZero() {
        DatCommand command = new DatCommand();
        CommandLine commandLine = new CommandLine(command);
        commandLine.parseArgs();

        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(captured));
        int exitCode;
        try {
            exitCode = command.call();
        } finally {
            System.setOut(original);
        }

        assertTrue(exitCode != 0, "'dat' with no subcommand must exit non-zero");
        String output = captured.toString();
        assertTrue(output.contains("Usage:") && output.contains("convert"),
                "'dat' with no subcommand must print usage help mentioning 'convert', got:\n" + output);
        assertEquals(CommandLine.ExitCode.USAGE, exitCode, "exit code must be picocli's USAGE code");
    }
}
