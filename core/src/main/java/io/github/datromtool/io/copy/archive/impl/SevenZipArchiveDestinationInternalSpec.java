package io.github.datromtool.io.copy.archive.impl;

import io.github.datromtool.io.copy.archive.AbstractArchiveDestinationInternalSpec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class SevenZipArchiveDestinationInternalSpec extends AbstractArchiveDestinationInternalSpec {

    @NonNull
    @Getter
    private final SevenZipArchiveDestinationSpec parent;
    @NonNull
    private final SevenZOutputFile sevenZOutputFile;
    @NonNull
    private final SevenZArchiveEntry entry;

    // Stateful part
    private transient OutputStream outputStream;

    @Override
    public String getName() {
        return entry.getName();
    }

    @Override
    public OutputStream getOutputStream() {
        if (outputStream == null) {
            outputStream = new SevenZipOutputStream(sevenZOutputFile);
        }
        return outputStream;
    }

    @Override
    public void close() throws IOException {
        if (outputStream != null) {
            outputStream.close();
            sevenZOutputFile.closeArchiveEntry();
            outputStream = null;
        }
    }

    @RequiredArgsConstructor
    private static final class SevenZipOutputStream extends OutputStream {

        private final SevenZOutputFile sevenZOutputFile;

        @Override
        public void write(int b) throws IOException {
            sevenZOutputFile.write(b);
        }

        @Override
        public void write(@Nonnull byte[] b) throws IOException {
            sevenZOutputFile.write(b);
        }

        @Override
        public void write(@Nonnull byte[] b, int off, int len) throws IOException {
            sevenZOutputFile.write(b, off, len);
        }
    }
}
