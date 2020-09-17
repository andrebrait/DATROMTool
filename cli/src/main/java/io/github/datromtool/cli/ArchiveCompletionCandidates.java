package io.github.datromtool.cli;

import io.github.datromtool.io.ArchiveType;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;

public final class ArchiveCompletionCandidates implements Iterable<String> {

    @Override
    @Nonnull
    public Iterator<String> iterator() {
        return Arrays.stream(ArchiveType.values())
                .filter(ArchiveType::isAvailableAsOutput)
                .map(Enum::name)
                .iterator();
    }
}
