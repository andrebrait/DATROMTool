package io.github.datromtool.io.spec.implementations;

import io.github.datromtool.io.spec.AbstractArchiveSourceSpec;
import io.github.datromtool.io.spec.ArchiveSourceInternalSpec;
import io.github.datromtool.io.spec.compression.CompressionAlgorithm;
import io.github.datromtool.io.spec.compression.Decompressor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TarArchiveSourceSpec extends AbstractArchiveSourceSpec {

    private final Decompressor decompressor;

    // Stateful part

    private transient TarArchiveInputStream tarArchiveInputStream;

    public TarArchiveSourceSpec(@Nullable CompressionAlgorithm compressionAlgorithm, @Nonnull Path path) {
        this(compressionAlgorithm != null ? compressionAlgorithm.getDecompressor() : null, path);
    }

    public TarArchiveSourceSpec(@Nullable CompressionAlgorithm compressionAlgorithm, @Nonnull Path path, @Nonnull Iterable<String> names) {
        this(compressionAlgorithm != null ? compressionAlgorithm.getDecompressor() : null, path, names);
    }

    public TarArchiveSourceSpec(@Nullable Decompressor decompressor, @Nonnull Path path) {
        super(path);
        this.decompressor = decompressor;
    }

    public TarArchiveSourceSpec(@Nullable Decompressor decompressor, @Nonnull Path path, @Nonnull Iterable<String> names) {
        super(path, names);
        this.decompressor = decompressor;
    }

    @Override
    protected void initArchive() throws IOException {
        if (tarArchiveInputStream == null) {
            InputStream rawInputStream = Files.newInputStream(getPath());
            if (decompressor != null) {
                tarArchiveInputStream = new TarArchiveInputStream(decompressor.decompress(rawInputStream));
            } else {
                tarArchiveInputStream = new TarArchiveInputStream(rawInputStream);
            }
        }
    }

    @Nullable
    @Override
    protected ArchiveSourceInternalSpec getNextEntry() throws IOException {
        TarArchiveEntry tarArchiveEntry;
        while ((tarArchiveEntry = tarArchiveInputStream.getNextTarEntry()) != null) {
            if (tarArchiveEntry.isFile() && tarArchiveInputStream.canReadEntryData(tarArchiveEntry)) {
                return new TarArchiveSourceInternalSpec(this, tarArchiveInputStream, tarArchiveEntry);
            }
        }
        return null;
    }

    @Override
    protected void closeArchive() throws IOException {
        if (tarArchiveInputStream != null) {
            tarArchiveInputStream.close();
        }
    }
}
