package io.github.datromtool.retool;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.SerializationHelper;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

/**
 * Syncs Retool {@code clonelists} and {@code metadata} from the {@code retool-clonelists-
 * metadata} data repository (issue #44 step 1) into a local cache directory, downloading only
 * files whose sha256 differs from the published {@code hash.json} manifest - mirroring retool
 * v2.4.9's own update mechanism ({@code modules/clone_lists/update_clone_list_metadata.py}),
 * verified against its source rather than its docs (see the issue for line references).
 *
 * <p><b>Scope (this step):</b> only {@code clonelists} and {@code metadata} are synced, not
 * upstream's {@code mias}/{@code retroachievements} directories - out of scope for this
 * codebase's current Retool integration (issue #19), which only consumes clone lists and
 * metadata. Add them as their own {@link #SYNCED_DIRECTORIES} entries if/when a future issue
 * wires them up; nothing else in this class is directory-specific.
 *
 * <p><b>Concurrency:</b> a bounded {@link ExecutorService} (fixed pool, default {@link
 * #DEFAULT_CONCURRENCY} threads - upstream's own parallel-download count), created fresh per
 * {@link #sync()} call and closed at the end, mirroring {@code FileScanner}/{@code FileCopier}'s
 * per-call executor lifecycle in {@code io}. Unlike those, this closes it via Java 19+'s
 * {@link ExecutorService#close()} (try-with-resources) rather than a manual shutdown dance - a
 * newer, equivalent idiom this codebase's Java 25 baseline allows and no existing caller here
 * needs to match byte-for-byte.
 *
 * <p><b>Testability:</b> all network access goes through the injectable {@link Fetcher} seam
 * (constructor parameter), so tests never touch the network. The default {@link HttpFetcher}
 * exists for real use; tests instead pass an in-test fake (a {@code Map}-backed stub recording
 * every {@link URI} it was asked to fetch) rather than standing up a real {@code
 * com.sun.net.httpserver.HttpServer} socket - a fake gives byte-for-byte control over per-URI
 * responses/404s/transport failures with no ephemeral-port binding, which is one more source of
 * flakiness on this project's cross-OS CI matrix (ubuntu/macos/windows) for no behavioral gain:
 * {@link Fetcher} is a one-method seam, so a real socket would only re-verify the JDK's own
 * {@code HttpClient}, not this class's sync logic.
 */
@Slf4j
public final class RetoolDownloader {

    /**
     * Upstream's hardcoded fallback base URL ({@code constants.py:3-5}), overridable at runtime
     * by the fetched {@code internal-config.json}'s {@code cloneListMetadataUrl} - see
     * {@link #resolveEffectiveBase()}.
     */
    public static final String DEFAULT_BASE_URL =
            "https://raw.githubusercontent.com/unexpectedpanda/retool-clonelists-metadata/main";

    /** Directory names within the cache/upstream repo - also the names {@code cli}'s 1G1R
     * command uses for its cache-fallback directories (issue #44 step 2), kept as one source of
     * truth rather than a second hardcoded copy of these literals. */
    public static final String CLONELISTS_DIRECTORY = "clonelists";
    public static final String METADATA_DIRECTORY = "metadata";

    private static final List<String> SYNCED_DIRECTORIES =
            List.of(CLONELISTS_DIRECTORY, METADATA_DIRECTORY);

    /** {@code ~/.DATROMTool/retool} - beside the existing {@code config.yaml}/
     * {@code region-data.yaml} (issue #44 step 2's default {@code --dir} and 1G1R's cache-fallback
     * base), reusing {@link SerializationHelper#DEFAULT_BASE_PATH} rather than a second hardcoded
     * copy of {@code .DATROMTool}. */
    public static final Path DEFAULT_CACHE_DIR = SerializationHelper.DEFAULT_BASE_PATH.resolve("retool");

    public static final int DEFAULT_CONCURRENCY = 10;

    /** A few bounded attempts on transport failure, per the issue's error policy; a 404 never
     * retries (see {@link #fetchWithRetry}). Upstream retries 5 times with a fixed 5s sleep; a
     * fixed 5s sleep would make the default suite take minutes, so this uses a much shorter
     * delay - the *policy* (bounded retries, fixed delay, then fail) is what's being ported, not
     * upstream's literal timing, which was never a contract callers depend on. */
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(200);

