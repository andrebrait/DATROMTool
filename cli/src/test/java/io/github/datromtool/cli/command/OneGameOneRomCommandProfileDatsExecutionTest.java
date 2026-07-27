package io.github.datromtool.cli.command;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.converter.DatafileConverter;
import io.github.datromtool.config.Profile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Frozen migration proof (issue #19 step 3, mandatory red-first surface test) for the PR #31
 * deferred obligation: a profile supplying {@code input.dats} with no positional {@code DAT_FILE}
 * argument must run past the DAT-requiredness check (using the profile's DAT paths), not hit
 * "Missing required parameter: 'DAT_FILE'".
 *
 * <p>Executed RED on HEAD (before this step's execution wiring): {@code datafiles} stayed empty
 * regardless of the profile, so {@code call()} always threw the requiredness
 * {@link CommandLine.ParameterException} first. Frozen here; green once
 * {@code OneGameOneRomCommand#call()} falls back to {@code profile.getInput().getDats()} when no
 * positional DAT file is given. {@code minimal.dat} has no Parent/Clone info, so a run that gets
 * past the requiredness check still fails downstream (same "Parent/Clone information" error
 * {@code OneGameOneRomCommandDivergenceTest} pins for the positional-argument case) - this test
 * only pins that the *specific* requiredness error is gone, not full pipeline success.
 */
class OneGameOneRomCommandProfileDatsExecutionTest {

    private static Path fixture(String name) {
        try {
            return Paths.get(OneGameOneRomCommandProfileDatsExecutionTest.class
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

    // Windows CI fix (review round): hand-writing YAML with an interpolated absolute path inside
    // a double-quoted scalar breaks on Windows - the path's own backslashes are themselves YAML
    // escape sequences ("while scanning a double-quoted scalar"). Serializing a real
    // {@link Profile} through SerializationHelper's YAML mapper instead always emits forward
    // slashes regardless of platform (see PathJacksonModule), which is both portable and,
    // unlike a hand-built string, guaranteed to stay in sync with the profile's actual schema.
    private static void writeProfileWithDats(Path profileFile, Path... dats) throws IOException {
        Profile profile = Profile.builder()
                .input(Profile.InputSection.builder().dats(ImmutableList.copyOf(dats)).build())
                .build();
        Files.write(profileFile, SerializationHelper.getInstance().writeAsYaml(profile));
    }

    @Test
    void profileOnlyDatsRunsPastDatRequirednessCheck(@TempDir Path tempDir) throws IOException {
        Path datPath = fixture("datafiles/minimal.dat");
        Path profile = tempDir.resolve("profile.yaml");
        writeProfileWithDats(profile, datPath);

        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs("--profile", profile.toString());

        CommandLine.ParameterException ex = assertThrows(
                CommandLine.ParameterException.class,
                command::call,
                "a profile-dats-only run must still fail (minimal.dat lacks Parent/Clone info), "
                        + "but not with the DAT_FILE requiredness error");
        assertFalse(
                ex.getMessage().contains("Missing required parameter"),
                "profile input.dats alone must not hit the DAT_FILE requiredness error, got: "
                        + ex.getMessage());
        assertTrue(
                ex.getMessage().contains("Parent/Clone information"),
                "must proceed far enough to hit the downstream DAT-shape error instead, got: "
                        + ex.getMessage());
    }

    // Flag-beats-file: a positional DAT_FILE argument must win over profile input.dats, never
    // even attempting to load the profile's (bogus, nonexistent) path.
    @Test
    void positionalDatafileWinsOverProfileDats(@TempDir Path tempDir) throws IOException {
        Path profile = tempDir.resolve("profile.yaml");
        Files.writeString(profile, "input:\n  dats:\n    - \"/no/such/profile-dat.xml\"\n");

        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--profile", profile.toString(),
                fixture("datafiles/minimal.dat").toString());

        CommandLine.ParameterException ex = assertThrows(
                CommandLine.ParameterException.class,
                command::call,
                "positional DAT_FILE must be used, reaching minimal.dat's own downstream error");
        assertTrue(
                ex.getMessage().contains("Parent/Clone information"),
                "must fail with minimal.dat's own error, not a profile-path-loading error, got: "
                        + ex.getMessage());
    }
}
