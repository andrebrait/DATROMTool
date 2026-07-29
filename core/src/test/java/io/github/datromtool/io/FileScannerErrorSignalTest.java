package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.io.logging.FileScannerLoggingListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A scan that could not look at everything it was pointed at must not be reportable as a
 * complete one: the caller (the {@code scan} command's exit code, later the GUI) can only
 * distinguish a complete report from a truncated one if the failure reaches a listener.
 */
class FileScannerErrorSignalTest {

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    private static FileScanner scannerWith(FileScanner.Listener... listeners) {
        return new FileScanner(
                AppConfig.FileScannerConfig.builder().build(),
                ImmutableList.of(),
                ImmutableList.of(),
                ImmutableList.copyOf(listeners));
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

    private static void restorePermissions(Path directory) throws IOException {
        try {
            Files.setPosixFilePermissions(directory, Set.of(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                    java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE));
        } catch (UnsupportedOperationException e) {
            // nothing to restore on a filesystem that never applied them
        }
    }

    @Test
    void givenARootThatCannotBeListed_whenScanning_thenTheFailureIsReported(@TempDir Path tempDir) {
        Path missingRoot = tempDir.resolve("not-there");
        FileScannerLoggingListener listener = new FileScannerLoggingListener();

        ImmutableList<FileScanner.Result> results =
                scannerWith(listener).scan(ImmutableList.of(missingRoot));

        assertTrue(results.isEmpty(), "nothing is scannable under a root that cannot be listed");
        assertTrue(
                listener.isErrors(),
                "a root that could not be listed must not look like an empty, successful scan");
    }

    @Test
    void givenASubdirectoryThatCannotBeListed_whenScanning_thenTheFailureIsReported(
            @TempDir Path tempDir) throws IOException {
        Path unlistable = Files.createDirectory(tempDir.resolve("unlistable"));
        Files.write(unlistable.resolve("rom.bin"), new byte[64 * 1024]);
        assumeTrue(
                makeUnlistable(unlistable),
                "this filesystem/user cannot make a directory unlistable");
        FileScannerLoggingListener listener = new FileScannerLoggingListener();

        try {
            scannerWith(listener).scan(ImmutableList.of(tempDir));

            assertTrue(
                    listener.isErrors(),
                    "a subtree that could not be listed must not be silently skipped");
        } finally {
            restorePermissions(unlistable);
        }
    }

    @Test
    void givenTheCallingThreadIsInterrupted_whenCollectingResults_thenTheFailureIsReported(
            @TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("rom.bin"), new byte[64 * 1024]);
        FileScannerLoggingListener listener = new FileScannerLoggingListener();
        InterruptingListener interrupting = new InterruptingListener();

        ImmutableList<FileScanner.Result> results;
        try {
            results = scannerWith(listener, interrupting).scan(ImmutableList.of(tempDir));
        } finally {
            interrupting.release();
        }

        assertTrue(results.isEmpty(), "an interrupted scan collects no results");
        assertTrue(
                listener.isErrors(),
                "an interrupted scan must not look like a successful, empty one");
        assertTrue(
                Thread.interrupted(),
                "the interrupt must be handed back to the caller, not swallowed");
    }

    @Test
    void givenAnArchiveThatCannotBeRead_whenScanning_thenTheFailureIsReported(@TempDir Path tempDir)
            throws IOException {
        // Named like an archive, but its content is not one: the archive reader fails, and the
        // entries it would have contributed are missing from the report.
        Files.write(tempDir.resolve("broken.zip"), "not really a zip file".getBytes(UTF_8));
        FileScannerLoggingListener listener = new FileScannerLoggingListener();

        scannerWith(listener).scan(ImmutableList.of(tempDir));

        assertTrue(
                listener.isErrors(),
                "an archive that could not be read must not look like a complete scan of it");
    }

    /**
     * Interrupts the scanning caller and holds the worker thread, so result collection is
     * genuinely interrupted instead of racing a future that already completed.
     */
    private static final class InterruptingListener implements FileScanner.Listener {

        private final CountDownLatch holdWorker = new CountDownLatch(1);

        void release() {
            holdWorker.countDown();
        }

        @Override
        public void reportTotalItems(int totalItems) {
            // Runs on the scanning caller, right before the results are collected.
            Thread.currentThread().interrupt();
        }

        @Override
        public void reportStart(int thread, Path path, long bytes) {
            // Runs on the worker: keep its future incomplete until the test releases it.
            try {
                holdWorker.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void reportListing(Path path) {
        }

        @Override
        public void reportFinishedListing(int amount) {
        }

        @Override
        public void init(int numThreads) {
        }

        @Override
        public void reportBytesRead(int thread, long bytes) {
        }

        @Override
        public void reportSkip(int thread, Path path, String message) {
        }

        @Override
        public void reportFailure(int thread, Path path, String message, Throwable cause) {
        }

        @Override
        public void reportFinish(int thread, Path path) {
        }

        @Override
        public void reportAllFinished() {
        }
    }
}
