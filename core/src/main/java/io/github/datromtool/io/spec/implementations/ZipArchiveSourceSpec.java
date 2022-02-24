package io.github.datromtool.io.spec.implementations;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.io.ArchiveType;
import io.github.datromtool.io.spec.ArchiveSourceInternalSpec;
import io.github.datromtool.io.spec.ArchiveSourceSpec;
import io.github.datromtool.io.spec.exceptions.ArchiveEntryNotFoundException;
import io.github.datromtool.io.spec.exceptions.InvalidArchiveEntryException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZipArchiveSourceSpec implements ArchiveSourceSpec {

    @NonNull
    @Getter
    private final Path path;
    @NonNull
    private final ImmutableList<String> names;

    // Stateful part
    private transient ZipFile zipFile;
    private transient Enumeration<ZipArchiveEntry> entries;
    private transient Iterator<String> namesIterator;

    @Nonnull
    public static ZipArchiveSourceSpec from(@Nonnull Path path) {
        return from(path, ImmutableList.of());
    }

    @Nonnull
    public static ZipArchiveSourceSpec from(@Nonnull Path path, @Nonnull List<String> names) {
        return new ZipArchiveSourceSpec(path.toAbsolutePath().normalize(), ImmutableList.copyOf(names));
    }

    @Override
    public ArchiveType getType() {
        return ArchiveType.ZIP;
    }

    @Nullable
    @Override
    public ArchiveSourceInternalSpec getNextInternalSpec() throws IOException {
        if (zipFile == null) {
            zipFile = new ZipFile(path.toFile());
        }
        if (names.isEmpty()) {
            if (entries == null) {
                entries = zipFile.getEntriesInPhysicalOrder();
            }
            while (entries.hasMoreElements()) {
                ZipArchiveEntry zipArchiveEntry = entries.nextElement();
                if (isFile(zipArchiveEntry)) {
                    return new ZipArchiveSourceInternalSpec(this, zipFile, zipArchiveEntry);
                }
            }
        } else {
            if (namesIterator == null) {
                namesIterator = names.iterator();
            }
            if (namesIterator.hasNext()) {
                String name = namesIterator.next();
                ZipArchiveEntry zipArchiveEntry = zipFile.getEntry(name);
                return getValidZipArchiveEntry(name, zipArchiveEntry);
            }
        }
        return null;
    }

    @Nonnull
    private ZipArchiveSourceInternalSpec getValidZipArchiveEntry(String name, ZipArchiveEntry zipArchiveEntry) throws IOException {
        if (isFile(zipArchiveEntry)) {
            return new ZipArchiveSourceInternalSpec(this, zipFile, zipArchiveEntry);
        } else if (zipArchiveEntry == null) {
            throw new ArchiveEntryNotFoundException(path, name);
        } else {
            throw new InvalidArchiveEntryException(path, name);
        }
    }

    private boolean isFile(@Nullable ZipArchiveEntry zipArchiveEntry) {
        return zipArchiveEntry != null && !zipArchiveEntry.isDirectory() && !zipArchiveEntry.isUnixSymlink();
    }

    @Override
    public void close() throws IOException {
        entries = null;
        namesIterator = null;
        if (zipFile != null) {
            zipFile.close();
        }
    }
}