    private final URI baseUrl;
    private final Path cacheDir;
    private final Fetcher fetcher;
    private final int concurrency;
    private final JsonMapper jsonMapper;

    public RetoolDownloader(@Nonnull URI baseUrl, @Nonnull Path cacheDir) {
        this(baseUrl, cacheDir, new HttpFetcher(), DEFAULT_CONCURRENCY);
    }

    public RetoolDownloader(
            @Nonnull URI baseUrl,
            @Nonnull Path cacheDir,
            @Nonnull Fetcher fetcher,
            int concurrency) {
        if (!"https".equalsIgnoreCase(baseUrl.getScheme())) {
            throw new IllegalArgumentException(
                    format("Retool base URL must use HTTPS, got: '%s'", baseUrl));
        }
        if (concurrency <= 0) {
            throw new IllegalArgumentException("concurrency must be positive, got: " + concurrency);
        }
        this.baseUrl = baseUrl;
        this.cacheDir = cacheDir;
        this.fetcher = fetcher;
        this.concurrency = concurrency;
        this.jsonMapper = SerializationHelper.getInstance().getJsonMapper();
    }

    /**
     * Syncs every entry in {@link #SYNCED_DIRECTORIES}, returning a per-directory accounting of
     * what happened. Never throws for a single file/directory failure - those are recorded in the
     * returned {@link Result} (see the issue's error policy) - it only propagates if the sync
     * thread itself is interrupted.
     */
    @Nonnull
    public Result sync() {
        URI effectiveBase = resolveEffectiveBase();
        ThreadFactory threadFactory = new NamedThreadFactory("RETOOL-DOWNLOADER");
        ImmutableList.Builder<DirectoryResult> results = ImmutableList.builder();
        try (ExecutorService executorService = Executors.newFixedThreadPool(concurrency, threadFactory)) {
            for (String directory : SYNCED_DIRECTORIES) {
                results.add(syncDirectory(executorService, effectiveBase, directory));
            }
        }
        return new Result(results.build(), effectiveBase);
    }

    /**
     * Fetches {@code {base}/config/internal-config.json} and honors its {@code
     * cloneListMetadataUrl} key if present, non-blank, different from the configured base, and
     * itself HTTPS (kept simple per the issue: no recursive re-resolution, no further validation
     * beyond scheme). Any failure to fetch/parse the config (missing file, transport error,
     * malformed JSON) is logged and falls back to the originally configured base URL - the
     * config's whole purpose is retargeting an already-working sync, so a failure here must never
     * abort the run.
     */
    private URI resolveEffectiveBase() {
        URI configUri = join(baseUrl, "config", "internal-config.json");
        try {
            byte[] configBytes = fetchWithRetry(configUri);
            Map<String, Object> config = jsonMapper.readValue(configBytes, new TypeReference<Map<String, Object>>() {
            });
            Object override = config.get("cloneListMetadataUrl");
            if (override instanceof String overrideUrl
                    && !overrideUrl.isBlank()
                    && !overrideUrl.equals(baseUrl.toString())) {
                URI overrideUri = URI.create(overrideUrl);
                if ("https".equalsIgnoreCase(overrideUri.getScheme())) {
                    log.info("Retargeting Retool sync base URL per internal-config.json: '{}'", overrideUrl);
                    return overrideUri;
                }
                log.warn("Ignoring non-HTTPS cloneListMetadataUrl override '{}'", overrideUrl);
            }
        } catch (Exception e) {
            log.warn("Could not fetch/parse '{}'; using configured base URL '{}'", configUri, baseUrl, e);
        }
        return baseUrl;
    }

