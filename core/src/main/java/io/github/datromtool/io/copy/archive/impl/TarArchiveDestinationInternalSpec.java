package io.github.datromtool.io.copy.archive.impl;

import io.github.datromtool.io.copy.archive.AbstractArchiveDestinationInternalSpec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.IOException;
import java.io.OutputStream;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class TarArchiveDestinationInternalSpec extends AbstractArchiveDestinationInternalSpec {

    @NonNull
    @Getter
    private final TarArchiveDestinationSpec parent;
    @NonNull
    private final TarArchiveOutputStream tarArchiveOutputStream;
    @NonNull
    private final TarArchiveEntry entry;

    // Stateful part
    private transient OutputStream outputStream;

    @Override
    public String getName() {
        return entry.getName();
    }

    @Override
    public OutputStream getOutputStream() {
        if (outputStream == null) {
            outputStream = new NonCloseableOutputStream(tarArchiveOutputStream);
        }
        return outputStream;
    }

    @Override
    public void close() throws IOException {
        if (outputStream != null) {
            outputStream.close();
            tarArchiveOutputStream.closeArchiveEntry();
            outputStream = null;
        }
    }
}
