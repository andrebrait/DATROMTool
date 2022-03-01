package io.github.datromtool.io.spec.implementations;

import io.github.datromtool.io.spec.AbstractArchiveSourceInternalSpec;
import io.github.datromtool.io.spec.FileTimes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.BoundedInputStream;

import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class TarArchiveSourceInternalSpec extends AbstractArchiveSourceInternalSpec {

    @NonNull
    @Getter
    private final TarArchiveSourceSpec parent;
    @NonNull
    private final TarArchiveInputStream tarArchiveInputStream;
    @NonNull
    private final TarArchiveEntry tarArchiveEntry;

    // Stateful part
    private transient InputStream inputStream;

    @Override
    public String getName() {
        return tarArchiveEntry.getName();
    }

    @Override
    public long getSize() {
        return tarArchiveEntry.getRealSize();
    }

    @Override
    public FileTimes getFileTimes() {
        return FileTimes.from(tarArchiveEntry.getLastModifiedDate(), null, null);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            inputStream = new BoundedInputStream(tarArchiveInputStream, tarArchiveEntry.getRealSize());
        }
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        // Do nothing
    }
}
