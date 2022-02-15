package io.github.datromtool.io.logging;

import io.github.datromtool.ByteSize;
import io.github.datromtool.io.FileCopier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public final class FileCopierLoggingListener implements FileCopier.Listener {

    @Getter
    private volatile boolean errors = false;

    @Override
    public void init(int numThreads) {
        log.info("Starting File Copier with {} threads", numThreads);
    }

    @Override
    public void reportTotalItems(int totalItems) {
        log.info("Total file count: {}", totalItems);
    }

    @Override
    public void reportStart(int thread, Path source, Path destination, long bytes) {
        if (log.isInfoEnabled()) {
            log.info("Copying {} from '{}' to '{}'", ByteSize.fromBytes(bytes).toFormattedString(), source, destination);
        }
    }

    @Override
    public void reportBytesCopied(int thread, long bytes) {
        // do nothing
    }

    @Override
    public void reportFailure(int thread, Path source, Path destination, String message, Throwable cause) {
        errors = true;
        log.error("Failed to copy '{}' to '{}'. Message: '{}'.", source, destination, message, cause);
    }

    @Override
    public void reportFinish(int thread, Path source, Path destination) {
        log.info("Finished copying '{}' to '{}'", source, destination);
    }

    @Override
    public void reportAllFinished() {
        log.info("File copy finished");
    }
}
