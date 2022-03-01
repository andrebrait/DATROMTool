package io.github.datromtool.io.copy.archive.impl;

import io.github.datromtool.io.copy.archive.AbstractArchiveDestinationInternalSpec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.IOException;
import java.io.OutputStream;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class ZipArchiveDestinationInternalSpec extends AbstractArchiveDestinationInternalSpec {

    @NonNull
    @Getter
    private final ZipArchiveDestinationSpec parent;
    @NonNull
    private final ZipArchiveOutputStream zipArchiveOutputStream;
    @NonNull
    private final ZipArchiveEntry entry;

    // Stateful part
    private transient OutputStream outputStream;

    @Override
    public String getName() {
        return entry.getName();
    }

    @Override
    public OutputStream getOutputStream() {
        if (outputStream == null) {
            outputStream = new NonCloseableOutputStream(zipArchiveOutputStream);
        }
        return outputStream;
    }

    @Override
    public void close() throws IOException {
        if (outputStream != null) {
            outputStream.close();
            zipArchiveOutputStream.closeArchiveEntry();
            outputStream = null;
        }
    }
}
