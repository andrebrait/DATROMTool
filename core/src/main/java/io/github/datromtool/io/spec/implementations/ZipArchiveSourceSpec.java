package io.github.datromtool.io.spec.implementations;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.io.ArchiveType;
import io.github.datromtool.io.spec.ArchiveSourceInternalSpec;
import io.github.datromtool.io.spec.ArchiveSourceSpec;
import io.github.datromtool.io.spec.exceptions.ArchiveEntryNotFoundException;
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
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZipArchiveSourceSpec implements ArchiveSourceSpec {

    @NonNull
    @Getter
    private final Path path;
    @NonNull
    private final ImmutableSet<String> names;

    // Stateful part
    private transient ZipFile zipFile;
    private transient Enumeration<ZipArchiveEntry> entries;
    private transient Set<String> mutableNames;

    /**
     * Creates a ZipArchiveSourceSpec which will iterate over all valid entries in the archive in physical order.
     */
    @Nonnull
    public static ZipArchiveSourceSpec from(@Nonnull Path path) {
        return from(path, ImmutableSet.of());
    }

    /**
     * Creates a ZipArchiveSourceSpec which will iterate over the selected entries (by name) in the archive.
     *  The order of iteration will be the physical order in which they appear in the archive, regardless of
     *  the order in the provided collection of names.
     */
    @Nonnull
    public static ZipArchiveSourceSpec from(@Nonnull Path path, @Nonnull Iterable<String> names) {
        return new ZipArchiveSourceSpec(path.toAbsolutePath().normalize(), ImmutableSet.copyOf(names));
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
        if (entries == null) {
            entries = zipFile.getEntriesInPhysicalOrder();
        }
        if (names.isEmpty()) {
            while (entries.hasMoreElements()) {
                ZipArchiveEntry zipArchiveEntry = entries.nextElement();
                if (isFile(zipArchiveEntry)) {
                    return new ZipArchiveSourceInternalSpec(this, zipFile, zipArchiveEntry);
                }
            }
        } else {
            if (mutableNames == null) {
                mutableNames = new HashSet<>(names);
            }
            while (entries.hasMoreElements()) {
                ZipArchiveEntry zipArchiveEntry = entries.nextElement();
                if (isFile(zipArchiveEntry) && mutableNames.remove(zipArchiveEntry.getName())) {
                    return new ZipArchiveSourceInternalSpec(this, zipFile, zipArchiveEntry);
                }
            }
            if (!mutableNames.isEmpty()) {
                throw new ArchiveEntryNotFoundException(path, mutableNames);
            }
        }
        return null;
    }

    private static boolean isFile(@Nullable ZipArchiveEntry zipArchiveEntry) {
        return zipArchiveEntry != null && !zipArchiveEntry.isDirectory() && !zipArchiveEntry.isUnixSymlink();
    }

    @Override
    public void close() throws IOException {
        mutableNames = null;
        entries = null;
        if (zipFile != null) {
            zipFile.close();
        }
    }
}
