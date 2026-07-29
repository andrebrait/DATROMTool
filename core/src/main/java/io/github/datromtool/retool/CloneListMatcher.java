package io.github.datromtool.retool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.Patterns;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.domain.retool.CloneList;
import io.github.datromtool.domain.retool.NameType;
import io.github.datromtool.domain.retool.VariantFilter;
import io.github.datromtool.domain.retool.VariantGroup;
import io.github.datromtool.domain.retool.VariantTitle;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matches a {@link ParsedGame}'s DAT name against a Retool {@link CloneList}'s {@code titles}
 * (issue #19 step 2), producing a group assignment and effective priority. {@code supersets} and
 * {@code compilations} are deliberately out of scope for this step (parsed since step 1, not
 * acted on here) - see the issue's step 2 scope comment for why (supersets interact with 1G1R
 * selection order and deserve a focused change; compilations' {@code titlePosition}/optimization-
 * mode mechanics are the most ambiguous part of the upstream spec).
 *
 * <h2>Matching semantics per {@link NameType} (upstream:
 * <a href="https://unexpectedpanda.github.io/retool/contribute-clone-lists-variants-titles/">
 * contribute-clone-lists-variants-titles</a> and
 * <a href="https://unexpectedpanda.github.io/retool/naming-system/">naming-system</a>)</h2>
 * <ul>
 *   <li>{@link NameType#FULL} - exact, case-sensitive equality against the game's full DAT
 *       name.</li>
 *   <li>{@link NameType#REGEX} - {@code searchTerm} compiled as a {@link Pattern}, matched with
 *       {@link Matcher#matches()} (whole-string) against the full name. Upstream's docs describe
 *       {@code regex} only as letting Retool "match [the search term] accurately against names in
 *       the input DAT file" - they do not state find-vs-matches semantics (checked against the
 *       live titles and filters doc pages before implementing). Whole-string {@code matches()} is
 *       chosen for parity with {@link NameType#FULL}/{@link NameType#REGION_FREE} being exact
 *       comparisons rather than substring searches; a clone list author who wants a substring
 *       match can always wrap their pattern in {@code .*...*}. This is a documented
 *       implementation choice, not a spec quote.</li>
 *   <li>{@link NameType#REGION_FREE} - "the same as full names, except their regions and
 *       languages have been removed" (naming-system doc, verbatim). Reconstructed here by
 *       reusing this codebase's own region/language detection ({@link Patterns#SECTIONS} to find
 *       parenthetical groups, {@link RegionData}'s per-region patterns, and a local language-tag
 *       pattern mirroring {@link Patterns#LANGUAGES}'s inner shape) to strip only groups that are
 *       entirely region or language tokens - unlike {@link NameType#SHORT}, version/revision/disc
 *       tags are preserved. Compared with exact, case-sensitive equality against the search term
 *       (same rationale as {@code FULL}).</li>
 *   <li>{@link NameType#SHORT} (default) - upstream's short-name algorithm (naming-system doc,
 *       verbatim): "1. Normalize disc designations... 2. Remove tags and version strings... 3.
 *       Remove regions and languages... 4. Convert to lowercase". Upstream's own worked example
 *       (naming-system doc, verbatim) takes the full names {@code "This is a title (USA) (En,Fr)
 *       (Disc A) (Best Collection)"}, {@code "This is a title (Canada) (Disc 1)"}, {@code "This
 *       is a title (Europe) (De,It) (Disc A)"}, and {@code "This is a title V3 (Spain) (Disco
 *       Uno)"} to the single short name {@code "this is a title (disc 1)"} - note that upstream
 *       <b>retains</b> a normalized disc designator ({@code "(disc 1)"}) in the short name; only
 *       region, language, version, and edition tags are stripped. Steps 1-2 (disc-designation
 *       normalization and upstream's configured tag/version-string dictionary) are approximated
 *       here by stripping <em>every</em> parenthetical group, including disc designators - a
 *       deliberate, <b>documented divergence</b> from upstream (which normalizes and keeps them),
 *       not a curated port of upstream's tag dictionary or disc-normalization table. This is a
 *       faithful approximation for typical No-Intro/Redump names as far as <em>matching two names
 *       against each other</em> goes (both sides of a comparison go through the identical
 *       over-strip, so a shared disc designator does not break the match), but would diverge from
 *       upstream on a name whose title itself legitimately contains a trailing parenthetical that
 *       upstream's dictionary would not have stripped, or on any clone list authored assuming disc
 *       designators survive into the short name. Both sides of the comparison (the full game name
 *       and the title's {@code searchTerm}) go through the same transform, then compare
 *       case-insensitively.</li>
 * </ul>
 *
 * <h2>Conditional filters (upstream:
 * <a href="https://unexpectedpanda.github.io/retool/contribute-clone-lists-variants-filters/">
 * contribute-clone-lists-variants-filters</a>)</h2>
 * A matched title's {@code filters} are evaluated in array order; the <b>first</b> filter whose
 * {@link VariantFilter.Conditions} all match (conditions within one filter are ANDed - see
 * {@link VariantFilter}'s Javadoc) has its {@link VariantFilter.Results} applied, and evaluation
 * stops there. Upstream's filters doc states filters are "an array, you can add as many
 * conditions and results pairs as you like" but does not specify first-match-wins vs. all-
 * matches-apply semantics (checked against the live doc page). First-match-wins is chosen as the
 * more common convention for ordered rule lists (and is what this codebase's own
 * {@code GameComparator}/{@code SubComparator} chain does) - a documented implementation choice,
 * not a spec quote.
 *
 * <p>{@code matchLanguages} ("two-letter abbreviations in title case", e.g. {@code "Fr"}) and
 * {@code matchRegions} ("full names in title case", e.g. {@code "Europe"}) test the <em>matched
 * game's own</em> parsed languages/regions - compared case-insensitively for languages, and via
 * {@link RegionData}'s pattern-to-code mapping for regions (clone lists use human region names
 * like {@code "Europe"}; this codebase's {@link SortingPreference#getRegions()}/
 * {@link ParsedGame#getRegionsStream()} use internal codes like {@code "EUR"} - see
 * {@code region-data.yaml}). {@code matchString} is a regex tested against the full name with
 * {@link Matcher#matches()} (same whole-string choice, and same lack of a spec statement, as the
 * {@link NameType#REGEX} nameType above).
 *
 * <p>{@code regionOrder}: "If any of the regions in the {@code higherRegions} array is higher in
 * the user region order than all of the regions in the {@code lowerRegions} array, then the
 * condition is true" (verbatim). "Higher" means earlier in
 * {@link SortingPreference#getRegions()} (an insertion-ordered {@link ImmutableSet}, most-
 * preferred first - the same convention {@code RegionSubComparator} relies on); a region absent
 * from the user's order is treated as ranking after every region that <em>is</em> in it (again
 * mirroring {@code RegionSubComparator}'s existing fallback). The literal placeholder
 * {@code "All other regions"} may replace either array: "the remaining regions will be calculated
 * automatically based on the array you've already populated" (verbatim) - implemented here as
 * (the user's own region order) minus (whichever regions are explicitly named in either array),
 * since the condition is inherently scoped to regions the user actually ordered; a region entirely
 * outside the user's order cannot be meaningfully "higher" or "lower" in it. This universe choice
 * (user's order vs. every region in {@code region-data.yaml}) is a documented implementation
 * choice, not a spec quote.
 */
public final class CloneListMatcher {

    private static final String ALL_OTHER_REGIONS = "All other regions";

    /**
     * How much of a game's name one clone list pattern may read before it is given up on. A
     * clone list is community data a user points {@code --clonelist} at, and {@code
     * java.util.regex} has no match timeout, so a catastrophically backtracking pattern would
     * otherwise monopolize the run's thread (CWE-1333). Legitimate patterns read a small multiple
     * of the name's length; this leaves three orders of magnitude of headroom.
     */
    private static final int MAX_PATTERN_CHARACTER_READS = 100_000;

    private static final Pattern PAREN_GROUP = Pattern.compile("\\([^()]+\\)");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern LANGUAGE_GROUP =
            Pattern.compile("[a-z]{2}(?:[,+][a-z]{2})*", Pattern.CASE_INSENSITIVE);

    private final CloneList cloneList;
    private final RegionData regionData;
    private final ImmutableList<String> userRegionOrder;

    public CloneListMatcher(
            @Nonnull CloneList cloneList,
            @Nonnull RegionData regionData,
            @Nonnull SortingPreference sortingPreference) {
        this.cloneList = cloneList;
        this.regionData = regionData;
        this.userRegionOrder = ImmutableList.copyOf(sortingPreference.getRegions());
    }

    /**
     * A clone list match's outcome: the group key {@link io.github.datromtool.GameSorter} should
     * use in place of DAT parent/clone grouping, and the effective priority
     * {@code PrioritySubComparator} should compare by (both after any matching
     * {@link VariantFilter.Results} override has been applied).
     */
    public record MatchResult(@Nonnull String group, int priority, boolean ignored) {
    }

    /**
     * Thrown when a clone list pattern reads more of a game's name than
     * {@link #MAX_PATTERN_CHARACTER_READS} allows, which means it is backtracking
     * catastrophically (CWE-1333).
     */
    public static final class PatternBudgetExceededException extends RuntimeException {

        PatternBudgetExceededException(String message) {
            super(message);
        }
    }

    /**
     * @return the match for {@code game}, or {@link Optional#empty()} if no variant title in the
     * clone list's {@code titles} (see class Javadoc for why {@code supersets}/
     * {@code compilations} are excluded) matched its full name. Groups are searched in file
     * order; a match inside a group with {@code ignore=true} comes back
     * {@link MatchResult#ignored()}, which drops the game from the run entirely (upstream:
     * {@code ignore} "force removes the title from Retool's consideration" - "ignored titles are
     * completely removed from Retool's consideration during processing"). Keeping such a title on
     * its DAT grouping instead would leave it in the 1G1R output, where it can still win its
     * group.
     */
    @Nonnull
    public Optional<MatchResult> match(@Nonnull ParsedGame game) {
        String fullName = game.getGame().getName();
        for (VariantGroup group : cloneList.variants()) {
            Optional<VariantTitle> best = bestMatch(group, fullName);
            if (best.isPresent()) {
                if (group.ignore()) {
                    return Optional.of(
                            new MatchResult(group.group(), best.get().priority(), true));
                }
                return Optional.of(applyFilters(group, best.get(), game, fullName));
            }
        }
        return Optional.empty();
    }

    /**
     * Picks the most specific matching title within {@code group} (review round: short-name
     * collapse picking the wrong title/priority). {@link NameType#SHORT} folds distinct full
     * names to the same key (e.g. this repo's own bundled Atari 2600 fixture's "Forest" group -
     * "Forest (Two Player)", "Forest (Two Player) (Muted)", "Forest (One Player)", and
     * "Forest (One Player) (Muted)" all fold to {@code "forest"}), so more than one title in a
     * group can legitimately match the same game; taking the first one in file order (the
     * pre-fix behavior) silently picks the wrong title/priority whenever the actually-intended
     * title is not first.
     *
     * <p>Specificity, most to least specific: an exact, case-sensitive literal match of
     * {@code fullName} against a title's {@code searchTerm} - this is what a {@link
     * NameType#FULL} match always is, but any other nameType can also coincidentally match
     * literally (e.g. "Forest (One Player)" against the searchTerm of the very title with that
     * name) - beats {@link NameType#REGION_FREE}, which beats {@link NameType#REGEX}, which
     * beats {@link NameType#SHORT} (fold-based, hence the least specific: it is what collapses
     * distinct names together in the first place). Ties within the same tier prefer the longer
     * {@code searchTerm} (a longer literal/pattern is narrower, hence more specific), then file
     * order, for a fully deterministic result.
     */
    private Optional<VariantTitle> bestMatch(VariantGroup group, String fullName) {
        Comparator<VariantTitle> bySpecificity = Comparator
                .comparingInt((VariantTitle t) -> specificityRank(t, fullName))
                .thenComparing(t -> t.searchTerm().length(), Comparator.reverseOrder());
        return group.titles().stream()
                .filter(t -> matchesTitle(t, fullName))
                .min(bySpecificity);
    }

    private static int specificityRank(VariantTitle title, String fullName) {
        if (fullName.equals(title.searchTerm())) {
            return 0;
        }
        return switch (title.nameType()) {
            case FULL -> 0; // unreachable in practice: a FULL match is always a literal match
            case REGION_FREE -> 1;
            case REGEX -> 2;
            case SHORT -> 3;
        };
    }

    private boolean matchesTitle(VariantTitle title, String fullName) {
        String searchTerm = title.searchTerm();
        return switch (title.nameType()) {
            case FULL -> fullName.equals(searchTerm);
            case REGEX -> matchesBounded(searchTerm, fullName);
            case REGION_FREE -> regionFreeName(fullName).equals(searchTerm);
            case SHORT -> shortName(fullName).equals(shortName(searchTerm));
        };
    }

    /**
     * Matches {@code regex} against a character source that stops the engine once it has read
     * {@link #MAX_PATTERN_CHARACTER_READS} characters — the backtracking engine reads the input
     * once per step, so the read count bounds the work regardless of how the pattern is written.
     *
     * @throws PatternBudgetExceededException naming the pattern that ran out of budget
     */
    private static boolean matchesBounded(String regex, String fullName) {
        try {
            return Pattern.compile(regex)
                    .matcher(new BudgetedCharSequence(fullName, MAX_PATTERN_CHARACTER_READS))
                    .matches();
        } catch (BudgetExceeded e) {
            throw new PatternBudgetExceededException(String.format(
                    "Clone list pattern '%s' read more than %d characters of '%s' without "
                            + "deciding: it backtracks catastrophically and was given up on",
                    regex,
                    MAX_PATTERN_CHARACTER_READS,
                    fullName));
        }
    }

    /** Internal signal, converted to a {@link PatternBudgetExceededException} by its only caller. */
    private static final class BudgetExceeded extends RuntimeException {

        BudgetExceeded() {
            super(null, null, false, false);
        }
    }

    private static final class BudgetedCharSequence implements CharSequence {

        private final CharSequence delegate;
        private int readsLeft;

        BudgetedCharSequence(CharSequence delegate, int readsLeft) {
            this.delegate = delegate;
            this.readsLeft = readsLeft;
        }

        @Override
        public char charAt(int index) {
            if (--readsLeft < 0) {
                throw new BudgetExceeded();
            }
            return delegate.charAt(index);
        }

        @Override
        public int length() {
            return delegate.length();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return delegate.subSequence(start, end);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    private MatchResult applyFilters(
            VariantGroup group,
            VariantTitle title,
            ParsedGame game,
            String fullName) {
        String effectiveGroup = group.group();
        int effectivePriority = title.priority();
        for (VariantFilter filter : title.filters()) {
            VariantFilter.Conditions conditions = filter.conditions();
            if (conditions != null && conditionsMatch(conditions, game, fullName)) {
                VariantFilter.Results results = filter.results();
                if (results != null) {
                    if (results.group() != null) {
                        effectiveGroup = results.group();
                    }
                    if (results.priority() != null) {
                        effectivePriority = results.priority();
                    }
                }
                break;
            }
        }
        return new MatchResult(effectiveGroup, effectivePriority, false);
    }

    private boolean conditionsMatch(VariantFilter.Conditions conditions, ParsedGame game, String fullName) {
        if (!conditions.matchLanguages().isEmpty() && !matchLanguages(conditions.matchLanguages(), game)) {
            return false;
        }
        if (!conditions.matchRegions().isEmpty() && !matchRegions(conditions.matchRegions(), game)) {
            return false;
        }
        if (conditions.matchString() != null
                && !matchesBounded(conditions.matchString(), fullName)) {
            return false;
        }
        return conditions.regionOrder() == null || matchRegionOrder(conditions.regionOrder());
    }

    private static boolean matchLanguages(ImmutableList<String> wanted, ParsedGame game) {
        ImmutableSet<String> wantedLower = wanted.stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(ImmutableSet.toImmutableSet());
        return game.getLanguagesStream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .anyMatch(wantedLower::contains);
    }

    private boolean matchRegions(ImmutableList<String> wanted, ParsedGame game) {
        ImmutableSet<String> wantedCodes = wanted.stream()
                .map(this::regionNameToCode)
                .flatMap(Optional::stream)
                .collect(ImmutableSet.toImmutableSet());
        return game.getRegionsStream().anyMatch(wantedCodes::contains);
    }

    private boolean matchRegionOrder(VariantFilter.RegionOrder regionOrder) {
        ImmutableSet<String> higher =
                resolveRegionCodes(regionOrder.higherRegions(), regionOrder.lowerRegions());
        ImmutableSet<String> lower =
                resolveRegionCodes(regionOrder.lowerRegions(), regionOrder.higherRegions());
        if (higher.isEmpty() || lower.isEmpty()) {
            return false;
        }
        for (String higherCode : higher) {
            int higherIndex = orderIndex(higherCode);
            boolean aboveAll = true;
            for (String lowerCode : lower) {
                if (higherIndex >= orderIndex(lowerCode)) {
                    aboveAll = false;
                    break;
                }
            }
            if (aboveAll) {
                return true;
            }
        }
        return false;
    }

    private int orderIndex(String code) {
        int index = userRegionOrder.indexOf(code);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private ImmutableSet<String> resolveRegionCodes(
            ImmutableList<String> thisArray,
            ImmutableList<String> otherArray) {
        boolean wildcard = thisArray.stream().anyMatch(r -> r.equalsIgnoreCase(ALL_OTHER_REGIONS));
        ImmutableSet<String> explicit = thisArray.stream()
                .filter(r -> !r.equalsIgnoreCase(ALL_OTHER_REGIONS))
                .map(this::regionNameToCode)
                .flatMap(Optional::stream)
                .collect(ImmutableSet.toImmutableSet());
        if (!wildcard) {
            return explicit;
        }
        ImmutableSet<String> otherExplicit = otherArray.stream()
                .filter(r -> !r.equalsIgnoreCase(ALL_OTHER_REGIONS))
                .map(this::regionNameToCode)
                .flatMap(Optional::stream)
                .collect(ImmutableSet.toImmutableSet());
        ImmutableSet.Builder<String> result = ImmutableSet.builder();
        result.addAll(explicit);
        for (String code : userRegionOrder) {
            if (!explicit.contains(code) && !otherExplicit.contains(code)) {
                result.add(code);
            }
        }
        return result.build();
    }

    private Optional<String> regionNameToCode(String name) {
        String trimmed = name.trim();
        return regionData.regions().stream()
                .filter(e -> e.pattern().matcher(trimmed).matches())
                .map(RegionData.RegionDataEntry::code)
                .findFirst()
                .or(() -> regionData.regions().stream()
                        .map(RegionData.RegionDataEntry::code)
                        .filter(code -> code.equalsIgnoreCase(trimmed))
                        .findFirst());
    }

    private String regionFreeName(String fullName) {
        Matcher matcher = Patterns.SECTIONS.matcher(fullName);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            String inner = matcher.group(1);
            if (isRegionGroup(inner) || isLanguageGroup(inner)) {
                sb.append(fullName, lastEnd, matcher.start());
                lastEnd = matcher.end();
            }
        }
        sb.append(fullName, lastEnd, fullName.length());
        return WHITESPACE.matcher(sb.toString()).replaceAll(" ").trim();
    }

    private boolean isRegionGroup(String inner) {
        String[] elements = inner.split(",");
        if (elements.length == 0) {
            return false;
        }
        for (String element : elements) {
            String trimmed = element.trim();
            boolean matched = regionData.regions().stream()
                    .anyMatch(e -> e.pattern().matcher(trimmed).matches());
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLanguageGroup(String inner) {
        return LANGUAGE_GROUP.matcher(inner.trim()).matches();
    }

    private static String shortName(String name) {
        String stripped = PAREN_GROUP.matcher(name).replaceAll(" ");
        return WHITESPACE.matcher(stripped).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
    }
}
