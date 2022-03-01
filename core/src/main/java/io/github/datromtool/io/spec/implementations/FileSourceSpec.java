package io.github.datromtool.io.spec.implementations;

import io.github.datromtool.display.CachingDisplayableAddressable;
import io.github.datromtool.io.spec.FileTimes;
import io.github.datromtool.io.spec.SourceSpec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileSourceSpec extends CachingDisplayableAddressable implements SourceSpec {

    private transient String $nameCache;

    @NonNull
    @Getter
    private final Path path;
    @Getter
    private final long size;
    @NonNull
    @Getter
    private final FileTimes fileTimes;

    // Stateful part
    private transient InputStream inputStream;

    @Nonnull
    public static FileSourceSpec from(@Nonnull Path path) throws IOException {
        Path normalizedPath = path.toAbsolutePath().normalize();
        BasicFileAttributes attributes = Files.readAttributes(normalizedPath, BasicFileAttributes.class);
        long size = attributes.size();
        FileTimes fileTimes = FileTimes.from(attributes);
        return new FileSourceSpec(normalizedPath, size, fileTimes);
    }

    @Override
    public String getName() {
        if ($nameCache == null) {
            $nameCache = path.toString();
        }
        return $nameCache;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            inputStream = Files.newInputStream(path);
        }
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
            inputStream = null;
        }
    }
}
