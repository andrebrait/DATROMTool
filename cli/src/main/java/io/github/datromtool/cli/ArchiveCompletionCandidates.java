package io.github.datromtool.cli;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.io.ArchiveType;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;

public final class ArchiveCompletionCandidates implements Iterable<String> {

    private static final ImmutableList<String> OUTPUT_TYPES =
            Arrays.stream(ArchiveType.values())
                    .filter(ArchiveType::isAvailableAsOutput)
                    .map(Enum::name)
                    .collect(ImmutableList.toImmutableList());

    @Override
    @Nonnull
    public Iterator<String> iterator() {
        return OUTPUT_TYPES.iterator();
    }
}
