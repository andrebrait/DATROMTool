package io.github.datromtool.io.copy.archive.impl;

import io.github.datromtool.io.copy.FileTimes;
import io.github.datromtool.io.copy.archive.AbstractArchiveSourceInternalSpec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.input.BoundedInputStream;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class SevenZipArchiveSourceInternalSpec extends AbstractArchiveSourceInternalSpec {

    @Getter
    @NonNull
    private final SevenZipArchiveSourceSpec parent;
    @NonNull
    private final SevenZFile sevenZFile;
    @NonNull
    private final SevenZArchiveEntry sevenZArchiveEntry;

    // Stateful part
    private transient InputStream inputStream;

    @Override
    public String getName() {
        return sevenZArchiveEntry.getName();
    }

    @Override
    public long getSize() {
        return sevenZArchiveEntry.getSize();
    }

    @Override
    public FileTimes getFileTimes() {
        return FileTimes.from(
                sevenZArchiveEntry.getHasLastModifiedDate() ? sevenZArchiveEntry.getLastModifiedDate() : null,
                sevenZArchiveEntry.getHasAccessDate() ? sevenZArchiveEntry.getAccessDate() : null,
                sevenZArchiveEntry.getHasCreationDate() ? sevenZArchiveEntry.getCreationDate() : null);
    }

    @Override
    public InputStream getInputStream() {
        if (inputStream == null) {
            BoundedInputStream boundedInputStream = new BoundedInputStream(new SevenZFileInputStream(sevenZFile), sevenZArchiveEntry.getSize());
            boundedInputStream.setPropagateClose(false);
            inputStream = boundedInputStream;
        }
        return inputStream;
    }

    @Override
    public void close() {
        // No need to close this InputStream
        inputStream = null;
    }

    /**
     * An InputStream which reads bytes directly from a SevenZFile.
     * Although SevenZFile has {@link SevenZFile#getInputStream(SevenZArchiveEntry)}, it's slower for some archive formats.
     */
    @RequiredArgsConstructor
    private static final class SevenZFileInputStream extends InputStream {

        private final SevenZFile sevenZFile;

        @Override
        public int read() throws IOException {
            return sevenZFile.read();
        }

        @Override
        public int read(@Nonnull byte[] b) throws IOException {
            return sevenZFile.read(b);
        }

        @Override
        public int read(@Nonnull byte[] b, int off, int len) throws IOException {
            return sevenZFile.read(b, off, len);
        }
    }
}
