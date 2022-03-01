package io.github.datromtool.io.copy.archive.impl;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import io.github.datromtool.io.copy.archive.AbstractArchiveSourceSpec;
import io.github.datromtool.io.copy.archive.ArchiveSourceInternalSpec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;

import static java.lang.String.format;

public final class RarArchiveSourceSpec extends AbstractArchiveSourceSpec {

    // Stateful part
    private transient Archive archive;

    public RarArchiveSourceSpec(@Nonnull Path path) {
        super(path);
    }

    public RarArchiveSourceSpec(@Nonnull Path path, @Nonnull Iterable<String> names) {
        super(path, names);
    }

    @Override
    protected void initArchive() throws IOException {
        if (archive == null) {
            try {
                archive = new Archive(getPath().toFile());
            } catch (RarException e) {
                throw new IOException(format("Could not open '%s'", getPath()), e);
            }
        }
    }

    @Nullable
    @Override
    protected ArchiveSourceInternalSpec getNextEntry() {
        FileHeader fileHeader;
        while ((fileHeader = archive.nextFileHeader()) != null) {
            if (isFile(fileHeader)) {
                return new RarArchiveSourceInternalSpec(this, archive, fileHeader);
            }
        }
        return null;
    }

    private static boolean isFile(FileHeader fileHeader) {
        return fileHeader.isFileHeader() && !fileHeader.isDirectory();
    }

    @Override
    protected void closeArchive() throws IOException {
        if (archive != null) {
            archive.close();
            archive = null;
        }
    }
}
