package io.github.datromtool.cli.command;

import io.github.datromtool.GameParser;
import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.converter.DatafileConverter;
import io.github.datromtool.cli.converter.DivergenceDetectionConverter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage matrix rows 1-3 and 6 for issue #18's {@code dat check} subcommand: a fixture DAT
 * with a name/DAT-metadata divergence is reported (game name + divergence kind), a clean DAT
 * reports none, the exit code mirrors {@code scan}'s convention (0 clean, 1 divergent), multiple
 * DATs are all processed and grouped per DAT, and stdout stays limited to the report (no
 * progress/logging noise).
 */
class DatCheckCommandTest {

    private static Path fixture(String name) {
        try {
            return Paths.get(DatCheckCommandTest.class
                    .getClassLoader()
                    .getResource("datafiles/" + name)
                    .toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CommandLine newCommandLine(DatCheckCommand command) {
        CommandLine commandLine = new CommandLine(command);
        commandLine.registerConverter(DatafileArgument.class, new DatafileConverter());
        commandLine.registerConverter(GameParser.DivergenceDetection.class, new DivergenceDetectionConverter());
        return commandLine;
    }

    private static int run(ByteArrayOutputStream captured, String... args) {
        DatCheckCommand command = new DatCheckCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(args);
        PrintStream original = System.out;
        System.setOut(new PrintStream(captured));
        try {
            return command.call();
        } finally {
            System.setOut(original);
        }
    }

    // A DAT can legally carry C1 control characters in a game name (XML 1.0 permits them), so
    // the report must render them inertly like every other terminal-bound label (issue #38).
    @Test
    void controlCharactersInAGameNameAreRenderedInertly() {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        run(captured, fixture("control-chars.dat").toString());
        String stdout = captured.toString();

        assertTrue(
                stdout.contains("Evil"),
                "test premise: the offending game must reach the report, got:\n" + stdout);
        assertFalse(
                stdout.contains("\u009B"),
                "a C1 control character from a DAT must not reach the terminal, got:\n" + stdout);
    }

    // Row 1(a) + row 2: a fixture DAT with a known divergence is reported (game name +
    // divergence kind), and the exit code is 1.
    @Test
    void divergentDatReportsGameNameAndDivergenceKindAndExitsOne() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int exitCode = run(out, fixture("divergent.dat").toString());
        String report = out.toString();
        assertEquals(1, exitCode, "a divergent DAT must exit 1");
        assertTrue(report.contains("Test Game (Europe)"),
                "report must name the divergent game, got:\n" + report);
        assertTrue(report.contains("region divergence"),
                "report must name the divergence kind, got:\n" + report);
        assertTrue(report.contains("detected=[EUR]") && report.contains("provided=[ITA]"),
                "report must show detected vs. provided values, got:\n" + report);
    }

    // Row 1(b): a clean DAT reports no divergences and exits 0.
    @Test
    void cleanDatReportsNoDivergencesAndExitsZero() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int exitCode = run(out, fixture("clean.dat").toString());
        String report = out.toString();
        assertEquals(0, exitCode, "a clean DAT must exit 0");
        assertTrue(report.contains("no divergences"),
                "report must state the DAT is clean, got:\n" + report);
        assertFalse(report.contains("divergence:"),
                "a clean report must not list any divergence entries, got:\n" + report);
    }

    // Row 3: multiple DATs are all processed and the report is grouped per DAT.
    @Test
    void multipleDatsAreAllProcessedAndGroupedPerDat() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int exitCode = run(out, fixture("clean.dat").toString(), fixture("divergent.dat").toString());
        String report = out.toString();
        assertEquals(1, exitCode, "any divergent DAT in the batch must exit 1");
        int cleanIndex = report.indexOf(fixture("clean.dat").toString());
        int divergentIndex = report.indexOf(fixture("divergent.dat").toString());
        assertTrue(cleanIndex >= 0 && divergentIndex >= 0,
                "report must include a heading line per input DAT, got:\n" + report);
        assertTrue(report.contains("no divergences"),
                "report must show the clean DAT's own group as clean, got:\n" + report);
        assertTrue(report.contains("region divergence"),
                "report must show the divergent DAT's own group with its finding, got:\n" + report);
    }

