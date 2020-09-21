package io.github.datromtool.cli.progressbar;

import io.github.datromtool.io.FileCopier;

import java.nio.file.Path;

public final class CommandLineCopierProgressBar implements FileCopier.Listener {

    @Override
    public void reportStart(Path path, Path destination, int thread) {

    }

    @Override
    public void reportProgress(
            Path path,
            Path destination,
            int thread,
            int percentage,
            long speed) {

    }

    @Override
    public void reportSkip(Path path, Path destination, int thread, String message) {

    }

    @Override
    public void reportFailure(
            Path path,
            Path destination,
            int thread,
            String message,
            Throwable cause) {

    }

    @Override
    public void reportFinish(Path path, Path destination, int thread) {

    }
}
