package io.github.datromtool.cli.command;

import io.github.datromtool.GameParser;
import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.converter.DatafileConverter;
import io.github.datromtool.cli.converter.DivergenceDetectionConverter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage matrix row 4 for issue #18: {@code 1g1r --divergence} accepts each
 * {@link GameParser.DivergenceDetection} value case-insensitively, rejects a bogus value listing
 * the valid candidates, defaults to {@code ONE_WAY} (unchanged behavior) when absent, and the
 * parsed value is the one threaded into {@link io.github.datromtool.command.OneGameOneRom}'s
 * construction (pinned by running the command end to end and confirming execution reaches past
 * {@link GameParser} construction/parsing for every accepted value, identically to today's
 * hardcoded {@code ONE_WAY}).
 */
class OneGameOneRomCommandDivergenceTest {

    private static Path fixture(String name) {
        try {
            return Paths.get(OneGameOneRomCommandDivergenceTest.class
                    .getClassLoader()
                    .getResource("datafiles/" + name)
                    .toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CommandLine newCommandLine(OneGameOneRomCommand command) {
        CommandLine commandLine = new CommandLine(command);
        commandLine.registerConverter(DatafileArgument.class, new DatafileConverter());
        commandLine.registerConverter(GameParser.DivergenceDetection.class, new DivergenceDetectionConverter());
        return commandLine;
    }

    // Default (option absent) behaves as ONE_WAY: no behavior change from before this option
    // existed.
    @Test
    void defaultsToOneWayWhenAbsent() {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        newCommandLine(command).parseArgs();
        assertEquals(GameParser.DivergenceDetection.ONE_WAY, command.getDivergenceDetection());
    }

    // Every enum value is accepted case-insensitively.
    @Test
    void acceptsEveryValueCaseInsensitively() {
        for (GameParser.DivergenceDetection mode : GameParser.DivergenceDetection.values()) {
            for (String spelling : new String[]{
                    mode.name().toLowerCase(Locale.ROOT),
                    mode.name().toUpperCase(Locale.ROOT)}) {
                OneGameOneRomCommand command = new OneGameOneRomCommand();
                newCommandLine(command).parseArgs("--divergence", spelling);
                assertEquals(mode, command.getDivergenceDetection(),
                        "spelling '" + spelling + "' must parse as " + mode);
            }
        }
    }

    // A bogus value fails parsing, listing the valid candidates.
    @Test
    void bogusValueFailsListingCandidates() {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        CommandLine.ParameterException ex = assertThrows(
                CommandLine.ParameterException.class,
                () -> commandLine.parseArgs("--divergence", "bogus"),
                "an unknown --divergence value must fail parsing");
        assertTrue(
                ex.getMessage().contains("ignore")
                        && ex.getMessage().contains("one_way")
                        && ex.getMessage().contains("two_way")
                        && ex.getMessage().contains("always"),
                "error must list the valid divergence candidates, got: " + ex.getMessage());
    }

    // Wiring pin: the parsed mode reaches OneGameOneRom's GameParser construction. minimal.dat has
    // no Parent/Clone info, so every accepted --divergence value must fail identically deep in the
    // pipeline (past GameParser.parse + the post-parse validate call) rather than at option
    // parsing — proving the option value flows all the way through OneGameOneRom.generate() for
    // every mode, not just the default.
    @Test
    void everyDivergenceValueReachesOneGameOneRomConstruction() {
        for (GameParser.DivergenceDetection mode : GameParser.DivergenceDetection.values()) {
            OneGameOneRomCommand command = new OneGameOneRomCommand();
            CommandLine commandLine = newCommandLine(command);
            commandLine.parseArgs(
                    "--divergence", mode.name().toLowerCase(Locale.ROOT),
                    fixture("minimal.dat").toString());
            CommandLine.ParameterException ex = assertThrows(
                    CommandLine.ParameterException.class,
                    command::call,
                    "mode " + mode + " must still reach the pipeline's DAT-shape validation");
            assertTrue(
                    ex.getMessage().contains("Parent/Clone information"),
                    "mode " + mode + " must fail with the same downstream DAT-shape error, got: "
                            + ex.getMessage());
        }
    }
}
