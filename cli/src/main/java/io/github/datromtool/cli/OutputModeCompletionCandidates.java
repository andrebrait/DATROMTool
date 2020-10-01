package io.github.datromtool.cli;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class OutputModeCompletionCandidates implements Iterable<String> {

    @Override
    @Nonnull
    public Iterator<String> iterator() {
        return getCandidatesStream().iterator();
    }

    public Stream<String> getCandidatesStream() {
        return Arrays.stream(OutputMode.values())
                .map(Enum::name)
                .map(String::toLowerCase);
    }

    @Override
    public String toString() {
        return getCandidatesStream().collect(Collectors.toList()).toString();
    }
}
