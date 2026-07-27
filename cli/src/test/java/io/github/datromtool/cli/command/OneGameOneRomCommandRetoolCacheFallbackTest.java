package io.github.datromtool.cli.command;

import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.converter.DatafileConverter;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage matrix row 6 for issue #44 step 2: falling back to the Retool update cache's
 * {@code clonelists}/{@code metadata} directories when neither {@code --clonelist}/
 * {@code --retool-metadata} nor their profile equivalents are given.
 *
 * <p><b>Mandatory red-first proof</b> ({@link #cacheFallbackAppliesWhenNoClonelistFlagGiven}):
 * executed RED before {@code OneGameOneRomCommand}'s {@code retoolCacheDir} field/fallback block
 * landed - the test file did not even compile (no such field), and behaviorally there was no
 * fallback at all. Frozen here byte-identical; green once the fallback landed.
 *
 * <p>Uses the same fixtures/helpers as {@link OneGameOneRomCommandRetoolTest} (that class's row 4
 * {@code clonelistDirectoryAutoMatchesByHeaderName} already proves the header-name auto-match
 * itself works for an explicit {@code --clonelist} directory; this class only proves the new
 * fallback wiring reaches that same, unchanged auto-match).
 */
class OneGameOneRomCommandRetoolCacheFallbackTest {

    private static final String[] AIR_RAIDERS_VARIANT_NAMES =
            {"Air Raiders (USA)", "Bogey Blaster (Europe)", "Top Gun (Germany)"};

    private static Path fixture(String name) {
        try {
            return Paths.get(OneGameOneRomCommandRetoolCacheFallbackTest.class
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

    private static String runAndCaptureStdout(OneGameOneRomCommand command) {
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

    private static int countAirRaidersVariantMatches(String output) {
        int matches = 0;
        for (String name : AIR_RAIDERS_VARIANT_NAMES) {
            if (output.contains(name)) {
                matches++;
            }
        }
        return matches;
    }

    // --- Row 6(a): fallback applies when the cache dir exists and no --clonelist flag is given ---

    @Test
    void cacheFallbackAppliesWhenNoClonelistFlagGiven(@TempDir Path tempDir) throws IOException {
        Path cacheDir = tempDir.resolve("retool");
        Path cachedClonelists = cacheDir.resolve("clonelists");
        Files.createDirectories(cachedClonelists);
        Files.copy(
                fixture("retool/clonelists/Atari - Atari 2600 (No-Intro).json"),
                cachedClonelists.resolve("Atari - Atari 2600 (No-Intro).json"));

        OneGameOneRomCommand command = new OneGameOneRomCommand();
        command.retoolCacheDir = cacheDir;
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(fixture("datafiles/atari-air-raiders.dat").toString());

        String output = runAndCaptureStdout(command);

        assertEquals(1, countAirRaidersVariantMatches(output),
                "cache-fallback clone list must group all three variants into one 1G1R entry, got:\n" + output);
    }

    // --- Row 6(b): explicit --clonelist still wins over the cache fallback ---

    @Test
    void explicitClonelistFlagWinsOverCacheFallback(@TempDir Path tempDir) throws IOException {
        // The cache dir exists but is empty (would fail to auto-match if it were actually used);
        // the explicit --clonelist flag points at the real, compatible fixture. The run must
        // succeed and group correctly, proving the flag - not the cache - is what got used.
        Path cacheDir = tempDir.resolve("retool");
        Files.createDirectories(cacheDir.resolve("clonelists"));

        OneGameOneRomCommand command = new OneGameOneRomCommand();
        command.retoolCacheDir = cacheDir;
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--clonelist", fixture("retool/clonelists/Atari - Atari 2600 (No-Intro).json").toString(),
                fixture("datafiles/atari-air-raiders.dat").toString());

        String output = runAndCaptureStdout(command);

        assertEquals(1, countAirRaidersVariantMatches(output),
                "explicit --clonelist must win over the cache fallback, got:\n" + output);
    }

    // --- Row 6(c): oracle - an absent cache dir leaves today's behavior exactly unchanged ---

    @Test
    void absentCacheDirLeavesBehaviorUnchanged(@TempDir Path tempDir) {
        // foo-bar-family.dat declares a real cloneof relationship in the DAT itself (Bar (Europe)
        // is cloneof Foo (USA)), so OneGameOneRom's pre-existing "DAT files lack Parent/Clone
        // information" validation already clears without any clone list at all - unlike
        // clean.dat/minimal.dat (single isolated entries) or atari-air-raiders.dat (which needs a
        // clone list to group at all). The only variable under test here is retoolCacheDir
        // pointing at a directory that was never created: the run must proceed exactly as it
        // does with the field left at its production default.
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        command.retoolCacheDir = tempDir.resolve("never-created");
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(fixture("datafiles/foo-bar-family.dat").toString());

        String output = runAndCaptureStdout(command);

        assertFalse(output.isBlank(), "an absent cache dir must leave today's behavior exactly unchanged");
        assertTrue(output.contains("Foo (USA)"), "output must contain the DAT's own parent entry, got:\n" + output);
    }

    // --- CodeRabbit review round (PR #45), finding 4: only clone-list cache fallback had
    // end-to-end coverage; a regression in the metadata half of the same fallback block stayed
    // green. Same shape as OneGameOneRomCommandRetoolTest's
    // retoolMetadataEnrichesLanguagelessDatSoIncludeLanguagesMatches, but via the cache fallback
    // (retoolCacheDir) instead of an explicit --retool-metadata flag. ---

    @Test
    void metadataCacheFallbackAppliesWhenNoMetadataFlagGiven(@TempDir Path tempDir) throws IOException {
        Path cacheDir = tempDir.resolve("retool");
        Path cachedMetadata = cacheDir.resolve("metadata");
        Files.createDirectories(cachedMetadata);
        Files.copy(
                fixture("retool/metadata/Test Metadata DAT.json"),
                cachedMetadata.resolve("Test Metadata DAT.json"));

        OneGameOneRomCommand command = new OneGameOneRomCommand();
        command.retoolCacheDir = cacheDir;
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--include-languages", "en",
                fixture("datafiles/languageless.dat").toString());

        String output = runAndCaptureStdout(command);

        assertTrue(
                output.contains("Mystery Game (Japan)"),
                "cache-fallback metadata must enrich the languageless DAT so --include-languages en "
                        + "matches, got:\n" + output);
    }

    // --- CodeRabbit review round, finding 6: the fallback's only signal was log.info, which
    // cli/src/main/resources/logback.xml routes to the FILE appender only - never the console -
    // so a user running with a stale/absent-flag cache silently got different 1G1R grouping with
    // no on-screen indication at all. A notice must reach stderr (matching RetoolUpdateCommand's
    // own stderr progress convention), and stdout must stay clean/machine-parseable. ---

    @Test
    void cacheFallbackNoticeAppearsOnStderrNotStdout(@TempDir Path tempDir) throws IOException {
        Path cacheDir = tempDir.resolve("retool");
        Path cachedClonelists = cacheDir.resolve("clonelists");
        Files.createDirectories(cachedClonelists);
        Files.copy(
                fixture("retool/clonelists/Atari - Atari 2600 (No-Intro).json"),
                cachedClonelists.resolve("Atari - Atari 2600 (No-Intro).json"));

        OneGameOneRomCommand command = new OneGameOneRomCommand();
        command.retoolCacheDir = cacheDir;
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(fixture("datafiles/atari-air-raiders.dat").toString());

        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(capturedErr));
        String stdout;
        try {
            stdout = runAndCaptureStdout(command);
        } finally {
            System.setErr(originalErr);
        }

        String stderr = capturedErr.toString();
        assertTrue(stderr.contains(cachedClonelists.toString()),
                "cache fallback must be visible on stderr (console), naming the directory used, got:\n" + stderr);
        assertFalse(stdout.contains(cachedClonelists.toString()),
                "stdout must stay clean/machine-parseable - the fallback notice must not leak into it, got:\n" + stdout);
    }

    // --- Row 7: multi-DAT interaction - fallback does not apply, no error, run proceeds ---

    @Test
    void cacheFallbackDoesNotApplyWithMultipleDats(@TempDir Path tempDir) throws IOException {
        // Deliberate design choice (see OneGameOneRomCommand's fallback block Javadoc): with more
        // than one DAT, the fallback simply does not apply (no per-DAT resolution support, and
        // the user never asked for --clonelist/--retool-metadata) - it must NOT surface the
        // explicit-flag multi-DAT rejection either, since no flag/fallback path is actually used.
        Path cacheDir = tempDir.resolve("retool");
        Path cachedClonelists = cacheDir.resolve("clonelists");
        Files.createDirectories(cachedClonelists);
        Files.copy(
                fixture("retool/clonelists/Atari - Atari 2600 (No-Intro).json"),
                cachedClonelists.resolve("Atari - Atari 2600 (No-Intro).json"));

        OneGameOneRomCommand command = new OneGameOneRomCommand();
        command.retoolCacheDir = cacheDir;
        CommandLine commandLine = newCommandLine(command);
        // languageless.dat has an internal cloneof relationship, so the combined DAT set is not
        // all-parent - required for the run to clear the pre-existing "DAT files lack
        // Parent/Clone information" validation and actually reach output (same fixture pairing
        // OneGameOneRomCommandRetoolTest#multipleDatsWithoutClonelistOrMetadataStillProceed uses).
        commandLine.parseArgs(
                fixture("datafiles/minimal.dat").toString(),
                fixture("datafiles/languageless.dat").toString());

        String output = runAndCaptureStdout(command);

        assertFalse(output.isBlank(), "multi-DAT run must proceed normally when the fallback is skipped, not error");
    }
}
