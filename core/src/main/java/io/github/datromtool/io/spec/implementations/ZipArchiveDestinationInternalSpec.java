package io.github.datromtool.io.spec.implementations;

import io.github.datromtool.io.spec.AbstractArchiveDestinationInternalSpec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.IOException;
import java.io.OutputStream;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class ZipArchiveDestinationInternalSpec extends AbstractArchiveDestinationInternalSpec {

    @Getter
    private final ZipArchiveDestinationSpec parent;
    private final ZipArchiveOutputStream zipArchiveOutputStream;
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
