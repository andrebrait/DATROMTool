package io.github.datromtool.io.copy.archive.impl.process;

import io.github.datromtool.io.copy.FileTimes;
import io.github.datromtool.io.copy.archive.CachingAbstractArchiveSourceInternalSpec;
import io.github.datromtool.util.ArchiveUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.BoundedInputStream;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class ProcessArchiveSourceInternalSpec extends CachingAbstractArchiveSourceInternalSpec {

    @Getter
    @NonNull
    private final ProcessArchiveSourceSpec parent;
    @NonNull
    private final InputStream processInputStream;
    @NonNull
    private final ProcessArchiveFile file;

    // Stateful part
    private transient InputStream inputStream;

    @Nonnull
    @Override
    protected String getNameForCache() {
        return ArchiveUtils.normalizePath(file.getName());
    }

    @Override
    public long getSize() {
        return file.getSize();
    }

    @Override
    public FileTimes getFileTimes() {
        return file.getFileTimes();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            inputStream = new BoundedInputStream(processInputStream, file.getSize());
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
