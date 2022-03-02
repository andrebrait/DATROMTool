package io.github.datromtool.io.copy.archive;

import io.github.datromtool.io.ArchiveType;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.nio.file.Path;

@RequiredArgsConstructor
public final class ArchiveDestinationSpecFactory {

    private final boolean forceSevenZip;
    private final boolean forceUnrar;

    @Nonnull
    public ArchiveDestinationSpec buildSourceSpec(@Nonnull Path path, @Nonnull ArchiveType archiveType) {
        return null;
    }
}
