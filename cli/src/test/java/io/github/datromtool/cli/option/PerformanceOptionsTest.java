package io.github.datromtool.cli.option;

import io.github.datromtool.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceOptionsTest {

    @CommandLine.Command
    private static final class Holder {

        @CommandLine.ArgGroup(exclusive = false)
        PerformanceOptions performanceOptions = new PerformanceOptions();
    }

    private static CommandLine newCommandLine(Holder holder) {
        return new CommandLine(holder);
    }

    private static PerformanceOptions parse(String... args) {
        Holder holder = new Holder();
        newCommandLine(holder).parseArgs(args);
        return holder.performanceOptions;
    }

    @Test
    void copyThreadsAloneConfiguresTheCopier() {
        PerformanceOptions options = parse("--copy-threads", "3");
        AppConfig.FileCopierConfig merged =
                options.merge(AppConfig.FileCopierConfig.builder().build());
        assertEquals(
                3,
                merged.getThreads().value(),
                "--copy-threads must set the copier thread count");
    }

    @Test
    void scanAndCopyThreadCountsStayIndependent() {
        PerformanceOptions options = parse("--scan-threads", "2", "--copy-threads", "3");
        AppConfig.FileScannerConfig mergedScanner =
                options.merge(AppConfig.FileScannerConfig.builder().build());
        AppConfig.FileCopierConfig mergedCopier =
                options.merge(AppConfig.FileCopierConfig.builder().build());
        assertEquals(
                2,
                mergedScanner.getThreads().value(),
                "--scan-threads must set the scanner thread count");
        assertEquals(
                3,
                mergedCopier.getThreads().value(),
                "--copy-threads must set the copier thread count, not the scanner's value");
    }

    // Row 25
    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void nonPositiveScanThreadsFailsValidation(String value) {
        Holder holder = new Holder();
        CommandLine commandLine = newCommandLine(holder);
        CommandLine.ParameterException thrown = assertThrows(
                CommandLine.ParameterException.class,
                () -> commandLine.parseArgs("--scan-threads", value),
                "--scan-threads " + value + " must be rejected as non-positive");
        assertTrue(
                thrown.getMessage().toLowerCase(Locale.ROOT).contains("positive"),
                "the error must explain threads must be positive, got: " + thrown.getMessage());
    }

    // Row 26
    @Test
    void scanBufferWithUnitSuffixMergesAsExactByteCount() {
        PerformanceOptions options = parse("--scan-buffer", "32KB");
        AppConfig.FileScannerConfig merged =
                options.merge(AppConfig.FileScannerConfig.builder().build());
        assertEquals(
                32 * 1024,
                merged.getDefaultBufferSize().bytes(),
                "--scan-buffer 32KB must merge as exactly 32768 bytes");
    }

    @Test
    void scanBufferOneMegabyteMergesAsExactByteCount() {
        PerformanceOptions options = parse("--scan-buffer", "1MB");
        AppConfig.FileScannerConfig merged =
                options.merge(AppConfig.FileScannerConfig.builder().build());
        assertEquals(
                1024 * 1024,
                merged.getDefaultBufferSize().bytes(),
                "--scan-buffer 1MB must merge as exactly 1048576 bytes");
    }

    @Test
    void scanMaxBufferOverIntegerMaxValueFailsValidation() {
        Holder holder = new Holder();
        CommandLine commandLine = newCommandLine(holder);
        assertThrows(
                CommandLine.ParameterException.class,
                () -> commandLine.parseArgs("--scan-max-buffer", "3GB"),
                "--scan-max-buffer 3GB exceeds Integer.MAX_VALUE bytes and must be rejected");
    }

    // Row 27
    @Test
    void copyBufferMapsToFileCopierBufferSize() {
        PerformanceOptions options = parse("--copy-buffer", "32KB");
        AppConfig.FileCopierConfig merged =
                options.merge(AppConfig.FileCopierConfig.builder().build());
        assertEquals(
                32 * 1024,
                merged.getBufferSize().bytes(),
                "--copy-buffer 32KB must set FileCopierConfig.bufferSize");
    }

    @Test
    void copyRawZipAloneSetsAllowRawZipCopyTrue() {
        PerformanceOptions options = parse("--copy-raw-zip");
        AppConfig.FileCopierConfig merged =
                options.merge(AppConfig.FileCopierConfig.builder().build());
        assertTrue(
                merged.isAllowRawZipCopy(),
                "--copy-raw-zip alone must set FileCopierConfig.allowRawZipCopy true");
    }

    // Issue #23: a persisted allowRawZipCopy=true must survive a merge triggered by an
    // unrelated --copy-* flag that doesn't touch --copy-raw-zip at all. Reproduction: config.yaml
    // has allowRawZipCopy: true, user runs with only --copy-threads 3.
    @Test
    void copyThreadsAloneMustNotResetPersistedAllowRawZipCopyToFalse() {
        PerformanceOptions options = parse("--copy-threads", "3");
        AppConfig.FileCopierConfig original = AppConfig.FileCopierConfig.builder()
                .allowRawZipCopy(true)
                .build();
        AppConfig.FileCopierConfig merged = options.merge(original);
        assertTrue(
                merged.isAllowRawZipCopy(),
                "--copy-threads alone must not reset a persisted allowRawZipCopy=true to false");
    }

    // Matrix row 6 (issue #14 step 3): scan/copy thread wrappers cannot cross-assign at
    // compile time; this end-to-end parse pins the runtime merge stays independent too.
    @Test
    void scanThreadsThreeAndCopyThreadsFourMergeIntoTheirRespectiveConfigs() {
        PerformanceOptions options = parse("--scan-threads", "3", "--copy-threads", "4");
        AppConfig.FileScannerConfig mergedScanner =
                options.merge(AppConfig.FileScannerConfig.builder().build());
        AppConfig.FileCopierConfig mergedCopier =
                options.merge(AppConfig.FileCopierConfig.builder().build());
        assertEquals(3, mergedScanner.getThreads().value(), "--scan-threads 3 must merge into FileScannerConfig.threads");
        assertEquals(4, mergedCopier.getThreads().value(), "--copy-threads 4 must merge into FileCopierConfig.threads");
    }

    // Hostile row H4 (pinning actual behavior)
    @Test
    void negativeScanBufferValueFailsParsing() {
        Holder holder = new Holder();
        CommandLine commandLine = newCommandLine(holder);
        assertThrows(
                CommandLine.ParameterException.class,
                () -> commandLine.parseArgs("--scan-buffer", "-1KB"),
                "--scan-buffer -1KB does not match the byte-size grammar (no sign allowed) "
                        + "and must fail as a ParameterException");
    }
}
