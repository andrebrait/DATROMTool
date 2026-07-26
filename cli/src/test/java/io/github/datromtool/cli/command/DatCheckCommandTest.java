package io.github.datromtool.cli.command;

import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.converter.DatafileConverter;
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
