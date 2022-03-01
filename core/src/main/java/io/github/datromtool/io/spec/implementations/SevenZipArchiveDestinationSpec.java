package io.github.datromtool.io.spec.implementations;

import io.github.datromtool.io.spec.AbstractArchiveDestinationSpec;
import io.github.datromtool.io.spec.ArchiveDestinationInternalSpec;
import io.github.datromtool.io.spec.SourceSpec;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;

public final class SevenZipArchiveDestinationSpec extends AbstractArchiveDestinationSpec {

    // Stateful part
    private transient SevenZOutputFile sevenZOutputFile;

    public SevenZipArchiveDestinationSpec(@Nonnull Path path) {
        super(path);
    }

    @Override
    public ArchiveDestinationInternalSpec createInternalDestinationSpecFor(String name, SourceSpec sourceSpec) throws IOException {
        if (sevenZOutputFile == null) {
            sevenZOutputFile = new SevenZOutputFile(getPath().toFile());
        }
        SevenZArchiveEntry sevenZArchiveEntry = new SevenZArchiveEntry();
        sevenZArchiveEntry.setName(name);
        sevenZArchiveEntry.setSize(sourceSpec.getSize());
        sevenZArchiveEntry.setLastModifiedDate(sourceSpec.getFileTimes().getLastModifiedTimeAsDate());
        sevenZArchiveEntry.setAccessDate(sourceSpec.getFileTimes().getLastAccessTimeAsDate());
        sevenZArchiveEntry.setCreationDate(sourceSpec.getFileTimes().getCreationTimeAsDate());
        sevenZOutputFile.putArchiveEntry(sevenZArchiveEntry);
        return new SevenZipArchiveDestinationInternalSpec(this, sevenZOutputFile, sevenZArchiveEntry);
    }

    @Override
    public void close() throws IOException {
        if (sevenZOutputFile != null) {
            sevenZOutputFile.close();
            sevenZOutputFile = null;
        }
    }
}
