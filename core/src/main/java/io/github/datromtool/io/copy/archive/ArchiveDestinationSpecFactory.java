package io.github.datromtool.io.copy.archive;

import io.github.datromtool.io.ArchiveType;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.nio.file.Path;

@RequiredArgsConstructor
public final class ArchiveDestinationSpecFactory {

    @Nonnull
    public ArchiveDestinationSpec buildSourceSpec(@Nonnull Path path, @Nonnull ArchiveType archiveType) {
        return null;
    }
}
