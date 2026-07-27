package io.github.datromtool.cli.command;

import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.converter.DatafileConverter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Frozen migration proof (issue #19 step 3, mandatory red-first surface test): {@code
 * --clonelist} and {@code --retool-metadata} must be accepted options on {@code 1g1r}. Executed
 * RED on HEAD before this step's CLI wiring landed - picocli rejected both as unknown options
 * with a {@code CommandLine.UnmatchedArgumentException} (a {@link CommandLine.ParameterException}
 * subtype) at parse time. Frozen here byte-identical; green once the corresponding {@link
 * io.github.datromtool.cli.option.InputOptions} fields land. Only option *acceptance* at parse
 * time is pinned here - any existing file is a valid argument value for this test's purposes,
 * since content parsing/validation happens later, at {@code call()} time.
 */
class OneGameOneRomCommandRetoolOptionsTest {

    private static Path fixture(String name) {
        try {
            return Paths.get(OneGameOneRomCommandRetoolOptionsTest.class
                    .getClassLoader()
                    .getResource(name)
                    .toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CommandLine newCommandLine(OneGameOneRomCommand command) {
        CommandLine commandLine = new CommandLine(command);
        commandLine.registerConverter(DatafileArgument.class, new DatafileConverter());
        return commandLine;
    }

    @Test
    void clonelistOptionIsAccepted() {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        assertDoesNotThrow(
                () -> commandLine.parseArgs("--clonelist", fixture("datafiles/minimal.dat").toString()),
                "--clonelist must be a recognized option");
    }

    @Test
    void retoolMetadataOptionIsAccepted() {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        assertDoesNotThrow(
                () -> commandLine.parseArgs("--retool-metadata", fixture("datafiles/minimal.dat").toString()),
                "--retool-metadata must be a recognized option");
    }
}
