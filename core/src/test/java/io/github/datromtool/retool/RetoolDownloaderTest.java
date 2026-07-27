package io.github.datromtool.retool;

import io.github.datromtool.SerializationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage matrix rows 1-11 for issue #44 step 1's {@link RetoolDownloader}. No test touches the
 * network - every case drives a {@link FakeFetcher} (see {@link RetoolDownloader}'s class
 * Javadoc for why a fake was chosen over a real {@code com.sun.net.httpserver.HttpServer}
 * socket).
 */
class RetoolDownloaderTest {

    private static final URI BASE = URI.create("https://example.test/data");

    @TempDir
    Path cacheDir;

    private FakeFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new FakeFetcher();
        // No cloneListMetadataUrl override unless a test stubs one explicitly (row 9).
        fetcher.stub(uri("config/internal-config.json"), "{}");
    }

    private static URI uri(String path) {
        return URI.create(BASE + "/" + path);
    }

    private RetoolDownloader downloader() {
        return new RetoolDownloader(BASE, cacheDir, fetcher, 4);
    }

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

    private RetoolDownloader.DirectoryResult directoryResult(RetoolDownloader.Result result, String directory) {
        return result.directories().stream()
                .filter(d -> d.directory().equals(directory))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No result for directory " + directory));
    }

    // Row 1: unchanged local file (hash matches) -> NOT re-downloaded.
    @Test
    void unchangedFileIsNotReDownloaded() throws IOException {
        byte[] content = "hello\n".getBytes(StandardCharsets.UTF_8);
        Path localFile = cacheDir.resolve("clonelists").resolve("foo.json");
        Files.createDirectories(localFile.getParent());
        Files.write(localFile, content);

        fetcher.stub(uri("clonelists/hash.json"), hashJson(Map.of("foo.json", sha256Hex(content))));
        fetcher.stub(uri("metadata/hash.json"), hashJson(Map.of()));
        URI fileUri = uri("clonelists/foo.json");

        RetoolDownloader.Result result = downloader().sync();

        assertFalse(fetcher.calls().contains(fileUri), "fetcher must not be called for an unchanged file");
        RetoolDownloader.DirectoryResult dirResult = directoryResult(result, "clonelists");
        assertEquals(1, dirResult.checked());
        assertEquals(0, dirResult.downloaded());
        assertEquals(1, dirResult.skipped());
        assertTrue(dirResult.failedFiles().isEmpty());
    }

    // Row 2: changed local file -> re-downloaded, content replaced.
    @Test
    void changedFileIsReDownloadedAndReplaced() throws IOException {
        Path localFile = cacheDir.resolve("clonelists").resolve("foo.json");
        Files.createDirectories(localFile.getParent());
        Files.write(localFile, "old content\n".getBytes(StandardCharsets.UTF_8));

        byte[] newContent = "new content\n".getBytes(StandardCharsets.UTF_8);
        fetcher.stub(uri("clonelists/hash.json"), hashJson(Map.of("foo.json", sha256Hex(newContent))));
        fetcher.stub(uri("metadata/hash.json"), hashJson(Map.of()));
        fetcher.stub(uri("clonelists/foo.json"), newContent);

        RetoolDownloader.Result result = downloader().sync();

        assertArrayEquals(newContent, Files.readAllBytes(localFile));
        RetoolDownloader.DirectoryResult dirResult = directoryResult(result, "clonelists");
        assertEquals(1, dirResult.downloaded());
        assertEquals(0, dirResult.skipped());
    }

    // Row 3: absent local file -> downloaded.
    @Test
    void absentFileIsDownloaded() throws IOException {
        byte[] content = "brand new\n".getBytes(StandardCharsets.UTF_8);
        fetcher.stub(uri("clonelists/hash.json"), hashJson(Map.of("foo.json", sha256Hex(content))));
        fetcher.stub(uri("metadata/hash.json"), hashJson(Map.of()));
        fetcher.stub(uri("clonelists/foo.json"), content);

        RetoolDownloader.Result result = downloader().sync();

        Path localFile = cacheDir.resolve("clonelists").resolve("foo.json");
        assertTrue(Files.isRegularFile(localFile));
        assertArrayEquals(content, Files.readAllBytes(localFile));
        assertEquals(1, directoryResult(result, "clonelists").downloaded());
    }

    // Row 4: a local file with CRLF line endings whose LF-normalized bytes hash to the published
    // value is treated as UNCHANGED - the subtle upstream porting detail this class must pin.
    @Test
    void crlfLocalFileMatchingLfNormalizedHashIsTreatedAsUnchanged() throws IOException {
        byte[] crlfContent = "line1\r\nline2\r\n".getBytes(StandardCharsets.UTF_8);
        byte[] lfNormalized = "line1\nline2\n".getBytes(StandardCharsets.UTF_8);
        Path localFile = cacheDir.resolve("clonelists").resolve("foo.json");
        Files.createDirectories(localFile.getParent());
        Files.write(localFile, crlfContent);

        fetcher.stub(uri("clonelists/hash.json"), hashJson(Map.of("foo.json", sha256Hex(lfNormalized))));
        fetcher.stub(uri("metadata/hash.json"), hashJson(Map.of()));
        URI fileUri = uri("clonelists/foo.json");

        RetoolDownloader.Result result = downloader().sync();

        assertFalse(fetcher.calls().contains(fileUri), "CRLF file hashing to the LF-normalized value must be treated as unchanged");
        assertEquals(1, directoryResult(result, "clonelists").skipped());
        assertArrayEquals(crlfContent, Files.readAllBytes(localFile), "the local file itself must never be rewritten");
    }

    // Row 5: a filename with spaces and parentheses produces the exact expected percent-encoded
    // URI - %20/%28/%29, no '+' (ruling out java.net.URLEncoder).
    @Test
    void fileNameWithSpacesAndParensIsPercentEncodedPerRfc3986() throws IOException {
        String fileName = "Sony - PlayStation (Redump).json";
        String expectedEncodedSegment = "Sony%20-%20PlayStation%20%28Redump%29.json";
        byte[] content = "{}".getBytes(StandardCharsets.UTF_8);

        fetcher.stub(uri("clonelists/hash.json"), hashJson(Map.of(fileName, sha256Hex(content))));
        fetcher.stub(uri("metadata/hash.json"), hashJson(Map.of()));
        URI expectedUri = uri("clonelists/" + expectedEncodedSegment);
        fetcher.stub(expectedUri, content);

        RetoolDownloader.Result result = downloader().sync();

        assertTrue(fetcher.calls().contains(expectedUri), "expected exactly: " + expectedUri);
        assertEquals(1, directoryResult(result, "clonelists").downloaded());
        assertArrayEquals(content, Files.readAllBytes(cacheDir.resolve("clonelists").resolve(fileName)));
    }

    // Row 6: 404 on one file -> recorded failed, other files still downloaded, sync returns a
    // result marking the failure.
    @Test
    void notFoundOnOneFileIsRecordedFailedWhileOthersStillDownload() throws IOException {
        byte[] goodContent = "good\n".getBytes(StandardCharsets.UTF_8);
        fetcher.stub(uri("clonelists/hash.json"), hashJson(Map.of(
                "good.json", sha256Hex(goodContent),
                "missing.json", sha256Hex("irrelevant".getBytes(StandardCharsets.UTF_8)))));
        fetcher.stub(uri("metadata/hash.json"), hashJson(Map.of()));
        fetcher.stub(uri("clonelists/good.json"), goodContent);
        fetcher.notFound(uri("clonelists/missing.json"));

        RetoolDownloader.Result result = downloader().sync();

        RetoolDownloader.DirectoryResult dirResult = directoryResult(result, "clonelists");
        assertEquals(2, dirResult.checked());
        assertEquals(1, dirResult.downloaded());
        assertEquals(List.of("missing.json"), dirResult.failedFiles());
        assertArrayEquals(goodContent, Files.readAllBytes(cacheDir.resolve("clonelists").resolve("good.json")));
        assertFalse(Files.exists(cacheDir.resolve("clonelists").resolve("missing.json")));
        assertTrue(result.hasFailures());
    }

    // Row 7: missing/invalid hash.json for one directory -> that directory skipped, the other
    // still processed.
    @Test
    void invalidHashJsonSkipsOnlyThatDirectory() throws IOException {
        fetcher.stub(uri("clonelists/hash.json"), "not valid json".getBytes(StandardCharsets.UTF_8));
        byte[] metaContent = "meta\n".getBytes(StandardCharsets.UTF_8);
        fetcher.stub(uri("metadata/hash.json"), hashJson(Map.of("bar.json", sha256Hex(metaContent))));
        fetcher.stub(uri("metadata/bar.json"), metaContent);

        RetoolDownloader.Result result = downloader().sync();

        RetoolDownloader.DirectoryResult clonelistsResult = directoryResult(result, "clonelists");
        assertTrue(clonelistsResult.directorySkipped());
        assertTrue(clonelistsResult.skipReason() != null && !clonelistsResult.skipReason().isBlank());
        RetoolDownloader.DirectoryResult metadataResult = directoryResult(result, "metadata");
        assertFalse(metadataResult.directorySkipped());
        assertEquals(1, metadataResult.downloaded());
        assertArrayEquals(metaContent, Files.readAllBytes(cacheDir.resolve("metadata").resolve("bar.json")));
    }

    // Row 8: non-HTTPS base URL -> rejected with a clear error.
    @Test
    void nonHttpsBaseUrlIsRejected() {
        URI httpBase = URI.create("http://example.test/data");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new RetoolDownloader(httpBase, cacheDir, fetcher, 4));
        assertTrue(e.getMessage().contains("HTTPS"));
    }

    // Row 9: cloneListMetadataUrl override in internal-config.json is honored.
    @Test
    void cloneListMetadataUrlOverrideIsHonored() throws IOException {
        URI overrideBase = URI.create("https://override.test/other");
        fetcher.stub(uri("config/internal-config.json"),
                hashJson(Map.of("cloneListMetadataUrl", overrideBase.toString())));

        URI overrideClonelistsHash = URI.create(overrideBase + "/clonelists/hash.json");
        URI overrideMetadataHash = URI.create(overrideBase + "/metadata/hash.json");
        fetcher.stub(overrideClonelistsHash, hashJson(Map.of()));
        fetcher.stub(overrideMetadataHash, hashJson(Map.of()));
        // Deliberately not stubbing the original BASE's per-directory hash.json endpoints: if the
        // override were ignored, those fetches would fail and the directories would come back
        // skipped, which the assertions below would catch.

        RetoolDownloader.Result result = downloader().sync();

        assertTrue(fetcher.calls().contains(overrideClonelistsHash));
        assertTrue(fetcher.calls().contains(overrideMetadataHash));
        assertFalse(directoryResult(result, "clonelists").directorySkipped());
        assertFalse(directoryResult(result, "metadata").directorySkipped());
    }

    // Row 10: atomic write - a fetch that throws mid-directory leaves no partial file, and does
    // not affect the other file in the same directory.
    @Test
    void midDirectoryFetchFailureLeavesNoPartialFile() throws IOException {
        byte[] goodContent = "ok\n".getBytes(StandardCharsets.UTF_8);
        fetcher.stub(uri("clonelists/hash.json"), hashJson(Map.of(
                "a.json", sha256Hex(goodContent),
                "b.json", sha256Hex("whatever".getBytes(StandardCharsets.UTF_8)))));
        fetcher.stub(uri("metadata/hash.json"), hashJson(Map.of()));
        fetcher.stub(uri("clonelists/a.json"), goodContent);
        fetcher.failWithTransportError(uri("clonelists/b.json"));

        RetoolDownloader.Result result = downloader().sync();

        Path dirPath = cacheDir.resolve("clonelists");
        assertArrayEquals(goodContent, Files.readAllBytes(dirPath.resolve("a.json")));
        assertFalse(Files.exists(dirPath.resolve("b.json")));
        try (Stream<Path> entries = Files.list(dirPath)) {
            assertTrue(entries.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")),
                    "no orphaned temp file must remain after a failed download");
        }
        assertEquals(List.of("b.json"), directoryResult(result, "clonelists").failedFiles());
    }

    // Row 11: the result object reports accurate checked/downloaded/skipped/failed counts across
    // a mixed directory (one unchanged, one absent, one 404).
    @Test
    void resultReportsAccurateCounts() throws IOException {
        byte[] unchangedContent = "same\n".getBytes(StandardCharsets.UTF_8);
        Path unchangedFile = cacheDir.resolve("clonelists").resolve("unchanged.json");
        Files.createDirectories(unchangedFile.getParent());
        Files.write(unchangedFile, unchangedContent);

        byte[] newContent = "new\n".getBytes(StandardCharsets.UTF_8);
        fetcher.stub(uri("clonelists/hash.json"), hashJson(Map.of(
                "unchanged.json", sha256Hex(unchangedContent),
                "absent.json", sha256Hex(newContent),
                "missing.json", sha256Hex("x".getBytes(StandardCharsets.UTF_8)))));
        fetcher.stub(uri("metadata/hash.json"), hashJson(Map.of()));
        fetcher.stub(uri("clonelists/absent.json"), newContent);
        fetcher.notFound(uri("clonelists/missing.json"));

        RetoolDownloader.Result result = downloader().sync();

        RetoolDownloader.DirectoryResult dirResult = directoryResult(result, "clonelists");
        assertEquals(3, dirResult.checked());
        assertEquals(1, dirResult.downloaded());
        assertEquals(1, dirResult.skipped());
        assertEquals(1, dirResult.failedFiles().size());
        assertEquals(dirResult.checked(), dirResult.downloaded() + dirResult.skipped() + dirResult.failedFiles().size());
    }

    // A1 (issue #44 step 2 gate finding): a non-HTTPS cloneListMetadataUrl override must be
    // ignored - the sync keeps using the originally configured HTTPS base rather than following
    // (or merely failing on) the insecure override. Code was already correct by inspection; this
    // closes the coverage gap.
    @Test
    void nonHttpsCloneListMetadataUrlOverrideIsIgnored() throws IOException {
        fetcher.stub(uri("config/internal-config.json"),
                hashJson(Map.of("cloneListMetadataUrl", "http://insecure.test/other")));
        fetcher.stub(uri("clonelists/hash.json"), hashJson(Map.of()));
        fetcher.stub(uri("metadata/hash.json"), hashJson(Map.of()));

        RetoolDownloader.Result result = downloader().sync();

        assertTrue(fetcher.calls().contains(uri("clonelists/hash.json")),
                "the original HTTPS base's hash.json must still be used when the override is rejected");
        assertTrue(fetcher.calls().contains(uri("metadata/hash.json")),
                "the original HTTPS base's hash.json must still be used when the override is rejected");
        assertFalse(directoryResult(result, "clonelists").directorySkipped());
        assertFalse(directoryResult(result, "metadata").directorySkipped());
    }

    // A2 (issue #44 step 2 gate finding): row 10 above only fails the FETCH, so atomicWrite's own
    // write-stage failure/cleanup path (tmp file created and written successfully, then the
    // Files.move itself fails) was never exercised - mutating that code away would not fail any
    // existing test. Pinned here for real: the destination is pre-created as a non-empty
    // directory, so a regular-file source can never atomically replace it via Files.move with
    // ATOMIC_MOVE, on any of this project's CI platforms (POSIX rename()/Windows MoveFileEx both
    // refuse to replace a directory with a file, regardless of emptiness) - forcing the actual
    // catch-and-delete-the-tmp-file path to run after a successful fetch and a successful
    // tmp-file write.
    @Test
    void atomicWriteCleansUpTempFileWhenTheMoveItselfFails() throws IOException {
        byte[] content = "new content\n".getBytes(StandardCharsets.UTF_8);
        Path localFile = cacheDir.resolve("clonelists").resolve("foo.json");
        Files.createDirectories(localFile); // "foo.json" pre-exists as a DIRECTORY, not a file
        Files.writeString(localFile.resolve("dummy.txt"), "not empty");

        fetcher.stub(uri("clonelists/hash.json"), hashJson(Map.of("foo.json", sha256Hex(content))));
        fetcher.stub(uri("metadata/hash.json"), hashJson(Map.of()));
        fetcher.stub(uri("clonelists/foo.json"), content);

        RetoolDownloader.Result result = downloader().sync();

        assertTrue(Files.isDirectory(localFile),
                "the pre-existing directory must be untouched since the move itself must fail");
        assertTrue(Files.isRegularFile(localFile.resolve("dummy.txt")),
                "directory contents must be untouched");
        try (Stream<Path> entries = Files.list(cacheDir.resolve("clonelists"))) {
            assertTrue(entries.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")),
                    "atomicWrite's catch block must delete the temp file when the move itself fails");
        }
        assertEquals(List.of("foo.json"), directoryResult(result, "clonelists").failedFiles(),
                "a move failure must be recorded as a failed file, not a silent success");
    }

    // A3 (issue #44 step 2 gate finding): HttpFetcher previously read a response body fully via
    // BodyHandlers.ofByteArray() before this class got any chance to check its size. readBounded
    // is the extracted, pure enforcement logic (parameterized on maxBytes rather than hardcoding
    // HttpFetcher's real 32MB cap) so it is unit-testable with a tiny cap and a cheap synthetic
    // stream - no real network/socket, no actual multi-megabyte payload needed.
    @Test
    void readBoundedReturnsExactBytesWhenUnderCap() throws IOException {
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] result = RetoolDownloader.HttpFetcher.readBounded(new ByteArrayInputStream(data), 1024, BASE);
        assertArrayEquals(data, result);
    }

    @Test
    void readBoundedThrowsClearExceptionWhenStreamExceedsCap() {
        // Yields an effectively unbounded number of bytes without needing an actual
        // multi-megabyte backing array - read(byte[], int, int) always fills the whole buffer.
        InputStream unbounded = new InputStream() {
            @Override
            public int read() {
                return 'a';
            }

            @Override
            public int read(byte[] b, int off, int len) {
                Arrays.fill(b, off, off + len, (byte) 'a');
                return len;
            }
        };
        IOException e = assertThrows(IOException.class,
                () -> RetoolDownloader.HttpFetcher.readBounded(unbounded, 10, BASE));
        assertTrue(e.getMessage().contains("10"), "error must name the cap that was exceeded, got: " + e.getMessage());
        assertTrue(e.getMessage().contains(BASE.toString()), "error must name the URI, got: " + e.getMessage());
    }

    /**
     * An in-memory {@link RetoolDownloader.Fetcher} test double, recording every {@link URI} it
     * was asked to fetch and letting each test wire up per-URI responses/404s/transport failures
     * with no real network or socket involved.
     */
    private static final class FakeFetcher implements RetoolDownloader.Fetcher {

        private final Map<URI, byte[]> responses = new LinkedHashMap<>();
        private final Set<URI> notFoundUris = new HashSet<>();
        private final Set<URI> transportFailUris = new HashSet<>();
        private final List<URI> calls = Collections.synchronizedList(new ArrayList<>());

        void stub(URI uri, byte[] body) {
            responses.put(uri, body);
        }

        void stub(URI uri, String body) {
            stub(uri, body.getBytes(StandardCharsets.UTF_8));
        }

        void notFound(URI uri) {
            notFoundUris.add(uri);
        }

        void failWithTransportError(URI uri) {
            transportFailUris.add(uri);
        }

        List<URI> calls() {
            return List.copyOf(calls);
        }

        @Override
        public byte[] fetch(URI uri) throws IOException {
            calls.add(uri);
            if (notFoundUris.contains(uri)) {
                throw new RetoolDownloader.HttpStatusException(uri, 404);
            }
            if (transportFailUris.contains(uri)) {
                throw new IOException("simulated transport failure for " + uri);
            }
            byte[] body = responses.get(uri);
            if (body == null) {
                throw new IOException("No stub registered for " + uri);
            }
            return body;
        }
    }
}
