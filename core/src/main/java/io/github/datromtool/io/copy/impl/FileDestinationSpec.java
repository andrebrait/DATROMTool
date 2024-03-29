package io.github.datromtool.io.copy.impl;

import io.github.datromtool.display.CachingDisplayableAddressable;
import io.github.datromtool.io.copy.DestinationSpec;
import io.github.datromtool.io.copy.SourceSpec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileDestinationSpec extends CachingDisplayableAddressable implements DestinationSpec {

    private transient String $nameCache;

    @NonNull
    @Getter
    private final Path path;
    @NonNull
    private final SourceSpec sourceSpec;

    // Stateful part
    private transient OutputStream outputStream;

    @Nonnull
    public static FileDestinationSpec of(@Nonnull Path path, @Nonnull SourceSpec sourceSpec) {
        return new FileDestinationSpec(path.toAbsolutePath().normalize(), sourceSpec);
    }

    @Override
    public String getName() {
        if ($nameCache == null) {
            $nameCache = path.toString();
        }
        return $nameCache;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = Files.newOutputStream(path);
        }
        return outputStream;
    }

    @Override
    public void close() throws IOException {
        if (outputStream != null) {
            outputStream.close();
            outputStream = null;
        }
        sourceSpec.getFileTimes().applyTo(path);
    }
}

