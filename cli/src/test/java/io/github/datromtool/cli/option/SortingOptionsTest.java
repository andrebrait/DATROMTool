package io.github.datromtool.cli.option;

import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.converter.PatternsFileConverter;
import io.github.datromtool.data.SortingPreference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortingOptionsTest {

    @CommandLine.Command
    private static final class Holder {

        @CommandLine.ArgGroup(exclusive = false)
        SortingOptions sortingOptions = new SortingOptions();
    }

    private static SortingPreference parse(String... args) {
        Holder holder = new Holder();
        CommandLine commandLine = new CommandLine(holder);
        commandLine.registerConverter(PatternsFileArgument.class, new PatternsFileConverter());
        commandLine.parseArgs(args);
        return holder.sortingOptions.toSortingPreference();
    }

    private static Path fixture(String name) {
        try {
            return Paths.get(SortingOptionsTest.class
                    .getClassLoader()
                    .getResource("patterns/" + name)
                    .toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Set<String> patternsOf(Set<Pattern> patterns) {
        return patterns.stream().map(Pattern::pattern).collect(Collectors.toSet());
    }

    private static boolean anyMatches(Set<Pattern> patterns, String candidate) {
        return patterns.stream().anyMatch(p -> p.matcher(candidate).matches());
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

    // Row 16
    @Test
    void sortRegionsCaseFoldsAndPreservesCommaSplitOrder() {
        SortingPreference preference = parse("--sort-regions", "eu, us, jp");
        assertEquals(
                List.of("EU", "US", "JP"),
                List.copyOf(preference.getRegions()),
                "--sort-regions must uppercase and preserve input order, got: " + preference.getRegions());
    }

    // Row 16
    @Test
    void sortLanguagesCaseFoldsAndPreservesCommaSplitOrder() {
        SortingPreference preference = parse("--sort-languages", "FR, en, De");
        assertEquals(
                List.of("fr", "en", "de"),
                List.copyOf(preference.getLanguages()),
                "--sort-languages must lowercase and preserve input order, got: " + preference.getLanguages());
    }

    // Row 17
    @Test
    void preferAndPrefersFileMergeIntoPrefers() {
        SortingPreference preference = parse(
                "--prefer", "Virtual Console",
                "--prefers-file", fixture("fixture.json").toString());
        assertTrue(
                anyMatches(preference.getPrefers(), "Virtual Console"),
                "--prefer must merge into prefers, got: " + preference.getPrefers());
        assertTrue(
                anyMatches(preference.getPrefers(), "FileString"),
                "--prefers-file strings must merge into prefers, got: " + preference.getPrefers());
        assertTrue(
                anyMatches(preference.getPrefers(), "FilePatternXYZ"),
                "--prefers-file patterns must merge into prefers, got: " + preference.getPrefers());
    }

    // Row 17
    @Test
    void avoidAndAvoidsFileMergeIntoAvoids() {
        SortingPreference preference = parse(
                "--avoid", "Rev A",
                "--avoids-file", fixture("fixture.json").toString());
        assertTrue(
                anyMatches(preference.getAvoids(), "Rev A"),
                "--avoid must merge into avoids, got: " + preference.getAvoids());
        assertTrue(
                anyMatches(preference.getAvoids(), "FileString"),
                "--avoids-file strings must merge into avoids, got: " + preference.getAvoids());
        assertTrue(
                anyMatches(preference.getAvoids(), "FilePatternXYZ"),
                "--avoids-file patterns must merge into avoids, got: " + preference.getAvoids());
    }

    // Row 18
    static Stream<Arguments> booleanFlags() {
        return Stream.of(
                Arguments.of("--prioritize-languages", (Predicate<SortingPreference>) SortingPreference::isPrioritizeLanguages),
                Arguments.of("--early-versions", (Predicate<SortingPreference>) SortingPreference::isEarlyVersions),
                Arguments.of("--early-revisions", (Predicate<SortingPreference>) SortingPreference::isEarlyRevisions),
                Arguments.of("--early-prereleases", (Predicate<SortingPreference>) SortingPreference::isEarlyPrereleases),
                Arguments.of("--prefer-prereleases", (Predicate<SortingPreference>) SortingPreference::isPreferPrereleases),
                Arguments.of("--prefer-parents", (Predicate<SortingPreference>) SortingPreference::isPreferParents));
    }

    @ParameterizedTest
    @MethodSource("booleanFlags")
    void booleanFlagDefaultsFalseAndFlipsTrueWhenSet(String flag, Predicate<SortingPreference> getter) {
        SortingPreference defaultPreference = parse();
        assertFalse(
                getter.test(defaultPreference),
                flag + " must default to false");
        SortingPreference setPreference = parse(flag);
        assertTrue(
                getter.test(setPreference),
                flag + " must set its field to true");
    }
}
