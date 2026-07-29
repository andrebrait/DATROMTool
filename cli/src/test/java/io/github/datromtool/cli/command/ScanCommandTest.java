package io.github.datromtool.cli.command;

import io.github.datromtool.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Coverage matrix for issue #17's {@code scan} subcommand.
 *
 * <p>Fixture: {@code test-data/data/scan-test-files/0016384} (a small dir already used by
 * {@code core}'s {@code FileScannerTest}, with known hashes in {@code test-data/data/CRC32SUMS}/
 * {@code SHA1SUMS}), kept lean here to a couple of known hashes rather than the full fixture set.
 */
class ScanCommandTest {

    private static final Path FIXTURE_DIR =
            Paths.get("../test-data/data/scan-test-files/0016384").toAbsolutePath().normalize();

    // The plain (non-archived, non-headered) 16384-byte fixture file's known digest.
    private static final String KNOWN_CRC = "4291626c";
    private static final String KNOWN_MD5 = "25fb98425aea8268ba4e26c00eef4a00";
    private static final String KNOWN_SHA1 = "c502ca2c1cd3e7301028fe4f29ac51101c3866e7";

    // The second line of the errored-scan notice; pinned separately from the first so a
    // regression that misroutes only the log pointer cannot slip through.
    private static final String LOG_FILE_NOTICE = "Check the generated log file for details:";

    private static int run(ScanCommand command, String... args) {
        CommandLine commandLine = new CommandLine(command);
        commandLine.parseArgs(args);
        return command.call();
    }

