package io.github.datromtool.io.spec.implementations;

import io.github.datromtool.io.spec.AbstractArchiveSourceInternalSpec;
import io.github.datromtool.io.spec.FileTimes;
import io.github.datromtool.util.ArchiveUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class ZipArchiveSourceInternalSpec extends AbstractArchiveSourceInternalSpec {

    @Getter
    private final ZipArchiveSourceSpec parent;
    private final ZipFile zipFile;
    private final ZipArchiveEntry zipArchiveEntry;

    // Stateful part
    private transient InputStream inputStream;

    @Override
    public String getName() {
        return ArchiveUtils.normalizePath(zipArchiveEntry.getName());
    }

    @Override
    public long getSize() {
        return zipArchiveEntry.getSize();
    }

    @Override
    public FileTimes getFileTimes() {
        return FileTimes.builder()
                .lastModifiedTime(zipArchiveEntry.getLastModifiedTime())
                .lastAccessTime(zipArchiveEntry.getLastAccessTime())
                .creationTime(zipArchiveEntry.getCreationTime())
                .build();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            inputStream = zipFile.getInputStream(zipArchiveEntry);
        }
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}
