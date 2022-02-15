package io.github.datromtool.cli.util;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.argument.StringFilterArgument;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArgumentUtils {

    public static ImmutableSet<Pattern> merge(
            Collection<String> strings,
            Collection<Pattern> patterns,
            Collection<PatternsFileArgument> patternsFiles) {
        return ImmutableSet.<Pattern>builder()
                .addAll(toLiteralPatterns(strings).iterator())
                .addAll(patterns)
                .addAll(patternsFiles.stream()
                        .map(PatternsFileArgument::getStringFilter)
                        .map(StringFilterArgument::getStrings)
                        .flatMap(ArgumentUtils::toLiteralPatterns)
                        .iterator())
                .addAll(patternsFiles.stream()
                        .map(PatternsFileArgument::getStringFilter)
                        .map(StringFilterArgument::getPatterns)
                        .flatMap(Collection::stream)
                        .iterator())
                .build();
    }

    @NonNull
    private static Stream<Pattern> toLiteralPatterns(Collection<String> strings) {
        return strings.stream()
                .map(Pattern::quote)
                .map(Pattern::compile);
    }

}
