package io.github.datromtool.io.copy.archive.impl;

import io.github.datromtool.io.copy.archive.AbstractArchiveSourceSpec;
import io.github.datromtool.io.copy.archive.ArchiveSourceInternalSpec;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;

public final class SevenZipArchiveSourceSpec extends AbstractArchiveSourceSpec {

    // Stateful part
    private transient SevenZFile sevenZFile;

    public SevenZipArchiveSourceSpec(@Nonnull Path path) {
        super(path);
    }

    public SevenZipArchiveSourceSpec(@Nonnull Path path, @Nonnull Iterable<String> names) {
        super(path, names);
    }

    @Override
    protected void initArchive() throws IOException {
        if (sevenZFile == null) {
            sevenZFile = new SevenZFile(getPath().toFile());
        }
    }

    @Nullable
    @Override
    protected ArchiveSourceInternalSpec getNextEntry() throws IOException {
        SevenZArchiveEntry sevenZArchiveEntry;
        while ((sevenZArchiveEntry = sevenZFile.getNextEntry()) != null) {
            if (isFile(sevenZArchiveEntry)) {
                return new SevenZipArchiveSourceInternalSpec(this, sevenZFile, sevenZArchiveEntry);
            }
        }
        return null;
    }

    private boolean isFile(SevenZArchiveEntry sevenZArchiveEntry) {
        return !sevenZArchiveEntry.isDirectory() && !sevenZArchiveEntry.isAntiItem();
    }

    @Override
    protected void closeArchive() throws IOException {
        if (sevenZFile != null) {
            sevenZFile.close();
            sevenZFile = null;
        }
    }
}
