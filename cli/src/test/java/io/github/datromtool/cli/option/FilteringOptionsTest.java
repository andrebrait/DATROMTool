package io.github.datromtool.cli.option;

import io.github.datromtool.Patterns;
import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.converter.PatternsFileConverter;
import io.github.datromtool.data.Filter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link FilteringOptions#toFilter()}: how region/language include-exclude lists case-fold,
 * how the various exclude/include expression sources merge, and the allow/no-all category
 * tri-state.
 */
class FilteringOptionsTest {

    @CommandLine.Command
    private static final class Holder {

        @CommandLine.ArgGroup(exclusive = false)
        FilteringOptions filteringOptions = new FilteringOptions();
    }

    private static Filter parse(String... args) {
        Holder holder = new Holder();
        CommandLine commandLine = new CommandLine(holder);
        commandLine.registerConverter(PatternsFileArgument.class, new PatternsFileConverter());
        commandLine.parseArgs(args);
        return holder.filteringOptions.toFilter();
    }

    private static Path fixture(String name) {
        try {
            return Paths.get(FilteringOptionsTest.class
                    .getClassLoader()
                    .getResource("patterns/" + name)
                    .toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean anyMatches(Set<Pattern> patterns, String candidate) {
        return patterns.stream().anyMatch(p -> p.matcher(candidate).matches());
    }

    // Row 1
    @Test
    void includeRegionsTrimsAndUppercasesCommaSeparatedList() {
        Filter filter = parse("--include-regions", "us, eu");
        assertEquals(
                Set.of("US", "EU"),
                filter.getIncludeRegions(),
                "--include-regions must trim, uppercase, and comma-split, got: " + filter.getIncludeRegions());
    }

    // Row 2
    @Test
    void excludeRegionsTrimsAndUppercasesCommaSeparatedList() {
        Filter filter = parse("--exclude-regions", "us, eu");
        assertEquals(
                Set.of("US", "EU"),
                filter.getExcludeRegions(),
                "--exclude-regions must trim, uppercase, and comma-split, got: " + filter.getExcludeRegions());
    }

    // Row 3
    @Test
    void includeLanguagesTrimsAndLowercasesCommaSeparatedList() {
        Filter filter = parse("--include-languages", "EN, Pt");
        assertEquals(
                Set.of("en", "pt"),
                filter.getIncludeLanguages(),
                "--include-languages must trim, lowercase, and comma-split, got: " + filter.getIncludeLanguages());
    }

    // Row 4
    @Test
    void excludeLanguagesTrimsAndLowercasesCommaSeparatedList() {
        Filter filter = parse("--exclude-languages", "EN, Pt");
        assertEquals(
                Set.of("en", "pt"),
                filter.getExcludeLanguages(),
                "--exclude-languages must trim, lowercase, and comma-split, got: " + filter.getExcludeLanguages());
    }

    // Row 5
    @Test
    void excludeLiteralIsQuotedNotInterpretedAsRegex() {
        Filter filter = parse("--exclude", "a(b");
        assertTrue(
                anyMatches(filter.getExcludes(), "a(b"),
                "--exclude 'a(b' must match the literal text 'a(b', got: " + filter.getExcludes());
        assertFalse(
                anyMatches(filter.getExcludes(), "ab"),
                "--exclude 'a(b' must not be interpreted as a regex (would match 'ab' if unquoted)");
    }

    // Row 6
    @Test
    void excludeRegexCompilesAsRegularExpression() {
        Filter filter = parse("--exclude-regex", "a.b");
        assertTrue(
                anyMatches(filter.getExcludes(), "aXb"),
                "--exclude-regex 'a.b' must match as a regular expression, got: " + filter.getExcludes());
    }

    // Row 7
    @Test
    void excludesFileMergesIntoExcludes() {
        Filter filter = parse("--excludes-file", fixture("fixture.json").toString());
        assertTrue(
                anyMatches(filter.getExcludes(), "FileString"),
                "--excludes-file strings must merge into Filter.excludes, got: " + filter.getExcludes());
        assertTrue(
                anyMatches(filter.getExcludes(), "FilePatternXYZ"),
                "--excludes-file patterns must merge into Filter.excludes, got: " + filter.getExcludes());
    }

    // Row 8
    @Test
    void includeMergesLiteralRegexAndFileSources() {
        Filter filter = parse(
                "--include", "a(b",
                "--include-regex", "c.d",
                "--includes-file", fixture("fixture.json").toString());
        assertTrue(
                anyMatches(filter.getIncludes(), "a(b"),
                "--include literal must merge into Filter.includes, got: " + filter.getIncludes());
        assertTrue(
                anyMatches(filter.getIncludes(), "cXd"),
                "--include-regex must merge into Filter.includes, got: " + filter.getIncludes());
        assertTrue(
                anyMatches(filter.getIncludes(), "FileString"),
                "--includes-file strings must merge into Filter.includes, got: " + filter.getIncludes());
        assertTrue(
                anyMatches(filter.getIncludes(), "FilePatternXYZ"),
                "--includes-file patterns must merge into Filter.includes, got: " + filter.getIncludes());
    }

    // Row 9
    @Test
    void noFlagsAllowEveryCategoryAndExcludeNothing() {
        Filter filter = parse();
        assertTrue(filter.isAllowProto(), "default allowProto must be true");
        assertTrue(filter.isAllowBeta(), "default allowBeta must be true");
        assertTrue(filter.isAllowDemo(), "default allowDemo must be true");
        assertTrue(filter.isAllowSample(), "default allowSample must be true");
        assertTrue(filter.isAllowBios(), "default allowBios must be true");
        for (Pattern p : new Pattern[]{
                Patterns.BAD, Patterns.PROGRAM, Patterns.ENHANCEMENT_CHIP, Patterns.PIRATE,
                Patterns.PROMO, Patterns.UNLICENSED, Patterns.DLC, Patterns.UPDATE}) {
            assertFalse(
                    filter.getExcludes().contains(p),
                    "default excludes must not contain " + p.pattern() + ", got: " + filter.getExcludes());
        }
    }

    // Row 10
    static Stream<Arguments> categoryBooleanFlags() {
        return Stream.of(
                Arguments.of("proto", (Function<Filter, Boolean>) Filter::isAllowProto),
                Arguments.of("beta", (Function<Filter, Boolean>) Filter::isAllowBeta),
                Arguments.of("demo", (Function<Filter, Boolean>) Filter::isAllowDemo),
                Arguments.of("sample", (Function<Filter, Boolean>) Filter::isAllowSample),
                Arguments.of("bios", (Function<Filter, Boolean>) Filter::isAllowBios));
    }

    @ParameterizedTest
    @MethodSource("categoryBooleanFlags")
    void noFlagClearsCorrespondingAllowBoolean(String flag, Function<Filter, Boolean> getter) {
        Filter filter = parse("--no-" + flag);
        assertFalse(
                getter.apply(filter),
                "--no-" + flag + " must clear Filter.allow" + flag);
    }

    @ParameterizedTest
    @MethodSource("categoryBooleanFlags")
    void explicitNoFlagUnderNoAllStaysCleared(String flag, Function<Filter, Boolean> getter) {
        Filter filter = parse("--no-all", "--no-" + flag);
        assertFalse(
                getter.apply(filter),
                "--no-all --no-" + flag + " must keep Filter.allow" + flag + " cleared");
    }

    // Row 11
    static Stream<Arguments> categoryPatternFlags() {
        return Stream.of(
                Arguments.of("bad", Patterns.BAD),
                Arguments.of("program", Patterns.PROGRAM),
                Arguments.of("chip", Patterns.ENHANCEMENT_CHIP),
                Arguments.of("pirate", Patterns.PIRATE),
                Arguments.of("promo", Patterns.PROMO),
                Arguments.of("unlicensed", Patterns.UNLICENSED),
                Arguments.of("dlc", Patterns.DLC),
                Arguments.of("update", Patterns.UPDATE));
    }

    @ParameterizedTest
    @MethodSource("categoryPatternFlags")
    void noFlagAddsCorrespondingPatternsConstantToExcludes(String flag, Pattern expected) {
        Filter filter = parse("--no-" + flag);
        assertTrue(
                filter.getExcludes().contains(expected),
                "--no-" + flag + " must add Patterns." + expected.pattern()
                        + " to Filter.excludes, got: " + filter.getExcludes());
    }

    // Row 12
    @Test
    void noAllAloneDisablesAllCategoriesAndExcludesAllPatterns() {
        Filter filter = parse("--no-all");
        assertFalse(filter.isAllowProto(), "--no-all must clear allowProto");
        assertFalse(filter.isAllowBeta(), "--no-all must clear allowBeta");
        assertFalse(filter.isAllowDemo(), "--no-all must clear allowDemo");
        assertFalse(filter.isAllowSample(), "--no-all must clear allowSample");
        assertFalse(filter.isAllowBios(), "--no-all must clear allowBios");
        for (Pattern p : new Pattern[]{
                Patterns.BAD, Patterns.PROGRAM, Patterns.ENHANCEMENT_CHIP, Patterns.PIRATE,
                Patterns.PROMO, Patterns.UNLICENSED, Patterns.DLC, Patterns.UPDATE}) {
            assertTrue(
                    filter.getExcludes().contains(p),
                    "--no-all must add " + p.pattern() + " to excludes, got: " + filter.getExcludes());
        }
    }

    // Row 13
    @Test
    void noAllWithExplicitProtoReAllowsOnlyProto() {
        Filter filter = parse("--no-all", "--proto");
        assertTrue(filter.isAllowProto(), "--no-all --proto must re-allow proto");
        assertFalse(filter.isAllowBeta(), "--no-all --proto must still clear beta");
        assertFalse(filter.isAllowDemo(), "--no-all --proto must still clear demo");
        assertFalse(filter.isAllowSample(), "--no-all --proto must still clear sample");
        assertFalse(filter.isAllowBios(), "--no-all --proto must still clear bios");
    }

    // Row 14
    @Test
    void noAllWithExplicitBadReAllowsOnlyBadPattern() {
        Filter filter = parse("--no-all", "--bad");
        assertFalse(
                filter.getExcludes().contains(Patterns.BAD),
                "--no-all --bad must NOT exclude Patterns.BAD, got: " + filter.getExcludes());
        for (Pattern p : new Pattern[]{
                Patterns.PROGRAM, Patterns.ENHANCEMENT_CHIP, Patterns.PIRATE,
                Patterns.PROMO, Patterns.UNLICENSED, Patterns.DLC, Patterns.UPDATE}) {
            assertTrue(
                    filter.getExcludes().contains(p),
                    "--no-all --bad must still exclude " + p.pattern() + ", got: " + filter.getExcludes());
        }
    }

    // Hostile row H1
    @Test
    void invalidExcludeRegexFailsParsingInsteadOfCrashing() {
        assertThrows(
                CommandLine.ParameterException.class,
                () -> parse("--exclude-regex", "(["),
                "an invalid --exclude-regex must be reported as a picocli ParameterException");
    }

    // Hostile row H2 (pinning actual behavior)
    @Test
    void emptyIncludeRegionsValueYieldsSingleEmptyStringEntry() {
        Filter filter = parse("--include-regions", "");
        // Unlike a commas-only value (H3), a bare empty value has no comma for picocli's
        // split-respecting-quoted-strings routine to match against, so it keeps the single
        // (empty) raw token instead of collapsing it away.
        assertEquals(
                Set.of(""),
                filter.getIncludeRegions(),
                "an empty --include-regions value must survive as a single empty-string region, got: "
                        + filter.getIncludeRegions());
    }

    // Hostile row H3 (pinning actual behavior)
    @Test
    void commasOnlyIncludeRegionsValueContributesNoEntries() {
        Filter filter = parse("--include-regions", ",,,");
        assertTrue(
                filter.getIncludeRegions().isEmpty(),
                "a commas-only --include-regions value must split into zero tokens (picocli strips "
                        + "trailing empty tokens), got: " + filter.getIncludeRegions());
    }
}
