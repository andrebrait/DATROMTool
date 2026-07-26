package io.github.datromtool.cli.util;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.argument.StringFilterArgument;
import io.github.datromtool.data.NameMatcher;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArgumentUtils {

    public static ImmutableSet<NameMatcher> merge(
            Collection<String> strings,
            Collection<Pattern> patterns,
            Collection<PatternsFileArgument> patternsFiles) {
        return ImmutableSet.<NameMatcher>builder()
                .addAll(toLiteralMatchers(strings).iterator())
                .addAll(toRegexMatchers(patterns).iterator())
                .addAll(patternsFiles.stream()
                        .map(PatternsFileArgument::getStringFilter)
                        .map(StringFilterArgument::strings)
                        .flatMap(ArgumentUtils::toLiteralMatchers)
                        .iterator())
                .addAll(patternsFiles.stream()
                        .map(PatternsFileArgument::getStringFilter)
                        .map(StringFilterArgument::patterns)
                        .flatMap(ArgumentUtils::toRegexMatchers)
                        .iterator())
                .build();
    }

    @Nonnull
    private static Stream<NameMatcher> toLiteralMatchers(Collection<String> strings) {
        return strings.stream()
                .map(NameMatcher::literal);
    }

    @Nonnull
    private static Stream<NameMatcher> toRegexMatchers(Collection<Pattern> patterns) {
        return patterns.stream()
                .map(Pattern::pattern)
                .map(NameMatcher::regex);
    }

}
