package io.github.datromtool.io.spec.implementations;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.io.ArchiveType;
import io.github.datromtool.io.spec.ArchiveSourceInternalSpec;
import io.github.datromtool.io.spec.ArchiveSourceSpec;
import io.github.datromtool.io.spec.exceptions.ArchiveEntryNotFoundException;
import io.github.datromtool.io.spec.exceptions.InvalidArchiveEntryException;
import io.github.datromtool.util.ArchiveUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static java.lang.String.format;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RarArchiveSourceSpec implements ArchiveSourceSpec {

    @NonNull
    @Getter
    private final Path path;
    @NonNull
    private final ImmutableList<String> names;

    @Nonnull
    public static RarArchiveSourceSpec from(@Nonnull Path path) {
        return from(path, ImmutableList.of());
    }

    @Nonnull
    public static RarArchiveSourceSpec from(@Nonnull Path path, @Nonnull List<String> names) {
        return new RarArchiveSourceSpec(path.toAbsolutePath().normalize(), ImmutableList.copyOf(names));
    }

    // Stateful part
    private transient Archive archive;
    private transient List<FileHeader> fileHeaders;
    private transient Iterator<String> namesIterator;

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
            if (fileHeaders == null) {
                fileHeaders = archive.getFileHeaders();
            }
            if (namesIterator == null) {
                namesIterator = names.iterator();
            }
            if (namesIterator.hasNext()) {
                String name = namesIterator.next();
                return getValidFileHeader(name);
            }
        }
        return null;
    }

    @Nonnull
    private RarArchiveSourceInternalSpec getValidFileHeader(String name) throws IOException {
        FileHeader fileHeader = null;
        for (FileHeader fh : fileHeaders) {
            if (name.equals(ArchiveUtils.normalizePath(fh.getFileName()))) {
                fileHeader = fh;
                break;
            }
        }
        if (fileHeader != null) {
            if (isFile(fileHeader)) {
                return new RarArchiveSourceInternalSpec(this, archive, fileHeader);
            } else {
                throw new InvalidArchiveEntryException(path, name);
            }
        } else {
            throw new ArchiveEntryNotFoundException(path, name);
        }
    }

    private boolean isFile(FileHeader fileHeader) {
        return fileHeader.isFileHeader() && !fileHeader.isDirectory();
    }

    @Override
    public void close() throws IOException {
        fileHeaders = null;
        namesIterator = null;
        if (archive != null) {
            archive.close();
        }
    }
}
