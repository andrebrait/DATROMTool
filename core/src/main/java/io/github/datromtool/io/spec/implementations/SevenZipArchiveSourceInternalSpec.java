package io.github.datromtool.io.spec.implementations;

import io.github.datromtool.io.spec.AbstractArchiveSourceInternalSpec;
import io.github.datromtool.io.spec.FileTimes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.BoundedInputStream;

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
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            inputStream = new BoundedInputStream(new SevenZFileInputStream(sevenZFile), sevenZArchiveEntry.getSize());
        }
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
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
