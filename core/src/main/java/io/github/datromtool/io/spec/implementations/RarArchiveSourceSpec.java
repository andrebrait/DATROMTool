package io.github.datromtool.io.spec.implementations;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.io.ArchiveType;
import io.github.datromtool.io.spec.ArchiveSourceInternalSpec;
import io.github.datromtool.io.spec.ArchiveSourceSpec;
import io.github.datromtool.io.spec.exceptions.ArchiveEntryNotFoundException;
import io.github.datromtool.util.ArchiveUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RarArchiveSourceSpec implements ArchiveSourceSpec {

    @NonNull
    @Getter
    private final Path path;
    @NonNull
    private final ImmutableSet<String> names;

    // Stateful part
    private transient Archive archive;
    private transient Set<String> mutableNames;

    /**
     * Creates a RarArchiveSourceSpec which will iterate over all valid entries in the order they appear in the archive.
     */
    @Nonnull
    public static RarArchiveSourceSpec from(@Nonnull Path path) {
        return from(path, Collections.emptySet());
    }

    /**
     * Creates a RarArchiveSourceSpec which will iterate over the selected entries (by name) in the archive.
     * The order of iteration will be the same in which they appear in the archive, regardless of the order in
     * the provided collection of names.
     */
    @Nonnull
    public static RarArchiveSourceSpec from(@Nonnull Path path, @Nonnull Iterable<String> names) {
        return new RarArchiveSourceSpec(path.toAbsolutePath().normalize(), ImmutableSet.copyOf(names));
    }

    @Override
    public ArchiveType getType() {
        return ArchiveType.RAR;
    }

    @Nullable
    @Override
    public ArchiveSourceInternalSpec getNextInternalSpec() throws IOException {
        if (archive == null) {
            try {
                archive = new Archive(path.toFile());
            } catch (RarException e) {
                throw new IOException(format("Could not open '%s'", path), e);
            }
        }
        if (names.isEmpty()) {
            FileHeader fileHeader;
            while ((fileHeader = archive.nextFileHeader()) != null) {
                if (isFile(fileHeader)) {
                    return new RarArchiveSourceInternalSpec(this, archive, fileHeader);
                }
            }
        } else {
            if (mutableNames == null) {
                mutableNames = new HashSet<>(names);
            }
            FileHeader fileHeader;
            while ((fileHeader = archive.nextFileHeader()) != null) {
                if (isFile(fileHeader) && mutableNames.remove(ArchiveUtils.normalizePath(fileHeader.getFileName()))) {
                    return new RarArchiveSourceInternalSpec(this, archive, fileHeader);
                }
            }
            if (!mutableNames.isEmpty()) {
                throw new ArchiveEntryNotFoundException(path, mutableNames);
            }
        }
        return null;
    }

    private static boolean isFile(FileHeader fileHeader) {
        return fileHeader.isFileHeader() && !fileHeader.isDirectory();
    }

    @Override
    public void close() throws IOException {
        mutableNames = null;
        if (archive != null) {
            archive.close();
        }
    }
}
