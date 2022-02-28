package io.github.datromtool.io.spec.implementations;

import io.github.datromtool.io.spec.AbstractArchiveSourceSpec;
import io.github.datromtool.io.spec.ArchiveSourceInternalSpec;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;

public final class ZipArchiveSourceSpec extends AbstractArchiveSourceSpec {

    // Stateful part
    private transient ZipFile zipFile;
    private transient Enumeration<ZipArchiveEntry> entries;

    public ZipArchiveSourceSpec(@Nonnull Path path) {
        super(path);
    }

    public ZipArchiveSourceSpec(@Nonnull Path path, @Nonnull Iterable<String> names) {
        super(path, names);
    }

    @Override
    protected void initArchive() throws IOException {
        if (zipFile == null) {
            zipFile = new ZipFile(getPath().toFile());
        }
        if (entries == null) {
            entries = zipFile.getEntriesInPhysicalOrder();
        }
    }

    @Nullable
    @Override
    protected ArchiveSourceInternalSpec getNextEntry() {
        while (entries.hasMoreElements()) {
            ZipArchiveEntry zipArchiveEntry = entries.nextElement();
            if (isFile(zipArchiveEntry)) {
                return new ZipArchiveSourceInternalSpec(this, zipFile, zipArchiveEntry);
            }
        }
        return null;
    }

    private static boolean isFile(ZipArchiveEntry zipArchiveEntry) {
        return !zipArchiveEntry.isDirectory() && !zipArchiveEntry.isUnixSymlink();
    }

    @Override
    protected void closeArchive() throws IOException {
        entries = null;
        if (zipFile != null) {
            zipFile.close();
        }
    }
}