    /**
     * Syncs one directory: fetches its {@code hash.json} manifest (the list of files to sync -
     * upstream never enumerates the directory itself), then dispatches one bounded task per
     * manifest entry. A missing/unparseable {@code hash.json} skips only this directory - other
     * directories still run (per the issue's error policy) - recorded via {@link
     * DirectoryResult#skipped}.
     */
    private DirectoryResult syncDirectory(ExecutorService executorService, URI base, String directory) {
        URI hashUri = join(base, directory, "hash.json");
        Map<String, String> hashes;
        try {
            byte[] hashBytes = fetchWithRetry(hashUri);
            hashes = jsonMapper.readValue(hashBytes, new TypeReference<Map<String, String>>() {
            });
            if (hashes == null) {
                throw new IOException("hash.json parsed to null");
            }
        } catch (Exception e) {
            log.warn("Skipping directory '{}': could not fetch/parse '{}'", directory, hashUri, e);
            return DirectoryResult.skipped(directory, format("Could not fetch/parse hash.json: %s", e.getMessage()));
        }

        Path dirPath = cacheDir.resolve(directory);
        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            log.warn("Skipping directory '{}': could not create cache directory '{}'", directory, dirPath, e);
            return DirectoryResult.skipped(directory, format("Could not create cache directory: %s", e.getMessage()));
        }

        List<Callable<FileOutcome>> tasks = hashes.entrySet().stream()
                .map(entry -> (Callable<FileOutcome>) () ->
                        syncFile(base, directory, dirPath, entry.getKey(), entry.getValue()))
                .toList();