    private static String captureStdout(Runnable action) {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(captured));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return captured.toString();
    }

    // Row 1: scanning a fixture dir yields the known entries/hashes (default out-mode: yaml).
    @Test
    void scanningFixtureDirYieldsKnownHashes() {
        assertTrue(Files.isDirectory(FIXTURE_DIR), "fixture dir must exist: " + FIXTURE_DIR);
        String stdout = captureStdout(() ->
                assertEquals(0, run(new ScanCommand(), FIXTURE_DIR.toString()), "scan must exit 0"));
        assertTrue(stdout.contains(KNOWN_CRC), "report must contain the known CRC, got:\n" + stdout);
        assertTrue(stdout.contains(KNOWN_MD5), "report must contain the known MD5, got:\n" + stdout);
        assertTrue(stdout.contains(KNOWN_SHA1), "report must contain the known SHA1, got:\n" + stdout);
        assertTrue(stdout.contains("16384"), "report must contain the known size, got:\n" + stdout);
    }

    // Row 2(a): --out-mode json produces parseable JSON.
    @Test
    void outModeJsonProducesParseableJson() {
        String stdout = captureStdout(() -> assertEquals(
                0,
                run(new ScanCommand(), FIXTURE_DIR.toString(), "--out-mode", "json"),
                "scan --out-mode json must exit 0"));
        ArrayNode node = (ArrayNode) new JsonMapper().readTree(stdout);
        assertFalse(node.isEmpty(), "parsed JSON report must contain entries");
        assertTrue(stdout.contains(KNOWN_CRC), "JSON report must contain the known CRC");
    }

    // Row 2(b): --out-mode yaml (also the default) produces parseable YAML.
    @Test
    void outModeYamlProducesParseableYaml() {
        String stdout = captureStdout(() -> assertEquals(
                0,
                run(new ScanCommand(), FIXTURE_DIR.toString(), "--out-mode", "yaml"),
                "scan --out-mode yaml must exit 0"));
        ArrayNode node = (ArrayNode) new YAMLMapper().readTree(stdout);
        assertFalse(node.isEmpty(), "parsed YAML report must contain entries");
    }

    // Row 2(c): --out-mode xml produces parseable XML.
    @Test
    void outModeXmlProducesParseableXml() {
        String stdout = captureStdout(() -> assertEquals(
                0,
                run(new ScanCommand(), FIXTURE_DIR.toString(), "--out-mode", "xml"),
                "scan --out-mode xml must exit 0"));
        // A successful parse (no exception) is the assertion: a bare XmlMapper.readTree() on a
        // non-well-formed document throws.
        var node = new XmlMapper().readTree(stdout);
        assertTrue(node.has("item"), "parsed XML report must contain <item> entries, got:\n" + stdout);
    }

    // Row 2(d): --out-file writes a parseable file instead of stdout.
    @Test
    void outFileWritesParseableFile(@TempDir Path tempDir) throws IOException {
        Path outFile = tempDir.resolve("report.json");
        String stdout = captureStdout(() -> assertEquals(
                0,
                run(new ScanCommand(),
                        FIXTURE_DIR.toString(), "--out-mode", "json", "--out-file", outFile.toString()),
                "scan --out-file must exit 0"));
        assertEquals("", stdout, "stdout must be empty when --out-file is set, got:\n" + stdout);
        String fileContent = Files.readString(outFile);
        ArrayNode node = (ArrayNode) new JsonMapper().readTree(fileContent);
        assertFalse(node.isEmpty(), "output file must contain a parseable, non-empty JSON report");
        assertTrue(fileContent.contains(KNOWN_CRC), "output file must contain the known CRC");
    }

    // Row 3: a nonexistent dir fails at parse time (ExistingDirectoryConverter).
    @Test
    void nonexistentDirFailsAtParseTime() {
        ScanCommand command = new ScanCommand();
        CommandLine commandLine = new CommandLine(command);
        CommandLine.ParameterException ex = assertThrows(
                CommandLine.ParameterException.class,
                () -> commandLine.parseArgs("/no/such/directory"),
                "a nonexistent dir must fail parsing");
        assertTrue(ex.getMessage().contains("/no/such/directory"),
                "error must name the offending path, got: " + ex.getMessage());
    }

    // Row 4: PerformanceOptions scanner knobs parse and reach the effective FileScannerConfig.
    @Test
    void scanThreadsOptionReachesTheFileScannerConfig() {
        ScanCommand command = new ScanCommand();
        CommandLine commandLine = new CommandLine(command);
        commandLine.parseArgs("--scan-threads", "3", "--scan-buffer", "64KB", FIXTURE_DIR.toString());
        AppConfig.FileScannerConfig config = command.resolveScannerConfig();
        assertEquals(3, config.getThreads().value(),
                "--scan-threads 3 must reach the effective FileScannerConfig");
        assertEquals(64 * 1024, config.getDefaultBufferSize().bytes(),
                "--scan-buffer 64KB must reach the effective FileScannerConfig");
    }

    // Row 5: the stdout report is not polluted by progress bar/log output (jline is forced to
    // stderr; logback's cli config only ever writes to a file, never to the console).
    @Test
    void stdoutReportIsNotPollutedByProgressOrLogOutput() {
        String stdout = captureStdout(() ->
                assertEquals(0, run(new ScanCommand(), FIXTURE_DIR.toString()), "scan must exit 0"));
        assertFalse(stdout.contains("\u001B"), "stdout must not contain ANSI escape sequences, got:\n" + stdout);
        assertFalse(stdout.contains("Scanning input directories"),
                "stdout must not contain progress bar text, got:\n" + stdout);
        // The whole captured stream must itself parse as a single YAML document (default
        // out-mode): any interleaved noise line would break the parse.
        ArrayNode node = (ArrayNode) new YAMLMapper().readTree(stdout);
        assertFalse(node.isEmpty(), "stdout must parse cleanly as the YAML report alone");
    }

    // Row 7: an empty directory scan must not crash. Previously
    // CommandLineProgressBar.printMainBar divided by totalItems == 0 (ArithmeticException) once
    // FileScanner.reportTotalItems(0) fired for a dir with nothing to scan.
    @Test
    void emptyDirectoryScanProducesEmptyReport(@TempDir Path emptyDir) {
        String stdout = captureStdout(() -> assertEquals(
                0,
                run(new ScanCommand(), emptyDir.toString(), "--out-mode", "json"),
                "scan of an empty dir must exit 0"));
        ArrayNode node = (ArrayNode) new JsonMapper().readTree(stdout);
        assertTrue(node.isEmpty(), "report for an empty dir must be an empty JSON array, got:\n" + stdout);
    }

    // Row 8: fewer files than configured scan threads must not crash. Previously
    // CommandLineProgressBar.reportAllFinished/getFinalAverage NPE'd indexing threadLineData
    // slots for threads that never received reportStart.
    @Test
    void scanningFewerFilesThanThreadsSucceeds(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("a.bin"), "aa");
        Files.writeString(tempDir.resolve("b.bin"), "bb");
        String stdout = captureStdout(() -> assertEquals(
                0,
                run(new ScanCommand(),
                        tempDir.toString(), "--out-mode", "json", "--scan-threads", "4"),
                "scan with more threads than files must exit 0"));
        ArrayNode node = (ArrayNode) new JsonMapper().readTree(stdout);
        assertEquals(2, node.size(), "report must contain both scanned files, got:\n" + stdout);
    }

    // Row 6: no dirs at all is a missing-parameter error, exit 2.
    @Test
    void noDirsFailsWithMissingParameterExitCode2() {
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(capturedErr));
        int exitCode;
        try {
            exitCode = new CommandLine(new ScanCommand()).execute();
        } finally {
            System.setErr(originalErr);
        }
        assertEquals(2, exitCode, "missing DIR parameter must exit with code 2, stderr was:\n" + capturedErr);
    }
    // Row 9: a scan that could not list part of the tree reports a truncated report through its
    // exit code instead of passing it off as a complete one (issue #37).
    @Test
    void unlistableSubdirectoryMakesScanExitNonZero(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("rom.bin"), new byte[16 * 1024]);
        Path unlistable = Files.createDirectory(tempDir.resolve("unlistable"));
        assumeTrue(makeUnlistable(unlistable), "this filesystem/user cannot make a directory unlistable");
        try {
            captureStdout(() -> assertEquals(
                    1,
                    run(new ScanCommand(), tempDir.toString(), "--out-mode", "json"),
                    "a scan that could not list a subdirectory must not exit 0"));
        } finally {
            Files.setPosixFilePermissions(unlistable, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE));
        }
    }

    // Row 10: an errored scan must say so on the terminal too (issue #75). Failures only ever
    // reach datromtool.log — logback's CLI config has no console appender on purpose — so an
    // exit code alone leaves an interactive user with no hint that the report is incomplete.
    @Test
    void erroredScanReportsFailuresOnStderr(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("rom.bin"), new byte[16 * 1024]);
        Path unlistable = Files.createDirectory(tempDir.resolve("unlistable"));
        assumeTrue(makeUnlistable(unlistable), "this filesystem/user cannot make a directory unlistable");
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(capturedErr));
        String stdout;
        try {
            stdout = captureStdout(() -> assertEquals(
                    1,
                    run(new ScanCommand(), tempDir.toString(), "--out-mode", "json"),
                    "a scan that could not list a subdirectory must not exit 0"));
        } finally {
            System.setErr(originalErr);
            Files.setPosixFilePermissions(unlistable, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE));
        }
        String stderr = capturedErr.toString();
        assertTrue(stderr.contains("Errors during execution detected"),
                "stderr must announce that the scan hit errors, got:\n" + stderr);
        assertTrue(stderr.contains(LOG_FILE_NOTICE),
                "stderr must point at the log file holding the details, got:\n" + stderr);
        assertTrue(stderr.contains("datromtool.log"),
                "the log-file notice must name the log file itself, got:\n" + stderr);
        // The report itself stays machine-readable: both notice lines belong on stderr only.
        assertFalse(stdout.contains("Errors during execution detected"),
                "the error notice must not pollute the stdout report, got:\n" + stdout);
        assertFalse(stdout.contains(LOG_FILE_NOTICE),
                "the log-file notice must not pollute the stdout report, got:\n" + stdout);
    }

    // Row 11: a clean scan stays quiet — the notice is not an unconditional banner.
    @Test
    void cleanScanPrintsNoErrorNotice(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("rom.bin"), new byte[16 * 1024]);
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(capturedErr));
        try {
            captureStdout(() -> assertEquals(
                    0,
                    run(new ScanCommand(), tempDir.toString(), "--out-mode", "json"),
                    "a clean scan must exit 0"));
        } finally {
            System.setErr(originalErr);
        }
        String stderr = capturedErr.toString();
        assertFalse(stderr.contains("Errors during execution detected"),
                "a clean scan must not claim errors, got:\n" + stderr);
        assertFalse(stderr.contains(LOG_FILE_NOTICE),
                "a clean scan must not send the user to the log file, got:\n" + stderr);
    }

    /**
     * Strips every permission bit and confirms the directory really became unlistable — it does
     * not on a filesystem without POSIX permissions, nor for a superuser.
     */
    private static boolean makeUnlistable(Path directory) {
        try {
            Files.setPosixFilePermissions(directory, Set.of());
        } catch (UnsupportedOperationException | IOException e) {
            return false;
        }
        try (DirectoryStream<Path> ignored = Files.newDirectoryStream(directory)) {
            return false;
        } catch (IOException expected) {
            return true;
        }
    }

    @Test
    void progressOutputStaysPinnedToStderr() {
        // jline writes progress at file-descriptor level, bypassing System.setOut capture,
        // so no behavioral test can catch a regression of this choice; the end-to-end
        // property is jar-probe-verified. This pin fails if anyone flips the mode.
        assertEquals(
                org.jline.terminal.TerminalBuilder.SystemOutput.ForcedSysErr,
                ScanCommand.PROGRESS_OUTPUT,
                "scan progress must go to stderr so stdout stays machine-readable");
    }
}
