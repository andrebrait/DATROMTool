package io.github.datromtool.cli;

import io.github.datromtool.io.ArchiveType;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class ArchiveCompletionCandidates implements Iterable<String> {

    @Override
    @Nonnull
    public Iterator<String> iterator() {
        return Arrays.stream(ArchiveType.values())
                .filter(ArchiveType::isAvailableAsOutput)
                .flatMap(e -> e.getAliases().isEmpty()
                        ? Stream.of(e.name())
                        : e.getAliases().stream())
                .iterator();
    }

    @Override
    public String toString() {
        return StreamSupport.stream(this.spliterator(), false)
                .collect(Collectors.toList())
                .toString();
    }
}
