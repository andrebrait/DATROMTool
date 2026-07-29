package io.github.datromtool.cli.option;

import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.converter.PatternsFileConverter;
import io.github.datromtool.data.NameMatcher;
import io.github.datromtool.data.OrderPreference;
import io.github.datromtool.data.SortingPreference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortingOptionsTest {

    // The sorting options case-fold through the same converters as the filtering ones, so a
    // blank value must be rejected there too rather than becoming an empty preference entry.
    @ParameterizedTest
    @ValueSource(strings = {"--sort-regions", "--sort-languages"})
    void blankRegionOrLanguageValueIsRejected(String option) {
        assertThrows(
                CommandLine.ParameterException.class,
                () -> parse(option, ""),
                option + " with an empty value must be rejected, not read as a literal empty code");
    }


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

    private static Set<String> patternsOf(Set<NameMatcher> matchers) {
        return matchers.stream()
                .map(NameMatcher::getPattern)
                .map(Pattern::pattern)
                .collect(Collectors.toSet());
    }

    private static boolean anyMatches(Set<NameMatcher> matchers, String candidate) {
        return matchers.stream()
                .map(NameMatcher::getPattern)
                .anyMatch(p -> p.matcher(candidate).matches());
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

    // Matrix row 1: --versions/--revisions/--prereleases parse to the order enum, default LATEST.
    static Stream<Arguments> orderOptions() {
        return Stream.of(
                Arguments.of("--versions", (Function<SortingPreference, OrderPreference>) SortingPreference::getVersions),
                Arguments.of("--revisions", (Function<SortingPreference, OrderPreference>) SortingPreference::getRevisions),
                Arguments.of("--prereleases", (Function<SortingPreference, OrderPreference>) SortingPreference::getPrereleases));
    }

    @ParameterizedTest
    @MethodSource("orderOptions")
    void orderOptionDefaultsLatestAndSetsEarliestWhenRequested(
            String option, Function<SortingPreference, OrderPreference> getter) {
        SortingPreference defaultPreference = parse();
        assertEquals(
                OrderPreference.LATEST,
                getter.apply(defaultPreference),
                option + " must default to LATEST");
        SortingPreference earliestPreference = parse(option, "earliest");
        assertEquals(
                OrderPreference.EARLIEST,
                getter.apply(earliestPreference),
                option + " earliest must set its field to EARLIEST");
    }

    // Matrix row 2: case-insensitive values are accepted.
    @ParameterizedTest
    @ValueSource(strings = {"EARLIEST", "Latest", "latest", "earliest"})
    void orderOptionAcceptsValueCaseInsensitively(String value) {
        SortingPreference preference = parse("--versions", value);
        assertEquals(
                OrderPreference.valueOf(value.toUpperCase(Locale.ROOT)),
                preference.getVersions(),
                "--versions " + value + " must be accepted case-insensitively");
    }

    // Matrix row 2: outer whitespace on the raw value must be trimmed by the converter.
    @Test
    void orderOptionAcceptsOuterWhitespace() {
        SortingPreference preference = parse("--versions", " earliest ");
        assertEquals(
                OrderPreference.EARLIEST,
                preference.getVersions(),
                "--versions ' earliest ' must be trimmed and accepted");
    }

    // Matrix row 2: a bogus order value must fail parsing with a clear message.
    @Test
    void bogusOrderValueFailsParsingWithValidValuesListed() {
        CommandLine.ParameterException e = assertThrows(
                CommandLine.ParameterException.class,
                () -> parse("--versions", "sideways"),
                "an invalid --versions value must be reported as a ParameterException");
        assertTrue(
                e.getMessage().contains("sideways"),
                "error message must mention the bogus value, got: " + e.getMessage());
        assertTrue(
                e.getMessage().contains("latest") && e.getMessage().contains("earliest"),
                "error message must list valid values latest/earliest, got: " + e.getMessage());
    }
}