        int downloaded = 0;
        int skipped = 0;
        ImmutableList.Builder<String> failed = ImmutableList.builder();
        try {
            List<Future<FileOutcome>> futures = executorService.invokeAll(tasks);
            for (Future<FileOutcome> future : futures) {
                FileOutcome outcome = future.get();
                switch (outcome.status()) {
                    case DOWNLOADED -> downloaded++;
                    case SKIPPED -> skipped++;
                    case FAILED -> failed.add(outcome.fileName());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(format("Interrupted while syncing directory '%s'", directory), e);
        } catch (ExecutionException e) {
            // syncFile() never lets a checked/unchecked exception escape - every failure path
            // returns FileOutcome.failed(...) - so this can only mean a programming error.
            throw new IllegalStateException(
                    format("Unexpected task failure while syncing directory '%s'", directory), e.getCause());
        }
        return new DirectoryResult(directory, hashes.size(), downloaded, skipped, failed.build(), false, null);
    }

    /**
     * Downloads one manifest entry if needed (missing locally, or present with a mismatching
     * hash - see {@link #needsDownload}), writing it atomically (see {@link #atomicWrite}) so an
     * interrupted run cannot leave a truncated file that later parses as corrupt. Every failure
     * path is caught and converted to {@link FileOutcome#failed} - this method is invoked as an
     * {@link ExecutorService} task and must never let an exception escape, or one bad file would
     * poison {@link ExecutorService#invokeAll} for the whole directory.
     *
     * <p><b>Review round (issue #44 PR #45), finding 1:</b> a successful fetch is no longer
     * trusted blindly - its LF-normalized bytes must hash to {@code expectedHash} (the same
     * comparison {@link #needsDownload} uses) before {@link #atomicWrite} ever runs. A mismatch
     * (truncated/corrupted transfer, or a manifest/file pair that disagree) is a failure, not a
     * silent write of bad data that a later run might not even notice re-downloading, since the
     * corrupted bytes would already be on disk influencing behavior until then.
     */
    private FileOutcome syncFile(URI base, String directory, Path dirPath, String fileName, String expectedHash) {
        if (isUnsafeFileName(fileName)) {
            log.warn("Refusing to sync unsafe file name '{}' from directory '{}' manifest", fileName, directory);
            return FileOutcome.failed(fileName);
        }
        Path localFile;
        try {
            localFile = resolveWithinCache(dirPath, fileName);
        } catch (IOException e) {
            log.warn("Refusing to sync file name '{}' from directory '{}' manifest: {}", fileName, directory, e.getMessage());
            return FileOutcome.failed(fileName);
        }
        try {
            if (!needsDownload(localFile, expectedHash)) {
                return FileOutcome.skipped(fileName);
            }
            URI fileUri = join(base, directory, percentEncodeSegment(fileName));
            byte[] content = fetchWithRetry(fileUri);
            byte[] normalized = normalizeLineEndings(content);
            String actualHash = sha256Hex(normalized);
            if (!actualHash.equalsIgnoreCase(expectedHash)) {
                log.warn(
                        "Refusing to write '{}/{}': downloaded content hash '{}' does not match manifest hash '{}'",
                        directory, fileName, actualHash, expectedHash);
                return FileOutcome.failed(fileName);
            }
            atomicWrite(localFile, content);
            return FileOutcome.downloaded(fileName);
        } catch (Exception e) {
            log.warn("Failed to sync '{}/{}'", directory, fileName, e);
            return FileOutcome.failed(fileName);
        }
    }

    // Manifest entries are file names from the (semi-trusted) configured upstream, not user
    // input - but a compromised/misconfigured source publishing "../../evil" as a key must not
    // be able to write outside the cache directory, so the same bare-name shape check
    // RetoolFileResolver applies to DAT header names applies here too.
    //
    // Review round, finding 3: ':' is also rejected - "C:payload" isn't caught by any of the
    // other shape checks (no '/', no '\\', not ".."/"."), but on Windows Path#resolve treats a
    // leading "X:" as drive-relative, resolving against that drive's current directory and
    // escaping the cache directory entirely.
    private static boolean isUnsafeFileName(String fileName) {
        return fileName.isEmpty()
                || fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains(":")
                || fileName.equals("..")
                || fileName.equals(".");
    }

    /**
     * Resolves {@code fileName} within {@code dirPath}, given the platform cannot even express
     * every string as a path (e.g. a NUL byte - review round, finding 2) and {@link
     * Path#resolve(String)} throws an <em>unchecked</em> {@link InvalidPathException} for those,
     * which - unlike every other failure in {@link #syncFile} - was escaping this method
     * entirely, because the resolve previously ran before any try block. Per {@link
     * #syncDirectory}'s {@link ExecutionException} handling, an escaping unchecked exception here
     * would poison the whole directory sync, not just the one bad manifest entry.
     *
     * <p>Also asserts containment (defense in depth, mirroring {@link
     * RetoolFileResolver#resolveFile}'s own containment assert for the same class of
     * hostile-manifest-entry threat): {@link #isUnsafeFileName} already rejects every traversal
     * shape this class knows of, but a resolved candidate that somehow still escapes {@code
     * dirPath} must never be written to, regardless of which check let it through.
     */
    private static Path resolveWithinCache(Path dirPath, String fileName) throws IOException {
        Path normalizedDir = dirPath.normalize();
        Path candidate;
        try {
            candidate = normalizedDir.resolve(fileName).normalize();
        } catch (InvalidPathException e) {
            throw new IOException(format("'%s' is not a valid path on this platform", fileName), e);
        }
        if (!candidate.startsWith(normalizedDir)) {
            throw new IOException(format(
                    "File name '%s' resolves outside cache directory '%s' - refusing to use it",
                    fileName, dirPath));
        }
        return candidate;
    }

    /**
     * {@code true} if {@code localFile} is absent, or its LF-normalized bytes (see {@link
     * #normalizeLineEndings}) hash to something other than {@code expectedHash} (case-
     * insensitive) - replicating upstream's CRLF-to-LF rewrite-before-hash so hashes computed
     * here match the published manifest for a byte-identical file saved with CRLF line endings.
     * Never rewrites {@code localFile} itself - only the in-memory bytes used for hashing are
     * normalized.
     */
    private boolean needsDownload(Path localFile, String expectedHash) {
        if (!Files.isRegularFile(localFile)) {
            return true;
        }
        try {
            byte[] raw = Files.readAllBytes(localFile);
            byte[] normalized = normalizeLineEndings(raw);
            String actualHash = sha256Hex(normalized);
            return !actualHash.equalsIgnoreCase(expectedHash);
        } catch (IOException e) {
            log.warn("Could not read local file '{}' for hash comparison; will re-download", localFile, e);
            return true;
        }
    }

    private static byte[] normalizeLineEndings(byte[] bytes) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if (b == '\r' && i + 1 < bytes.length && bytes[i + 1] == '\n') {
                continue; // drop the \r, the following \n is written on the next iteration
            }
            out.write(b);
        }
        return out.toByteArray();
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a mandatory JDK algorithm (java.security.MessageDigest javadoc) - this
            // can only mean a broken JDK installation, not a reachable runtime condition.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // Temp file + atomic move within the same directory (same filesystem, so ATOMIC_MOVE is
    // always honored) - an interrupted write leaves only an orphaned .tmp file, never a
    // truncated/corrupt localFile.
    private static void atomicWrite(Path localFile, byte[] content) throws IOException {
        Path tmp = Files.createTempFile(localFile.getParent(), localFile.getFileName().toString(), ".tmp");
        try {
            Files.write(tmp, content);
            Files.move(tmp, localFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
    }

    /**
     * Fetches {@code uri}, retrying transport failures up to {@link #MAX_ATTEMPTS} times with a
     * fixed delay, but never retrying a 404 ({@link HttpStatusException} with {@link
     * HttpStatusException#statusCode()} {@code == 404}) - it means the file was intentionally
     * removed/renamed upstream, not a transient failure, exactly like upstream's own "404 -> warn
     * and skip, no retry" policy.
     */
    private byte[] fetchWithRetry(URI uri) throws IOException {
        IOException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return fetcher.fetch(uri);
            } catch (HttpStatusException e) {
                if (e.statusCode() == 404) {
                    throw e;
                }
                last = e;
            } catch (IOException e) {
                last = e;
            }
            if (attempt < MAX_ATTEMPTS) {
                sleepBeforeRetry();
            }
        }
        throw last;
    }

