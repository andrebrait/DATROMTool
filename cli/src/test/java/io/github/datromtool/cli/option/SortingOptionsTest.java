package io.github.datromtool.cli.option;

import io.github.datromtool.data.SortingPreference;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortingOptionsTest {

    @CommandLine.Command
    private static final class Holder {

        @CommandLine.ArgGroup(exclusive = false)
        SortingOptions sortingOptions = new SortingOptions();
    }

    private static SortingPreference parse(String... args) {
        Holder holder = new Holder();
        new CommandLine(holder).parseArgs(args);
        return holder.sortingOptions.toSortingPreference();
    }

    private static Set<String> patternsOf(Set<Pattern> patterns) {
        return patterns.stream().map(Pattern::pattern).collect(Collectors.toSet());
    }

    @Test
    void avoidRegexPopulatesAvoids() {
        SortingPreference preference = parse("--avoid-regex", "Rev [AB]");
        assertEquals(
                Set.of("Rev [AB]"),
                patternsOf(preference.getAvoids()),
                "--avoid-regex expressions must land in the avoids set");
        assertTrue(
                preference.getPrefers().isEmpty(),
                "--avoid-regex must not affect prefers, but got: " + preference.getPrefers());
    }

    @Test
    void preferRegexPopulatesPrefersOnly() {
        SortingPreference preference = parse("--prefer-regex", "Virtual Console");
        assertEquals(
                Set.of("Virtual Console"),
                patternsOf(preference.getPrefers()),
                "--prefer-regex expressions must land in the prefers set");
        assertTrue(
                preference.getAvoids().isEmpty(),
                "--prefer-regex must not leak into avoids, but got: " + preference.getAvoids());
    }
}
