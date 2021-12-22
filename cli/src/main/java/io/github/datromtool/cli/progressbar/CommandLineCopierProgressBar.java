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
    public synchronized void reportStart(int thread, Path path, Path destination, long bytes) {

    }

    @Override
    public void reportBytesCopied(int thread, long bytes) {

    }

    @Override
    public void reportFailure(int thread, Path source, Path destination, String message, Throwable cause) {

    }

    @Override
    public void reportFinish(int thread, Path source, Path destination) {

    }

    @Override
    public synchronized void reportAllFinished() {

    }


}