    private static void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during retry backoff", e);
        }
    }

    private static URI join(URI base, String... segments) {
        StringBuilder sb = new StringBuilder(base.toString());
        if (sb.isEmpty() || sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(segments[i]);
        }
        return URI.create(sb.toString());
    }

    /**
     * Percent-encodes {@code segment} (a file's bare name, i.e. the URI's last path segment) per
     * RFC 3986's unreserved-character set ({@code ALPHA / DIGIT / "-" / "." / "_" / "~"}) -
     * everything else becomes a {@code %XX} UTF-8 byte escape. Deliberately not {@link
     * java.net.URLEncoder}: it is an {@code application/x-www-form-urlencoded} encoder, not an
     * RFC 3986 one - it emits {@code +} for space instead of {@code %20}, which upstream's own
     * {@code urllib.parse.quote}-based encoding (and raw.githubusercontent.com's own URL parsing)
     * does not accept as a space.
     */
    private static String percentEncodeSegment(String segment) {
        byte[] bytes = segment.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length);
        for (byte b : bytes) {
            int c = b & 0xFF;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~') {
                sb.append((char) c);
            } else {
                sb.append('%').append(format("%02X", c));
            }
        }
        return sb.toString();
    }

    /** A single {@code fileName -> outcome} result from {@link #syncFile}. */
    private record FileOutcome(Status status, String fileName) {
        enum Status {DOWNLOADED, SKIPPED, FAILED}

        static FileOutcome downloaded(String fileName) {
            return new FileOutcome(Status.DOWNLOADED, fileName);
        }

        static FileOutcome skipped(String fileName) {
            return new FileOutcome(Status.SKIPPED, fileName);
        }

        static FileOutcome failed(String fileName) {
            return new FileOutcome(Status.FAILED, fileName);
        }
    }

    /**
     * One directory's sync outcome. {@code checked} is the manifest's entry count (0 if the
     * directory was skipped outright); {@code downloaded + skipped + failedFiles.size()} always
     * equals {@code checked} for a processed (non-skipped) directory.
     */
    public record DirectoryResult(
            String directory,
            int checked,
            int downloaded,
            int skipped,
            ImmutableList<String> failedFiles,
            boolean directorySkipped,
            @Nullable String skipReason) {

        static DirectoryResult skipped(String directory, String reason) {
            return new DirectoryResult(directory, 0, 0, 0, ImmutableList.of(), true, reason);
        }
    }

    /**
     * The overall sync outcome - one {@link DirectoryResult} per {@link #SYNCED_DIRECTORIES}
     * entry, plus {@code effectiveBaseUrl}: the base actually used after {@link
     * #resolveEffectiveBase()} - which may differ from the configured {@link #baseUrl} if
     * upstream's {@code internal-config.json} retargeted it (issue #44 review round, finding 8:
     * callers need this to surface a retarget to the user, since printing the configured base
     * alone can silently name the wrong host).
     */
    public record Result(ImmutableList<DirectoryResult> directories, URI effectiveBaseUrl) {

        public boolean hasFailures() {
            return directories.stream()
                    .anyMatch(d -> d.directorySkipped() || !d.failedFiles().isEmpty());
        }
    }

    /** The injectable network seam - see this class's Javadoc for why tests fake this instead of the network. */
    public interface Fetcher {
        byte[] fetch(URI uri) throws IOException;
    }

    /** A non-2xx HTTP response, carrying the status code so callers can special-case 404. */
    public static final class HttpStatusException extends IOException {
        private final int statusCode;

        public HttpStatusException(URI uri, int statusCode) {
            super(format("HTTP %d fetching '%s'", statusCode, uri));
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }

    /**
     * The default, real-network {@link Fetcher}: JDK {@link HttpClient} with explicit connect/
     * read timeouts and an honest User-Agent identifying this tool (unlike upstream, which spoofs
     * a Chrome User-Agent - see the issue's design notes).
     */
    public static final class HttpFetcher implements Fetcher {

        private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
        private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

        /** A3 (issue #44 step 2 gate finding): the largest real upstream file is ~53KB (per the
         * issue), so 32MB is generous headroom while still bounding memory use against a
         * misbehaving/malicious server streaming an unbounded response - {@link
         * HttpResponse.BodyHandlers#ofByteArray} previously buffered a response fully before this
         * class got any chance to check its size. Enforced by {@link #readBounded} while the body
         * is still being streamed, not after the fact. */
        private static final long MAX_RESPONSE_BYTES = 32L * 1024 * 1024;

        private final HttpClient client;
        private final String userAgent;

        public HttpFetcher() {
            this.client = HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            this.userAgent = buildUserAgent();
        }

        private static String buildUserAgent() {
            String version = SerializationHelper.getInstance().getVersionString();
            return format("DATROMTool/%s", version != null ? version : "unknown");
        }

        @Override
        public byte[] fetch(URI uri) throws IOException {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("User-Agent", userAgent)
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            try {
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();
                try (InputStream body = response.body()) {
                    if (status < 200 || status >= 300) {
                        throw new HttpStatusException(uri, status);
                    }
                    return readBounded(body, MAX_RESPONSE_BYTES, uri);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(format("Interrupted while fetching '%s'", uri), e);
            }
        }

        /**
         * Reads {@code in} fully into a byte array, throwing {@link IOException} as soon as more
         * than {@code maxBytes} have been read - never buffering past the cap - rather than
         * reading an unbounded response and only checking its size afterward. Package-visible and
         * parameterized on {@code maxBytes} (rather than hardcoding {@link #MAX_RESPONSE_BYTES}
         * internally) so it is unit-testable with a small cap and a cheap synthetic stream,
         * without needing an actual multi-megabyte payload or any real network/socket.
         */
        static byte[] readBounded(InputStream in, long maxBytes, URI uri) throws IOException {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            long total = 0;
            int read;
            while ((read = in.read(chunk)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IOException(format(
                            "Response body for '%s' exceeded the %d-byte cap", uri, maxBytes));
                }
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }

    // io.github.datromtool.io.IndexedThreadFactory is package-private to io.github.datromtool.io
    // and not reusable here; this is the same daemon-thread-with-logged-uncaught-handler shape,
    // scoped locally instead.
    private static final class NamedThreadFactory implements ThreadFactory {

        private final String namePrefix;
        private final AtomicInteger indexCounter = new AtomicInteger(1);

        private NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            Thread thread = new Thread(r, namePrefix + "-" + indexCounter.getAndIncrement());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((t, e) -> log.error("Unexpected exception thrown", e));
            return thread;
        }
    }
}
