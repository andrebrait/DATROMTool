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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

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
            tarArchiveOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
        }
        TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(name);
        tarArchiveEntry.setSize(sourceSpec.getSize());
        FileTimes fileTimes = sourceSpec.getFileTimes();
        if (fileTimes.getLastModifiedTime() != null) {
            tarArchiveEntry.setModTime(fileTimes.getLastModifiedTime());
        }
        // Workaround for this not being handled in Apache Commons Compress
        addPaxHeader(tarArchiveEntry, "mtime", fileTimes.getLastModifiedTime());
        addPaxHeader(tarArchiveEntry, "ctime", fileTimes.getCreationTime());
        addPaxHeader(tarArchiveEntry, "atime", fileTimes.getLastAccessTime());
        tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);
        return new TarArchiveDestinationInternalSpec(this, tarArchiveOutputStream, tarArchiveEntry);
    }

    private void addPaxHeader(TarArchiveEntry entry, String header, FileTime value) {
        if (value != null) {
            Instant instant = value.toInstant();
            if (header.equals("mtime") && instant.getNano() == 0) {
                // Let Apache Commons Compress handle it
                return;
            }
            addFileTimePaxHeader(entry, header, value);
        }
    }

    private void addFileTimePaxHeader(TarArchiveEntry entry, String header, FileTime value) {
        if (value != null) {
            Instant instant = value.toInstant();
            long seconds = instant.getEpochSecond();
            int nanos = instant.getNano();
            if (nanos == 0) {
                entry.addPaxHeader(header, String.valueOf(seconds));
            } else {
                addInstantPaxHeader(entry, header, seconds, nanos);
            }
        }
    }

    private void addInstantPaxHeader(TarArchiveEntry entry, String header, long seconds, int nanos) {
        BigDecimal bdSeconds = BigDecimal.valueOf(seconds);
        BigDecimal bdNanos = BigDecimal.valueOf(nanos).movePointLeft(9).setScale(7, RoundingMode.DOWN);
        BigDecimal timestamp = bdSeconds.add(bdNanos);
        entry.addPaxHeader(header, timestamp.toPlainString());
    }

    @Override
    public void close() throws IOException {
        if (tarArchiveOutputStream != null) {
            tarArchiveOutputStream.close();
            tarArchiveOutputStream = null;
        }
    }
}
