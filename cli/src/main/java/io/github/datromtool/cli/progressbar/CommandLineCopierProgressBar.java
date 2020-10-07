package io.github.datromtool.cli.progressbar;

import io.github.datromtool.io.FileCopier;

import java.nio.file.Path;

public class CommandLineCopierProgressBar implements FileCopier.Listener {

    private int numThreads;

    @Override
    public synchronized void init(int numThreads) {
        this.numThreads = numThreads;
    }

    @Override
    public synchronized void reportStart(Path path, Path destination, int thread) {

    }

    @Override
    public synchronized void reportProgress(
            Path path,
            Path destination,
            int thread,
            int percentage,
            long speed) {

    }

    @Override
    public synchronized void reportFailure(
            Path path,
            Path destination,
            int thread,
            String message,
            Throwable cause) {

    }

    @Override
    public synchronized void reportFinish(Path path, Path destination, int thread) {

    }

    @Override
    public synchronized void reportAllFinished() {

    }
}
