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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end CLI coverage matrix for issue #19 step 3: {@code --clonelist}/{@code
 * --retool-metadata} (rows 1, 2, 4), the {@code minimumVersion} compatibility gate (row 3),
 * profile-supplied {@code input.clonelists}/{@code input.metadata} (row 5), and the {@code
 * --dump-profile} round-trip (row 7). Row 6 (profile {@code input.dats} execution) is pinned by
 * {@link OneGameOneRomCommandProfileDatsExecutionTest}; row 8 (oracle - unaffected runs stay
 * green) is every other test in this suite, unedited.
 *
 * <p><b>Fixture note on the {@code minimumVersion} gate</b> (see {@code
 * io.github.datromtool.retool.RetoolFileResolver}'s Javadoc for the full explanation): this
 * repository's own real, upstream-pinned clone list fixture ({@code
 * real-atari-2600-no-intro.json}, a verbatim copy of {@code core}'s {@code
 * atari-2600-no-intro.json}) declares {@code minimumVersion: "2.4.8"}, which is incompatible
 * with DATROMTool's actual running pre-1.0 version - so it is used here for the
 * <em>rejection</em> test, not the successful-grouping one. The successful-grouping test instead
 * uses a small, hand-authored clone list ({@code Atari - Atari 2600 (No-Intro).json}) containing
 * the same real "Air Raiders"/"Bogey Blaster"/"Top Gun" group upstream declares, with a
 * deliberately low {@code minimumVersion} ({@code "0.0.1"}) so it is compatible with any
 * DATROMTool version. This is a deviation from reusing the full real fixture end to end, made
 * necessary by the version-scheme mismatch documented in {@code RetoolFileResolver}.
 */
class OneGameOneRomCommandRetoolTest {

    private static Path fixture(String name) {
        try {
            return Paths.get(OneGameOneRomCommandRetoolTest.class
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

    private static String runAndCaptureStdout(OneGameOneRomCommand command, CommandLine commandLine) {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(captured));
        try {
            int exitCode = command.call();
            assertEquals(0, exitCode, "run must exit 0, captured output so far:\n" + captured);
        } finally {
            System.setOut(original);
        }
        return captured.toString();
    }

    // --- Row 1: --clonelist end-to-end grouping ---

    @Test
    void clonelistGroupsAirRaidersVariantsIntoOneEntry() {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--clonelist", fixture("retool/clonelists/Atari - Atari 2600 (No-Intro).json").toString(),
                fixture("datafiles/atari-air-raiders.dat").toString());

        String output = runAndCaptureStdout(command, commandLine);

        int matches = 0;
        for (String name : new String[]{"Air Raiders (USA)", "Bogey Blaster (Europe)", "Top Gun (Germany)"}) {
            if (output.contains(name)) {
                matches++;
            }
        }
        assertEquals(1, matches, "clone list must group all three variants into exactly one 1G1R entry, got:\n" + output);
    }

    // --- Row 4: directory auto-match resolves the right file by DAT header name ---

    @Test
    void clonelistDirectoryAutoMatchesByHeaderName() {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--clonelist", fixture("retool/clonelists").toString(),
                fixture("datafiles/atari-air-raiders.dat").toString());

        String output = runAndCaptureStdout(command, commandLine);

        int matches = 0;
        for (String name : new String[]{"Air Raiders (USA)", "Bogey Blaster (Europe)", "Top Gun (Germany)"}) {
            if (output.contains(name)) {
                matches++;
            }
        }
        assertEquals(1, matches, "directory auto-match must resolve the same grouping as the explicit file, got:\n" + output);
    }

    @Test
    void clonelistDirectoryWithNoMatchFailsClearly(@TempDir Path emptyDir) {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--clonelist", emptyDir.toString(),
                fixture("datafiles/atari-air-raiders.dat").toString());