    // Correction round: dat check's default divergence mode must be ONE_WAY (not the previously
    // hardcoded ALWAYS), which requires both detected and provided to be non-empty before
    // comparing. A release-less DAT (name implies a region, zero <release> elements at all) is
    // the common real-world No-Intro DAT shape a verifier probe found 5/5 games falsely flagged
    // under ALWAYS. Under ONE_WAY, "provided" is empty, so no comparison is made at all: no
    // divergence, exit 0.
    @Test
    void defaultModeReportsNoDivergencesForAReleaseLessDat() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int exitCode = run(out, fixture("release-less.dat").toString());
        String report = out.toString();
        assertEquals(0, exitCode, "a release-less DAT must not be flagged under the ONE_WAY default");
        assertTrue(report.contains("no divergences"),
                "report must state the release-less DAT is clean under the default mode, got:\n" + report);
    }

    // Escape hatch: --divergence always still flags the same release-less DAT, pinning that the
    // stricter mode remains reachable for users who want it.
    @Test
    void explicitAlwaysFlagsTheSameReleaseLessDat() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int exitCode = run(out, "--divergence", "always", fixture("release-less.dat").toString());
        String report = out.toString();
        assertEquals(1, exitCode, "--divergence always must flag a release-less DAT's name-implied region");
        assertTrue(report.contains("region divergence"),
                "report must show the region divergence under --divergence always, got:\n" + report);
    }

    // Second correction round: the "language" divergence report path (GameParser.detectLanguages
    // -> divergences.add("language", ...) -> this command's per-divergence print loop) had zero
    // test coverage. Pins a language-only divergence is named and reported, and exits 1.
    @Test
    void languageDivergentDatReportsLanguageDivergenceAndExitsOne() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int exitCode = run(out, fixture("lang-divergent.dat").toString());
        String report = out.toString();
        assertEquals(1, exitCode, "a language-divergent DAT must exit 1");
        assertTrue(report.contains("Test Game (En)"),
                "report must name the divergent game, got:\n" + report);
        assertTrue(report.contains("language divergence"),
                "report must name the divergence kind, got:\n" + report);
        assertTrue(report.contains("detected=[en]") && report.contains("provided=[fr]"),
                "report must show detected vs. provided values, got:\n" + report);
    }

    // Pins that a single game with BOTH a region and a language divergence gets both rows in the
    // report, not just one (region and language divergences are collected/printed independently).
    @Test
    void multiDivergentGameReportsBothRegionAndLanguageDivergenceRows() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int exitCode = run(out, fixture("multi-divergent.dat").toString());
        String report = out.toString();
        assertEquals(1, exitCode, "a multi-divergent DAT must exit 1");
        assertTrue(report.contains("Test Game (Europe) (En): region divergence: detected=[EUR], provided=[ITA]"),
                "report must include the region divergence row, got:\n" + report);
        assertTrue(report.contains("Test Game (Europe) (En): language divergence: detected=[en], provided=[fr]"),
                "report must include the language divergence row, got:\n" + report);
        long gameRowCount = report.lines()
                .filter(line -> line.contains("Test Game (Europe) (En)") && line.contains("divergence:"))
                .count();
        assertEquals(2, gameRowCount,
                "both divergence rows must appear under the same game, got:\n" + report);
    }

    // Row 6: stdout carries only the report; no logging/progress noise mixed in.
    @Test
    void stdoutCarriesOnlyTheReport() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        run(out, fixture("clean.dat").toString());
        String report = out.toString();
        assertEquals(
                fixture("clean.dat") + ": no divergences" + System.lineSeparator(),
                report,
                "stdout must contain exactly the report line, got:\n" + report);
    }
}
