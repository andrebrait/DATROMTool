package io.github.datromtool.cli.option;

import io.github.datromtool.config.AppConfig;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerformanceOptionsTest {

    @CommandLine.Command
    private static final class Holder {

        @CommandLine.ArgGroup(exclusive = false)
        PerformanceOptions performanceOptions = new PerformanceOptions();
    }

    private static PerformanceOptions parse(String... args) {
        Holder holder = new Holder();
        new CommandLine(holder).parseArgs(args);
        return holder.performanceOptions;
    }

    @Test
    void copyThreadsAloneConfiguresTheCopier() {
        PerformanceOptions options = parse("--copy-threads", "3");
        AppConfig.FileCopierConfig merged =
                options.merge(AppConfig.FileCopierConfig.builder().build());
        assertEquals(
                3,
                merged.getThreads(),
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
                mergedScanner.getThreads(),
                "--scan-threads must set the scanner thread count");
        assertEquals(
                3,
                mergedCopier.getThreads(),
                "--copy-threads must set the copier thread count, not the scanner's value");
    }
}
