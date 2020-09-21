package io.github.datromtool.cli.progressbar;

import io.github.datromtool.io.FileScanner;

import java.nio.file.Path;

public final class CommandLineScannerProgressBar implements FileScanner.Listener {

    @Override
    public void reportTotalItems(int totalItems) {

    }

    @Override
    public void reportStart(Path path, int thread) {

    }

    @Override
    public void reportProgress(Path path, int thread, int percentage, long speed) {

    }

    @Override
    public void reportSkip(Path path, int thread, String message) {

    }

    @Override
    public void reportFailure(Path path, int thread, String message, Throwable cause) {

    }

    @Override
    public void reportFinish(Path path, int thread) {

    }
}
