package io.github.datromtool.cli.command;

import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.converter.DatafileConverter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Row 32: pins {@link OneGameOneRomCommand#call()}'s cross-option validation that
 * {@code --out-dir} requires {@code --in-dir}, exercised at parse+call level with a minimal DAT
 * fixture so the eager {@code DatafileConverter} loading in the positional parameter succeeds.
 */
class OneGameOneRomCommandTest {

    private static Path fixture(String name) {
        try {
            return Paths.get(OneGameOneRomCommandTest.class
                    .getClassLoader()
                    .getResource("datafiles/" + name)
                    .toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void outDirWithoutInDirFailsCrossOptionValidation() {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = new CommandLine(command);
        commandLine.registerConverter(DatafileArgument.class, new DatafileConverter());
        commandLine.parseArgs(fixture("minimal.dat").toString(), "--out-dir", "out");
        assertThrows(
                CommandLine.ParameterException.class,
                command::call,
                "--out-dir without --in-dir must fail the command's cross-option validation");
    }
}
