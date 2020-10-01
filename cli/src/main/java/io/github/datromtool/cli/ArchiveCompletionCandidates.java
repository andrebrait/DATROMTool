package io.github.datromtool.cli;

import io.github.datromtool.io.ArchiveType;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ArchiveCompletionCandidates implements Iterable<String> {

    @Override
    @Nonnull
    public Iterator<String> iterator() {
        return getCandidatesStream().iterator();
    }

    public Stream<String> getCandidatesStream() {
        return Arrays.stream(ArchiveType.values())
                .filter(ArchiveType::isAvailableAsOutput)
                .flatMap(e -> e.getAliases().isEmpty()
                        ? Stream.of(e.name())
                        : e.getAliases().stream());
    }

    @Override
    public String toString() {
        return getCandidatesStream().collect(Collectors.toList()).toString();
    }
}
