package io.github.datromtool.cli.option;

import io.github.datromtool.Patterns;
import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.converter.GameCategoryConverter;
import io.github.datromtool.cli.converter.PatternsFileConverter;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.GameCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import picocli.CommandLine;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link FilteringOptions#toFilter()}: how region/language include-exclude lists case-fold,
 * how the various exclude/include expression sources merge, and how {@code --exclude-categories}
 * maps onto {@link Filter#getExcludeCategories()}.
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
        commandLine.registerConverter(GameCategory.class, new GameCategoryConverter());
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

    // Row 9 (new-surface equivalent of the old rows 9/12: nothing excluded by default)
    @Test
    void noFlagsExcludeNoCategoryAndExcludeNothing() {
        Filter filter = parse();
        assertTrue(
                filter.getExcludeCategories().isEmpty(),
                "default excludeCategories must be empty, got: " + filter.getExcludeCategories());
        for (Pattern p : new Pattern[]{
                Patterns.BAD, Patterns.PROGRAM, Patterns.ENHANCEMENT_CHIP, Patterns.PIRATE,
                Patterns.PROMO, Patterns.UNLICENSED, Patterns.DLC, Patterns.UPDATE}) {
            assertFalse(
                    filter.getExcludes().contains(p),
                    "default excludes must not contain " + p.pattern() + ", got: " + filter.getExcludes());
        }
    }

    // Row 1 of the coverage matrix: --exclude-categories parses to Filter.excludeCategories,
    // case-insensitively.
    @Test
    void excludeCategoriesParsesCommaSeparatedListIntoFilter() {
        Filter filter = parse("--exclude-categories", "proto,beta");
        assertEquals(
                Set.of(GameCategory.PROTO, GameCategory.BETA),
                filter.getExcludeCategories(),
                "--exclude-categories proto,beta must produce {PROTO, BETA}, got: "
                        + filter.getExcludeCategories());
    }

    @ParameterizedTest
    @EnumSource(GameCategory.class)
    void excludeCategoriesAcceptsEachCategoryCaseInsensitively(GameCategory category) {
        Filter lower = parse("--exclude-categories", category.name().toLowerCase());
        assertEquals(
                Set.of(category),
                lower.getExcludeCategories(),
                "lowercase category name must be accepted, got: " + lower.getExcludeCategories());

        Filter mixed = parse("--exclude-categories",
                category.name().charAt(0)
                        + category.name().substring(1).toLowerCase());
        assertEquals(
                Set.of(category),
                mixed.getExcludeCategories(),
                "mixed-case category name must be accepted, got: " + mixed.getExcludeCategories());
    }

    // Row 2: a bogus category value must fail parsing with a clear message.
    @Test
    void bogusCategoryValueFailsParsingWithValidValuesListed() {
        CommandLine.ParameterException e = assertThrows(
                CommandLine.ParameterException.class,
                () -> parse("--exclude-categories", "not-a-category"),
                "an invalid --exclude-categories value must be reported as a ParameterException");
        assertTrue(
                e.getMessage().contains("not-a-category"),
                "error message must mention the bogus value, got: " + e.getMessage());
        for (GameCategory category : GameCategory.values()) {
            assertTrue(
                    e.getMessage().contains(category.name()),
                    "error message must list valid category " + category + ", got: " + e.getMessage());
        }
    }

    // Row 6: Filter.excludes no longer receives Patterns.* injected by the CLI; only
    // --exclude-categories carries category information.
    @Test
    void excludeCategoriesDoesNotPopulateExcludesPatterns() {
        Filter filter = parse("--exclude-categories", "bad");
        assertTrue(
                filter.getExcludes().isEmpty(),
                "--exclude-categories bad must not populate Filter.excludes, got: " + filter.getExcludes());
        assertEquals(
                Set.of(GameCategory.BAD),
                filter.getExcludeCategories(),
                "--exclude-categories bad must produce {BAD}, got: " + filter.getExcludeCategories());
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
