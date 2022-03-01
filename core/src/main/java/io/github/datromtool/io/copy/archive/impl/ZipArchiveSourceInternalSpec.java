package io.github.datromtool.io.copy.archive.impl;

import io.github.datromtool.io.copy.FileTimes;
import io.github.datromtool.io.copy.archive.AbstractArchiveSourceInternalSpec;
import io.github.datromtool.util.ArchiveUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class ZipArchiveSourceInternalSpec extends AbstractArchiveSourceInternalSpec {

    @Getter
    @NonNull
    private final ZipArchiveSourceSpec parent;
    @NonNull
    private final ZipFile zipFile;
    @NonNull
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
        // When reading an existing entry, ZipArchiveEntry can parse these times from Extra fields correctly
        // (X000A_NTFS and X5455_ExtendedTimestamp)
        // No need to parse them by hand
        return FileTimes.from(
                zipArchiveEntry.getLastModifiedTime(),
                zipArchiveEntry.getLastAccessTime(),
                zipArchiveEntry.getCreationTime());
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
            inputStream = null;
        }
    }
}
