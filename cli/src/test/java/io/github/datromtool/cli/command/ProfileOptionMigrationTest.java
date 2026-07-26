package io.github.datromtool.cli.command;

import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.converter.DatafileConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Red-first surface-change proof for issue #15 step 3: {@code --profile} and
 * {@code --dump-profile} are new options on {@link OneGameOneRomCommand}. Parse-only (no
 * {@code call()} execution) so it only pins parser acceptance, not pipeline behavior.
 *
 * <p>Executed RED before the migration ({@code --profile}/{@code --dump-profile} were unknown
 * options, rejected with {@link CommandLine.UnmatchedArgumentException}); frozen byte-identical
 * and re-run GREEN, unchanged, after the migration.
 */
class ProfileOptionMigrationTest {

    private static Path datFixture() {
        try {
            return Paths.get(ProfileOptionMigrationTest.class
                    .getClassLoader()
                    .getResource("datafiles/minimal.dat")
                    .toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CommandLine newCommandLine() {
        CommandLine commandLine = new CommandLine(new OneGameOneRomCommand());
        commandLine.registerConverter(DatafileArgument.class, new DatafileConverter());
        return commandLine;
    }

    @Test
    void profileOptionIsAcceptedWithAnExistingFile(@TempDir Path tempDir) throws IOException {
        Path emptyProfile = tempDir.resolve("profile.yaml");
        Files.writeString(emptyProfile, "{}\n");
        assertDoesNotThrow(
                () -> newCommandLine().parseArgs(
                        datFixture().toString(),
                        "--profile", emptyProfile.toString()),
                "--profile must be accepted as a known option");
    }

    @Test
    void dumpProfileOptionIsAcceptedWithNoValue() {
        assertDoesNotThrow(
                () -> newCommandLine().parseArgs(datFixture().toString(), "--dump-profile"),
                "--dump-profile must be accepted with no value (defaults to yaml)");
    }

    @Test
    void dumpProfileOptionIsAcceptedWithYamlValue() {
        assertDoesNotThrow(
                () -> newCommandLine().parseArgs(datFixture().toString(), "--dump-profile", "yaml"),
                "--dump-profile yaml must be accepted");
    }

    @Test
    void dumpProfileOptionIsAcceptedWithJsonValue() {
        assertDoesNotThrow(
                () -> newCommandLine().parseArgs(datFixture().toString(), "--dump-profile", "json"),
                "--dump-profile json must be accepted");
    }
}
