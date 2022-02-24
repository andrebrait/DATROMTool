package io.github.datromtool.io.spec.implementations;

import io.github.datromtool.io.spec.AbstractArchiveDestinationInternalSpec;
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
            outputStream = zipArchiveOutputStream;
        }
        return outputStream;
    }

    @Override
    public void close() throws IOException {
        if (outputStream != null) {
            zipArchiveOutputStream.closeArchiveEntry();
        }
    }
}
