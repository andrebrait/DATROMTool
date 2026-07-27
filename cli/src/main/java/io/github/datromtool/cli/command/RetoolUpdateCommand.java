package io.github.datromtool.cli.command;

import io.github.datromtool.cli.GitVersionProvider;
import io.github.datromtool.retool.RetoolDownloader;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import static java.lang.String.format;

/**
 * Issue #44 step 2: {@code datrom retool update} - the sole, explicit-opt-in network path that
 * syncs Retool clone lists/metadata into the local cache via {@link RetoolDownloader}. {@code
 * 1g1r} itself never triggers a fetch (see {@link OneGameOneRomCommand}'s cache-fallback wiring) -
 * this command is the only place a network call can originate from, matching upstream retool's
 * own offline-by-default posture (see the issue's design notes).
 *
 * <p>Prints a human-readable, per-directory report to stdout (checked/downloaded/skipped/failed
 * counts, plus failed file names) and exits 0 when nothing failed, 1 when any file or whole
 * directory failed - the same "0 unless something went wrong" convention {@link ScanCommand}
 * already uses for its own report. Progress is kept simple (this can take a while on first run,
 * downloading the full clone list/metadata set): {@link RetoolDownloader} already logs
 * retargeting/skip/failure events at info/warn level, plus a start/finish line this command
 * prints straight to stderr (not stdout, so the report stays machine-parseable) - no jline
 * progress bar, since {@link RetoolDownloader} exposes no per-file listener seam to drive one
 * (unlike {@link io.github.datromtool.io.FileScanner}/{@link io.github.datromtool.io.FileCopier}),
 * and adding one is out of this step's scope.
 */
@CommandLine.Command(
        name = "update",
        description = "Download/update Retool clone lists and metadata into the local cache",
        sortOptions = false,
        abbreviateSynopsis = true,
        versionProvider = GitVersionProvider.class,
        mixinStandardHelpOptions = true)
public final class RetoolUpdateCommand implements Callable<Integer> {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec commandSpec;

    // Literal "(default: ...)" text in the description, not picocli's ${DEFAULT-VALUE}: probed
    // empirically (RetoolUpdateCommandTest) that this codebase's other commands (ScanCommand's
    // --out-mode, OneGameOneRomCommand's --divergence) already spell the default out as literal
    // text rather than relying on ${DEFAULT-VALUE} substitution, which this project's CommandLine
    // setup does not render (no showDefaultValues) - ${DEFAULT-VALUE} was tried first and failed
    // the usage-text test with the placeholder left un-substituted.
    @CommandLine.Option(
            names = "--base-url",
            paramLabel = "URL",
            description = "Base URL of the retool-clonelists-metadata data repository "
                    + "(default: " + RetoolDownloader.DEFAULT_BASE_URL + ")")
    private URI baseUrl = URI.create(RetoolDownloader.DEFAULT_BASE_URL);

    @CommandLine.Option(
            names = "--dir",
            paramLabel = "PATH",
            description = "Local cache directory to sync into, created if absent "
                    + "(default: ~/.DATROMTool/retool)")
    private Path dir = RetoolDownloader.DEFAULT_CACHE_DIR;

    // Package-private, test-only seam (no CommandLine.Option - never settable from the command
    // line): production always uses RetoolDownloader's real, network-backed HttpFetcher (the
    // 2-arg constructor); tests substitute a Fetcher fake here instead, exactly like
    // RetoolDownloaderTest's own FakeFetcher, so no test ever touches the network. Mirrors
    // OneGameOneRomCommand's retoolCacheDir / ScanCommand.PROGRESS_OUTPUT's test-only-visibility
    // pattern.
    @Nullable
    RetoolDownloader.Fetcher fetcher;

    @Override
    public Integer call() {
        // Review round, finding 7: RetoolDownloader's constructor rejects a non-HTTPS baseUrl
        // with an IllegalArgumentException - fine for a programming error, but a raw stack trace
        // is the wrong surface for a user-supplied --base-url. Validated here, before
        // buildDownloader() ever constructs one, so this is a clean picocli ParameterException
        // (exit code 2) like the sibling --dir/cache-dir-creation failure just below.
        if (!"https".equalsIgnoreCase(baseUrl.getScheme())) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    format("--base-url must use HTTPS, got: '%s'", baseUrl));
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    format("Could not create cache directory '%s': %s", dir, e.getMessage()));
        }
        System.err.printf("Syncing Retool clone lists/metadata from '%s' into '%s'...%n", baseUrl, dir);
        RetoolDownloader.Result result = buildDownloader().sync();
        // Review round, finding 8: internal-config.json can retarget the sync to a different
        // host (RetoolDownloader#resolveEffectiveBase) - the line above only ever named the
        // *configured* base, so a retarget was invisible to the user even though a different
        // host entirely was actually used. Surfaced here, after the sync, since the effective
        // base is only known once RetoolDownloader has resolved it.
        if (!result.effectiveBaseUrl().equals(baseUrl)) {
            System.err.printf("Retargeted to '%s' per upstream internal-config.json%n", result.effectiveBaseUrl());
        }
        System.err.println("Retool sync finished.");
        printReport(result);
        return result.hasFailures() ? 1 : 0;
    }

    RetoolDownloader buildDownloader() {
        return fetcher != null
                ? new RetoolDownloader(baseUrl, dir, fetcher, RetoolDownloader.DEFAULT_CONCURRENCY)
                : new RetoolDownloader(baseUrl, dir);
    }

    private static void printReport(RetoolDownloader.Result result) {
        for (RetoolDownloader.DirectoryResult directoryResult : result.directories()) {
            if (directoryResult.directorySkipped()) {
                System.out.printf(
                        "%s: skipped entire directory (%s)%n",
                        directoryResult.directory(),
                        directoryResult.skipReason());
                continue;
            }
            System.out.printf(
                    "%s: checked=%d downloaded=%d skipped=%d failed=%d%n",
                    directoryResult.directory(),
                    directoryResult.checked(),
                    directoryResult.downloaded(),
                    directoryResult.skipped(),
                    directoryResult.failedFiles().size());
            if (!directoryResult.failedFiles().isEmpty()) {
                System.out.printf("  failed files: %s%n", String.join(", ", directoryResult.failedFiles()));
            }
        }
    }

    // Package-private test-only accessors (no lombok - this class isn't @Data): let tests pin
    // --base-url/--dir parsing without duplicating picocli's own parsing logic.
    URI getBaseUrl() {
        return baseUrl;
    }

    Path getDir() {
        return dir;
    }
}
