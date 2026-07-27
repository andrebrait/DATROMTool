package io.github.datromtool.cli.command;

import io.github.datromtool.SerializationHelper;
import io.github.datromtool.retool.RetoolDownloader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage matrix for issue #44 step 2's {@code retool update} subcommand. {@link
 * RetoolUpdateCommand#buildDownloader()} is the injectable seam this suite uses to reach
 * {@link RetoolDownloader} with an in-test {@link RetoolDownloader.Fetcher} fake (via the
 * package-private {@code fetcher} field) instead of the real network - see that field's Javadoc.
 * No test in this class ever touches the network.
 */
class RetoolUpdateCommandTest {

    private static final URI BASE = URI.create("https://example.test/data");

    private static byte[] hashJson(Map<String, String> hashes) {
        return SerializationHelper.getInstance().getJsonMapper().writeValueAsBytes(hashes);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static URI uri(String path) {
        return URI.create(BASE + "/" + path);
    }

    private static RetoolUpdateCommand newCommand(Path dir, RetoolDownloader.Fetcher fetcher) {
        RetoolUpdateCommand command = new RetoolUpdateCommand();
        command.fetcher = fetcher;
        CommandLine commandLine = new CommandLine(command);
        commandLine.parseArgs("--base-url", BASE.toString(), "--dir", dir.toString());
        return command;
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

    // --- Row 4: --dir/--base-url defaults and overrides parse correctly ---

    @Test
    void defaultsAreRetoolDownloaderDefaults() {
        RetoolUpdateCommand command = new RetoolUpdateCommand();
        new CommandLine(command).parseArgs();
        assertEquals(URI.create(RetoolDownloader.DEFAULT_BASE_URL), command.getBaseUrl(),
                "--base-url default must be RetoolDownloader.DEFAULT_BASE_URL");
        assertEquals(RetoolDownloader.DEFAULT_CACHE_DIR, command.getDir(),
                "--dir default must be RetoolDownloader.DEFAULT_CACHE_DIR (~/.DATROMTool/retool)");
    }

    @Test
    void usageHelpShowsResolvedDefaults() {
        // Probe (see RetoolUpdateCommand's field Javadoc): picocli's ${DEFAULT-VALUE}
        // interpolation left the placeholder un-substituted for this project's CommandLine setup
        // (no showDefaultValues configured) - tried first and failed this exact assertion, which
        // is why the descriptions spell the default out as literal text instead. Whitespace is
        // stripped before comparing (a second probe: picocli's usage-text word wrapping hard-
        // breaks the long base URL mid-word, with no original space at the break point, so a
        // plain contains() check on the raw usage text fails too).
        String usage = new CommandLine(new RetoolUpdateCommand()).getUsageMessage();
        String usageNoWhitespace = usage.replaceAll("\\s+", "");
        assertTrue(usageNoWhitespace.contains(RetoolDownloader.DEFAULT_BASE_URL),
                "usage help must show the --base-url default, got:\n" + usage);
        assertTrue(usageNoWhitespace.contains(".DATROMTool/retool"),
                "usage help must show the --dir default, got:\n" + usage);
    }

    @Test
    void baseUrlAndDirOverridesParseCorrectly(@TempDir Path tempDir) {
        Path customDir = tempDir.resolve("custom-cache");
        URI customBaseUrl = URI.create("https://custom.example/data");
        RetoolUpdateCommand command = new RetoolUpdateCommand();
        new CommandLine(command).parseArgs("--base-url", customBaseUrl.toString(), "--dir", customDir.toString());
        assertEquals(customBaseUrl, command.getBaseUrl());
        assertEquals(customDir, command.getDir());
    }

    // --- Row 2/3: end-to-end against the in-test Fetcher fake; report content; exit codes ---

    @Test
    void endToEndSyncPrintsCountsAndExitsZeroWhenNothingFailed(@TempDir Path tempDir) throws IOException {
        FakeFetcher fetcher = new FakeFetcher();
        fetcher.stub(uri("config/internal-config.json"), "{}");
        byte[] content = "hello\n".getBytes(StandardCharsets.UTF_8);
        fetcher.stub(uri("clonelists/hash.json"), hashJson(Map.of("foo.json", sha256Hex(content))));
        fetcher.stub(uri("clonelists/foo.json"), content);
        fetcher.stub(uri("metadata/hash.json"), hashJson(Map.of()));

        RetoolUpdateCommand command = newCommand(tempDir, fetcher);
        int[] exitCode = new int[1];
        String stdout = captureStdout(() -> exitCode[0] = command.call());

        assertEquals(0, exitCode[0], "nothing failed, must exit 0");
        assertTrue(stdout.contains("clonelists: checked=1 downloaded=1 skipped=0 failed=0"),
                "report must contain clonelists counts, got:\n" + stdout);
        assertTrue(stdout.contains("metadata: checked=0 downloaded=0 skipped=0 failed=0"),
                "report must contain metadata counts, got:\n" + stdout);
        assertTrue(Files.isRegularFile(tempDir.resolve("clonelists").resolve("foo.json")),
                "the synced file must actually exist on disk");
    }

    @Test
    void failedFileNamesAppearInReportAndExitCodeIsOne(@TempDir Path tempDir) throws IOException {
        FakeFetcher fetcher = new FakeFetcher();
        fetcher.stub(uri("config/internal-config.json"), "{}");
        fetcher.stub(uri("clonelists/hash.json"),
                hashJson(Map.of("missing.json", sha256Hex("irrelevant".getBytes(StandardCharsets.UTF_8)))));
        fetcher.notFound(uri("clonelists/missing.json"));
        fetcher.stub(uri("metadata/hash.json"), hashJson(Map.of()));

        RetoolUpdateCommand command = newCommand(tempDir, fetcher);
        int[] exitCode = new int[1];
        String stdout = captureStdout(() -> exitCode[0] = command.call());

        assertEquals(1, exitCode[0], "a failed file must exit 1");
        assertTrue(stdout.contains("failed files: missing.json"),
                "report must name the failed file, got:\n" + stdout);
    }

    @Test
    void dirIsCreatedWhenAbsent(@TempDir Path tempDir) throws IOException {
        Path notYetCreated = tempDir.resolve("does-not-exist-yet").resolve("retool");
        FakeFetcher fetcher = new FakeFetcher();
        fetcher.stub(uri("config/internal-config.json"), "{}");
        fetcher.stub(uri("clonelists/hash.json"), hashJson(Map.of()));
        fetcher.stub(uri("metadata/hash.json"), hashJson(Map.of()));

        RetoolUpdateCommand command = newCommand(notYetCreated, fetcher);
        captureStdout(command::call);

        assertTrue(Files.isDirectory(notYetCreated), "--dir must be created if it did not already exist");
    }

    // --- CodeRabbit review round (PR #45), finding 7: a non-HTTPS --base-url must surface as a
    // clean picocli ParameterException (exit code 2, like the sibling --dir/cache-dir-creation
    // failure just above), not an IllegalArgumentException raw stack trace escaping from
    // RetoolDownloader's constructor. ---

    @Test
    void nonHttpsBaseUrlSurfacesAsParameterExceptionExitCode2(@TempDir Path tempDir) {
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(capturedErr));
        int exitCode;
        try {
            exitCode = new CommandLine(new RetoolUpdateCommand())
                    .execute("--base-url", "http://insecure.example/data", "--dir", tempDir.toString());
        } finally {
            System.setErr(originalErr);
        }
        assertEquals(2, exitCode, "a non-HTTPS --base-url must exit 2, stderr was:\n" + capturedErr);
        assertTrue(capturedErr.toString().contains("HTTPS"),
                "error must name HTTPS as the requirement, got:\n" + capturedErr);
    }

    // --- CodeRabbit review round, finding 8: internal-config.json's cloneListMetadataUrl can
    // retarget the sync to a different host; printing only the originally-configured base (as
    // this command did before this fix) means the user never sees which host was actually used. ---

    @Test
    void retargetedBaseUrlIsSurfacedOnStderr(@TempDir Path tempDir) throws IOException {
        URI overrideBase = URI.create("https://override.example/other");
        FakeFetcher fetcher = new FakeFetcher();
        fetcher.stub(uri("config/internal-config.json"), hashJson(Map.of("cloneListMetadataUrl", overrideBase.toString())));
        fetcher.stub(URI.create(overrideBase + "/clonelists/hash.json"), hashJson(Map.of()));
        fetcher.stub(URI.create(overrideBase + "/metadata/hash.json"), hashJson(Map.of()));

        RetoolUpdateCommand command = newCommand(tempDir, fetcher);
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(capturedErr));
        try {
            captureStdout(command::call);
        } finally {
            System.setErr(originalErr);
        }

        assertTrue(capturedErr.toString().contains(overrideBase.toString()),
                "stderr must surface the actually-used (retargeted) base URL, got:\n" + capturedErr);
    }

    /**
     * Same in-memory {@link RetoolDownloader.Fetcher} test double shape as {@code
     * RetoolDownloaderTest}'s own {@code FakeFetcher} (core module) - duplicated here rather than
     * shared across modules since it is a handful of lines and {@code cli} has no existing
     * test-fixture dependency on {@code core}'s test-jar.
     */
    private static final class FakeFetcher implements RetoolDownloader.Fetcher {

        private final Map<URI, byte[]> responses = new LinkedHashMap<>();
        private final Set<URI> notFoundUris = new HashSet<>();

        void stub(URI uri, byte[] body) {
            responses.put(uri, body);
        }

        void stub(URI uri, String body) {
            stub(uri, body.getBytes(StandardCharsets.UTF_8));
        }

        void notFound(URI uri) {
            notFoundUris.add(uri);
        }

        @Override
        public byte[] fetch(URI uri) throws IOException {
            if (notFoundUris.contains(uri)) {
                throw new RetoolDownloader.HttpStatusException(uri, 404);
            }
            byte[] body = responses.get(uri);
            if (body == null) {
                throw new IOException("No stub registered for " + uri);
            }
            return body;
        }
    }
}
