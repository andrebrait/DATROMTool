package io.github.datromtool.io.logging;

import io.github.datromtool.ByteSize;
import io.github.datromtool.io.FileScanner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public final class FileScannerLoggingListener implements FileScanner.Listener {

    @Getter
    private volatile boolean errors = false;

    @Override
    public void reportListing(Path path) {
        log.info("Listing files under '{}'", path);
    }

    @Override
    public void reportFinishedListing(int amount) {
        // do nothing
    }

    @Override
    public void init(int numThreads) {
        log.info("Starting File Scanner with {} threads", numThreads);
    }

    @Override
    public void reportTotalItems(int totalItems) {
        log.info("Total file count: {}", totalItems);
    }

    @Override
    public void reportStart(int thread, Path path, long bytes) {
        if (log.isInfoEnabled()) {
            log.info("Scanning '{}' (size: {})", path, ByteSize.fromBytes(bytes).toFormattedString());
        }
    }

    @Override
    public void reportBytesRead(int thread, long bytes) {
        // do nothing
    }

    @Override
    public void reportSkip(int thread, Path path, String message) {
        log.warn("Skipping '{}'. Message: '{}'", path, message);
    }

    @Override
    public void reportFailure(int thread, Path path, String message, Throwable cause) {
        errors = true;
        log.error("Failed to scan '{}'. Message: '{}'.", path, message, cause);
    }

    @Override
    public void reportFinish(int thread, Path path) {
        log.info("Finished scanning '{}'", path);
    }

    @Override
    public void reportAllFinished() {
        log.info("File scan finished");
    }
}
