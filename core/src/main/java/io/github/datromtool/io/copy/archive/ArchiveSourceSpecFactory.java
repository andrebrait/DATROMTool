package io.github.datromtool.io.copy.archive;

import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.nio.file.Path;

@RequiredArgsConstructor
public final class ArchiveSourceSpecFactory {

    private final boolean forceSevenZip;
    private final boolean forceUnrar;

    @Nonnull
    public ArchiveSourceSpec buildSourceSpec(@Nonnull Path path) {
        return null;
    }
}
