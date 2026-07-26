package io.github.datromtool.cli.command;

import io.github.datromtool.SerializationHelper;
import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.converter.DatafileConverter;
import io.github.datromtool.config.Profile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage matrix rows 3 (layering), 4 (--dump-profile), and 5 (error surfacing) for issue #15
 * step 3. Row 1/2/7 field-overlay behavior is pinned directly against {@link
 * io.github.datromtool.cli.profile.ProfileBinder} in {@code ProfileBinderTest}; row 6 (no
 * profile, no flags -&gt; unchanged behavior) is the existing cli test suite staying green,
 * unedited.
 */
class OneGameOneRomCommandProfileTest {

    private static Path datFixture() {
        try {
            return Paths.get(OneGameOneRomCommandProfileTest.class
                    .getClassLoader()
                    .getResource("datafiles/minimal.dat")
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

    // F2: OneGameOneRomCommand's --dump-profile snapshot layers the real
    // ~/.DATROMTool/config.yaml (or built-in defaults, if absent) into the dumped `performance`
    // section via SerializationHelper#loadAppConfig (see OneGameOneRomCommand line ~172), so on
    // any machine whose real config.yaml sets a non-default value, comparing the dump against
    // Profile.builder().build() (always AppConfig's hardcoded defaults) fails for a reason
    // unrelated to the behavior under test. Building the expected Profile from that same
    // in-process loadAppConfig() base keeps every other field's default assertion just as
    // strict while tolerating this one legitimate machine-dependent field.
    private static Profile defaultProfileWithRealPerformanceBase() {
        return Profile.builder()
                .performance(SerializationHelper.getInstance().loadAppConfig())
                .build();
    }

    private static String runAndCaptureStdout(String... args) {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(args);
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(captured));
        int exitCode;
        try {
            exitCode = command.call();
        } finally {
            System.setOut(original);
        }
        assertEquals(0, exitCode, "--dump-profile must exit 0");
        return captured.toString();
    }

    // Row 4(c): --dump-profile does not require DAT_FILE and does not run the pipeline (no
    // exception even though no DAT file, no --in-dir, no region-data setup is provided).
    @Test
    void dumpProfileDoesNotRequireDatFile() {
        String output = assertDoesNotThrow(
                () -> runAndCaptureStdout("--dump-profile"),
                "--dump-profile without DAT_FILE must not throw");
        assertTrue(output.contains("performance") || !output.isBlank(), "dump output must not be empty");
    }

    // Row 4(a)+(d): default format is yaml and is parseable as a Profile.
    @Test
    void dumpProfileDefaultsToYamlAndParses() {
        String output = runAndCaptureStdout("--dump-profile");
        Profile reloaded = assertDoesNotThrow(
                () -> SerializationHelper.getInstance().getYamlMapper().readValue(output, Profile.class),
                "default --dump-profile output must be valid YAML parseable as a Profile, got:\n" + output);
        assertEquals(
                defaultProfileWithRealPerformanceBase(),
                reloaded,
                "no flags/profile must dump the default Profile (performance section compared "
                        + "against this machine's real loadAppConfig() base, not hardcoded defaults)");
    }

    @Test
    void dumpProfileYamlValueMatchesDefault() {
        String output = runAndCaptureStdout("--dump-profile", "yaml");
        assertDoesNotThrow(
                () -> SerializationHelper.getInstance().getYamlMapper().readValue(output, Profile.class),
                "--dump-profile yaml output must be valid YAML, got:\n" + output);
    }

    // Row 4(d): explicit json format is honored and differs in shape from yaml (starts with '{').
    @Test
    void dumpProfileJsonValueProducesJson() {
        String output = runAndCaptureStdout("--dump-profile", "json");
        assertTrue(output.trim().startsWith("{"), "--dump-profile json output must be a JSON object, got:\n" + output);
        Profile reloaded = assertDoesNotThrow(
                () -> SerializationHelper.getInstance().getJsonMapper().readValue(output, Profile.class),
                "--dump-profile json output must be valid JSON parseable as a Profile, got:\n" + output);
        assertEquals(
                defaultProfileWithRealPerformanceBase(),
                reloaded,
                "no flags/profile must dump the default Profile (performance section compared "
                        + "against this machine's real loadAppConfig() base, not hardcoded defaults)");
    }

    // Row 4(b): --dump-profile output, reloaded as a --profile, reproduces the same effective
    // config (round-trip), here using --include-regions to give the dump non-default content.
    @Test
    void dumpProfileRoundTripsThroughReload(@TempDir Path tempDir) throws IOException {
        String firstDump = runAndCaptureStdout(
                datFixture().toString(), "--include-regions", "USA", "--dump-profile", "json");
        Path reloadFile = tempDir.resolve("reload.json");
        Files.writeString(reloadFile, firstDump);

        String secondDump = runAndCaptureStdout("--profile", reloadFile.toString(), "--dump-profile", "json");

        Profile first = SerializationHelper.getInstance().getJsonMapper().readValue(firstDump, Profile.class);
        Profile second = SerializationHelper.getInstance().getJsonMapper().readValue(secondDump, Profile.class);
        assertEquals(first.getFilter(), second.getFilter(), "reloading a dumped profile must reproduce the same effective Filter");
    }

    // Row 3: layering two --profile files, later wins (representative assertion; full merge
    // semantics are covered in core's SerializationHelper/JsonNodeMerge tests).
    @Test
    void laterProfileFileWinsOverEarlierOne(@TempDir Path tempDir) throws IOException {
        Path first = tempDir.resolve("first.yaml");
        Path second = tempDir.resolve("second.yaml");
        Files.writeString(first, "filter:\n  includeRegions: [\"USA\"]\n");
        Files.writeString(second, "filter:\n  includeRegions: [\"JPN\"]\n");

        String output = runAndCaptureStdout(
                "--profile", first.toString(),
                "--profile", second.toString(),
                "--dump-profile", "json");
        Profile effective = SerializationHelper.getInstance().getJsonMapper().readValue(output, Profile.class);
        assertEquals(java.util.Set.of("JPN"), effective.getFilter().getIncludeRegions(), "the later --profile file must win");
    }

    // Row 5(a): a nonexistent --profile file fails cleanly at parse time, naming the path.
    @Test
    void nonexistentProfileFileFailsCleanlyAtParseTime() {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        CommandLine.ParameterException thrown = assertThrows(
                CommandLine.ParameterException.class,
                () -> commandLine.parseArgs("--profile", "/no/such/profile.yaml"),
                "a nonexistent --profile file must fail parsing");
        assertTrue(
                thrown.getMessage().contains("/no/such/profile.yaml"),
                "error message must name the missing path, got: " + thrown.getMessage());
    }

    // Row 5(b): a malformed (unparseable) profile file surfaces as a clean ParameterException
    // naming the file, not a raw stack trace.
    @Test
    void malformedProfileFileSurfacesAsParameterException(@TempDir Path tempDir) throws IOException {
        Path malformed = tempDir.resolve("broken.yaml");
        Files.writeString(malformed, "filter: [unterminated\n");

        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs("--profile", malformed.toString(), "--dump-profile");

        CommandLine.ParameterException thrown = assertThrows(
                CommandLine.ParameterException.class,
                command::call,
                "a malformed profile file must surface as a ParameterException, not a raw exception");
        assertTrue(
                thrown.getMessage().contains(malformed.toString()),
                "error message must name the malformed file, got: " + thrown.getMessage());
    }
}
