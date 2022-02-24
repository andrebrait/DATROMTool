package io.github.datromtool.io.spec.implementations;

import io.github.datromtool.display.CachingDisplayableAddressable;
import io.github.datromtool.io.spec.DestinationSpec;
import io.github.datromtool.io.spec.SourceSpec;
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
        return path.toString();
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
        }
        sourceSpec.getFileTimes().applyTo(path);
    }
}

