package io.github.datromtool.io.copy.archive.impl;

import io.github.datromtool.io.compression.CompressionAlgorithm;
import io.github.datromtool.io.compression.Compressor;
import io.github.datromtool.io.copy.FileTimes;
import io.github.datromtool.io.copy.SourceSpec;
import io.github.datromtool.io.copy.archive.AbstractArchiveDestinationSpec;
import io.github.datromtool.io.copy.archive.ArchiveDestinationInternalSpec;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TarArchiveDestinationSpec extends AbstractArchiveDestinationSpec {

    private final Compressor compressor;

    // Stateful part
    private transient TarArchiveOutputStream tarArchiveOutputStream;

    public TarArchiveDestinationSpec(@Nullable CompressionAlgorithm algorithm, @Nonnull Path path) {
        this(algorithm != null ? algorithm.getCompressor() : null, path);
    }

    public TarArchiveDestinationSpec(@Nullable Compressor compressor, @Nonnull Path path) {
        super(path);
        this.compressor = compressor;
    }

    @Override
    public ArchiveDestinationInternalSpec createInternalDestinationSpecFor(String name, SourceSpec sourceSpec) throws IOException {
        if (tarArchiveOutputStream == null) {
            OutputStream rawOutputStream = Files.newOutputStream(getPath());
            if (compressor != null) {
                tarArchiveOutputStream = new TarArchiveOutputStream(compressor.compress(rawOutputStream));
            } else {
                tarArchiveOutputStream = new TarArchiveOutputStream(rawOutputStream);
            }
        }
        TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(name);
        tarArchiveEntry.setSize(sourceSpec.getSize());
        FileTimes fileTimes = sourceSpec.getFileTimes();
        if (fileTimes.getLastModifiedTime() != null) {
            tarArchiveEntry.setModTime(fileTimes.getLastModifiedTime());
        }
        tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);
        return new TarArchiveDestinationInternalSpec(this, tarArchiveOutputStream, tarArchiveEntry);
    }

    @Override
    public void close() throws IOException {
        if (tarArchiveOutputStream != null) {
            tarArchiveOutputStream.close();
            tarArchiveOutputStream = null;
        }
    }
}
