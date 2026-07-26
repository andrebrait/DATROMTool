package io.github.datromtool.cli.command;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.cli.GitVersionProvider;
import io.github.datromtool.cli.converter.ExistingDirectoryConverter;
import io.github.datromtool.cli.converter.OutputModeConverter;
import io.github.datromtool.cli.option.PerformanceOptions;
import io.github.datromtool.cli.progressbar.CommandLineProgressBar;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.OutputMode;
import io.github.datromtool.io.FileScanner;
import io.github.datromtool.io.logging.FileScannerLoggingListener;
import lombok.extern.slf4j.Slf4j;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import tools.jackson.core.JacksonException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static java.lang.String.format;

/**
 * Issue #17: expose {@link FileScanner} standalone, without a DAT and without the 1G1R
 * filter/sort/copy pipeline. The report schema is exactly {@link FileScanner.Result} (via
 * {@link SerializationHelper}'s existing XML/JSON/YAML writers) — no bespoke report DTO lives
 * here, since {@link FileScanner.Result} already serializes cleanly with every mapper (probed
 * empirically: {@code archiveType}/{@code path}/{@code digest} round-trip on all three formats
 * with no core annotations added). The scan results are copied into a plain {@link ArrayList}
 * before serialization (not the {@link ImmutableList} {@link FileScanner#scan} returns) purely
 * for a stable XML root tag: Jackson's {@code XmlMapper} derives the root element name from the
 * concrete class when a bare {@code Object} is handed to {@code writeValueAsString}, and Guava's
 * {@code ImmutableList} concrete type is the package-private {@code RegularImmutableList} —
 * valid but leaky. {@code ArrayList} yields {@code <ArrayList>}, which is stable across Guava
 * versions.
 *
 * <p>No DAT is loaded, so {@link FileScanner} is constructed with empty datafile/detector
 * collections — its constructor already treats an empty datafile collection as "no header
 * detection" ({@link FileScanner#FileScanner} falls back to
 * {@code FileScannerParameters.withDefaults()} whenever {@code datafiles.isEmpty()}), which is
 * exactly the "no DAT" case this command needs.
 *
 * <p>The progress bar is wired exactly like {@link OneGameOneRomCommand}, except the backing
 * {@link Terminal} is always built with
 * {@link TerminalBuilder.SystemOutput#ForcedSysErr}. Probed empirically: a jline
 * {@link Terminal}'s {@code writer()} otherwise writes ANSI progress straight to stdout (even
 * under a non-interactive/dumb terminal), which would corrupt a report written to stdout.
 * Forcing stderr unconditionally (rather than only when {@code --out-file} is absent) is simpler
 * than threading that condition through the listener wiring and is strictly safer: it also keeps
 * progress out of a piped stdout even when {@code --out-file} is set.
 */
@Slf4j
@CommandLine.Command(
        name = "scan",
        description = "Scan directories/archives and produce a standalone hash report "
                + "(no DAT, no 1G1R pipeline)",
        sortOptions = false,
        abbreviateSynopsis = true,
        versionProvider = GitVersionProvider.class,
        mixinStandardHelpOptions = true)
public final class ScanCommand implements Callable<Integer> {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec commandSpec;

    @CommandLine.Parameters(
            description = "Directories to scan for ROMs/archives",
            arity = "1..*",
            paramLabel = "DIR",
            converter = ExistingDirectoryConverter.class)
    private List<Path> dirs;

    @CommandLine.Option(
            names = "--out-mode",
            paramLabel = "MODE",
            converter = OutputModeConverter.class,
            completionCandidates = OutputModeConverter.class,
            description = "Report format. Options: ${COMPLETION-CANDIDATES} (default: yaml, for "
                    + "readable stdout output)")
    private OutputMode outMode = OutputMode.YAML;

    @CommandLine.Option(
            names = "--out-file",
            paramLabel = "PATH",
            description = "Write the report to this file (default: print to stdout)")
    private Path outFile;

    @CommandLine.ArgGroup(heading = "Performance options\n", exclusive = false)
    private PerformanceOptions performanceOptions;

    @Override
    public Integer call() {
        AppConfig.FileScannerConfig scannerConfig = resolveScannerConfig();
        FileScannerLoggingListener loggingListener = new FileScannerLoggingListener();
        ImmutableList<FileScanner.Result> scanResults = ImmutableList.of();
        try (Terminal terminal = createTerminal()) {
            List<FileScanner.Listener> listeners = ImmutableList.of(
                    loggingListener,
                    new CommandLineProgressBar(terminal, "Scanning", "Scanning input directories..."));
            FileScanner fileScanner = new FileScanner(
                    scannerConfig,
                    ImmutableList.of(),
                    ImmutableList.of(),
                    listeners);
            scanResults = fileScanner.scan(dirs);
        } catch (IOException e) {
            log.error("Error while closing the terminal", e);
        }

        List<FileScanner.Result> report = new ArrayList<>(scanResults);
        List<String> lines;
        try {
            lines = switch (outMode) {
                case XML -> SerializationHelper.getInstance().writeAsXml(report);
                case JSON -> SerializationHelper.getInstance().writeAsJson(report);
                case YAML -> SerializationHelper.getInstance().writeAsYaml(report);
            };
        } catch (JacksonException e) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    format("Could not write scan report as %s: %s", outMode, e.getMessage()));
        }

        if (outFile != null) {
            try {
                Files.write(outFile, lines);
            } catch (IOException e) {
                throw new CommandLine.ParameterException(
                        commandSpec.commandLine(),
                        format("Could not write output file '%s': %s", outFile, e.getMessage()));
            }
        } else {
            lines.forEach(System.out::println);
        }
        return loggingListener.isErrors() ? 1 : 0;
    }

    AppConfig.FileScannerConfig resolveScannerConfig() {
        AppConfig baseAppConfig = SerializationHelper.getInstance().loadAppConfig();
        return performanceOptions != null
                ? performanceOptions.merge(baseAppConfig.getScanner())
                : baseAppConfig.getScanner();
    }

    @Nullable
    private Terminal createTerminal() {
        try {
            return TerminalBuilder.builder()
                    .systemOutput(TerminalBuilder.SystemOutput.ForcedSysErr)
                    .build();
        } catch (IOException e) {
            log.error("Error while creating terminal", e);
            return null;
        }
    }
}
