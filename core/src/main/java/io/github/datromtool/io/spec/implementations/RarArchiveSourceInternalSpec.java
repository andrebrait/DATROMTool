package io.github.datromtool.io.spec.implementations;

import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import io.github.datromtool.io.spec.AbstractArchiveSourceInternalSpec;
import io.github.datromtool.io.spec.FileTimes;
import io.github.datromtool.util.ArchiveUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class RarArchiveSourceInternalSpec extends AbstractArchiveSourceInternalSpec {

    @Getter
    @NonNull
    private final RarArchiveSourceSpec parent;
    @NonNull
    private final Archive archive;
    @NonNull
    private final FileHeader fileHeader;

    // Stateful part
    private transient InputStream inputStream;

    @Override
    public String getName() {
        return ArchiveUtils.normalizePath(fileHeader.getFileName());
    }

    @Override
    public long getSize() {
        return fileHeader.getFullUnpackSize();
    }

    @Override
    public FileTimes getFileTimes() {
        return FileTimes.from(fileHeader.getMTime(), fileHeader.getATime(), fileHeader.getCTime());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            inputStream = archive.getInputStream(fileHeader);
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
