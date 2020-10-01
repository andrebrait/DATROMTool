package io.github.datromtool.cli.util;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.cli.argument.PatternsFileArgument;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArgumentUtils {

    public static ImmutableSet<Pattern> merge(
            Collection<Pattern> patterns,
            Collection<PatternsFileArgument> patternsFiles) {
        return ImmutableSet.<Pattern>builder()
                .addAll(patterns)
                .addAll(patternsFiles.stream()
                        .map(PatternsFileArgument::getPatterns)
                        .flatMap(Collection::stream)
                        .iterator())
                .build();
    }

}