        CommandLine.ParameterException ex = assertThrows(CommandLine.ParameterException.class, command::call);
        assertTrue(ex.getMessage().contains(emptyDir.toString()), "error must name the directory searched, got: " + ex.getMessage());
        assertTrue(
                ex.getMessage().contains("Atari - Atari 2600 (No-Intro)"),
                "error must name the DAT header name searched, got: " + ex.getMessage());
    }

    // --- Row 3: minimumVersion compatibility gate ---

    @Test
    void incompatibleClonelistFailsNamingFileAndBothVersions() {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        Path realClonelist = fixture("retool/clonelists/real-atari-2600-no-intro.json");
        commandLine.parseArgs(
                "--clonelist", realClonelist.toString(),
                fixture("datafiles/atari-air-raiders.dat").toString());

        CommandLine.ParameterException ex = assertThrows(CommandLine.ParameterException.class, command::call);
        assertTrue(ex.getMessage().contains(realClonelist.toString()), "error must name the file, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("2.4.8"), "error must name the clone list's minimumVersion, got: " + ex.getMessage());
        assertTrue(
                ex.getMessage().contains(SerializationHelper.getInstance().getVersionString()),
                "error must name the running DATROMTool version, got: " + ex.getMessage());
    }

    @Test
    void compatibleClonelistProceeds() {
        // Same fixture as clonelistGroupsAirRaidersVariantsIntoOneEntry - restated here as an
        // explicit "compatible -> proceeds" pin distinct from the grouping assertion above.
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--clonelist", fixture("retool/clonelists/Atari - Atari 2600 (No-Intro).json").toString(),
                fixture("datafiles/atari-air-raiders.dat").toString());
        String output = runAndCaptureStdout(command, commandLine);
        assertFalse(output.isBlank(), "a compatible clone list must let the run proceed to produce output");
    }

    // --- Row 2: --retool-metadata end-to-end language enrichment ---

    @Test
    void retoolMetadataEnrichesLanguagelessDatSoIncludeLanguagesMatches() {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--retool-metadata", fixture("retool/metadata/Test Metadata DAT.json").toString(),
                "--include-languages", "en",
                fixture("datafiles/languageless.dat").toString());

        String output = runAndCaptureStdout(command, commandLine);
        assertTrue(
                output.contains("Mystery Game (Japan)"),
                "metadata-enriched 'en' language must satisfy --include-languages en, got:\n" + output);
    }

    @Test
    void withoutMetadataLanguagelessGameFailsIncludeLanguagesFilter() {
        // Discriminating control: without --retool-metadata, "Mystery Game (Japan)" has no
        // explicit language tag, so its language is only region-derived ("ja" for Japan, see
        // region-data.yaml) - --include-languages en must exclude it, proving the positive test
        // above is actually driven by metadata enrichment, not some other path to "en".
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--include-languages", "en",
                fixture("datafiles/languageless.dat").toString());

        String output = runAndCaptureStdout(command, commandLine);
        assertFalse(
                output.contains("Mystery Game (Japan)"),
                "without metadata, region-derived 'ja' must not satisfy --include-languages en, got:\n" + output);
    }

    // --- Row 5: profile-supplied input.clonelists/input.metadata behave like flags; flags win ---

    @Test
    void profileClonelistsPathBehavesSameAsFlag(@TempDir Path tempDir) throws IOException {
        Path profile = tempDir.resolve("profile.yaml");
        Files.writeString(
                profile,
                "input:\n  clonelists: \"" + fixture("retool/clonelists/Atari - Atari 2600 (No-Intro).json") + "\"\n");

        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--profile", profile.toString(),
                fixture("datafiles/atari-air-raiders.dat").toString());

        String output = runAndCaptureStdout(command, commandLine);
        int matches = 0;
        for (String name : new String[]{"Air Raiders (USA)", "Bogey Blaster (Europe)", "Top Gun (Germany)"}) {
            if (output.contains(name)) {
                matches++;
            }
        }
        assertEquals(1, matches, "profile-supplied input.clonelists must behave exactly like --clonelist, got:\n" + output);
    }

    @Test
    void explicitClonelistFlagWinsOverProfileClonelists(@TempDir Path tempDir) throws IOException {
        // The profile points at a directory with no matching file (would fail if actually used);
        // the explicit flag points at the real compatible fixture. The run must succeed, proving
        // the flag - not the profile's (bogus) value - is what got used.
        Path profile = tempDir.resolve("profile.yaml");
        Files.writeString(profile, "input:\n  clonelists: \"" + tempDir + "\"\n");

        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--profile", profile.toString(),
                "--clonelist", fixture("retool/clonelists/Atari - Atari 2600 (No-Intro).json").toString(),
                fixture("datafiles/atari-air-raiders.dat").toString());

        String output = runAndCaptureStdout(command, commandLine);
        int matches = 0;
        for (String name : new String[]{"Air Raiders (USA)", "Bogey Blaster (Europe)", "Top Gun (Germany)"}) {
            if (output.contains(name)) {
                matches++;
            }
        }
        assertEquals(1, matches, "explicit --clonelist must win over the profile's input.clonelists, got:\n" + output);
    }

    // --- Row 7: --dump-profile round-trip with the new input fields populated ---

    @Test
    void dumpProfileRoundTripsClonelistAndMetadataPaths() throws Exception {
        Path clonelistPath = fixture("retool/clonelists/Atari - Atari 2600 (No-Intro).json");
        Path metadataPath = fixture("retool/metadata/Test Metadata DAT.json");

        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--clonelist", clonelistPath.toString(),
                "--retool-metadata", metadataPath.toString(),
                "--dump-profile", "json");

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

        Profile dumped = SerializationHelper.getInstance().getJsonMapper().readValue(captured.toString(), Profile.class);
        assertEquals(clonelistPath, dumped.getInput().getClonelists(), "dumped profile must round-trip input.clonelists");
        assertEquals(metadataPath, dumped.getInput().getMetadata(), "dumped profile must round-trip input.metadata");
    }
}
