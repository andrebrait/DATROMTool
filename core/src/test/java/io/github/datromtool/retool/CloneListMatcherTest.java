package io.github.datromtool.retool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.domain.retool.CloneList;
import io.github.datromtool.domain.retool.CloneListDescription;
import io.github.datromtool.domain.retool.NameType;
import io.github.datromtool.domain.retool.VariantFilter;
import io.github.datromtool.domain.retool.VariantGroup;
import io.github.datromtool.domain.retool.VariantTitle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static io.github.datromtool.util.TestUtils.createGame;
import static io.github.datromtool.util.TestUtils.getRegionByCode;
import static io.github.datromtool.util.TestUtils.loadRegionData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link CloneListMatcher} against both the real upstream fixtures already pinned by
 * {@code domain}'s {@code CloneListTest} (issue #19 step 1) and small synthetic clone lists
 * covering each {@link NameType} and conditional-filter condition in isolation.
 */
class CloneListMatcherTest {

    private static RegionData regionData;
    private static CloneList atari;
    private static CloneList dsi;

    @BeforeAll
    static void beforeAll() throws Exception {
        regionData = loadRegionData();
        SerializationHelper helper = SerializationHelper.getInstance();
        atari = helper.loadCloneList(resource("retool/clonelists/atari-2600-no-intro.json"));
        dsi = helper.loadCloneList(resource("retool/clonelists/nintendo-dsi-no-intro.json"));
    }

    private static Path resource(String name) throws URISyntaxException {
        return Paths.get(ClassLoader.getSystemResource(name).toURI());
    }

    private static ParsedGame gameNamed(String name) {
        return ParsedGame.builder()
                .game(createGame(name))
                .regionData(new RegionData(ImmutableSet.of()))
                .build();
    }

    private static CloneListMatcher matcher(CloneList cloneList, SortingPreference preference) {
        return new CloneListMatcher(cloneList, regionData, preference);
    }

    // Issue #19's own worked example: the Atari 2600 "Air Raiders"/"Bogey Blaster"/"Top Gun"
    // cross-region-rename group, using the real upstream fixture. All three titles omit
    // nameType/priority in the source file, so they default to SHORT/1.
    @Test
    void airRaidersGroupUnifiesAllThreeCrossRegionRenames() {
        CloneListMatcher m = matcher(atari, SortingPreference.builder().build());

        Optional<CloneListMatcher.MatchResult> usa = m.match(gameNamed("Air Raiders (USA)"));
        Optional<CloneListMatcher.MatchResult> europe = m.match(gameNamed("Bogey Blaster (Europe)"));
        Optional<CloneListMatcher.MatchResult> germany = m.match(gameNamed("Top Gun (Germany)"));

        assertTrue(usa.isPresent());
        assertTrue(europe.isPresent());
        assertTrue(germany.isPresent());
        assertEquals("Air Raiders", usa.get().group());
        assertEquals("Air Raiders", europe.get().group());
        assertEquals("Air Raiders", germany.get().group());
        assertEquals(1, usa.get().priority());
        assertEquals(1, europe.get().priority());
        assertEquals(1, germany.get().priority());
    }

    // Review round (short-name collapse picks the wrong title/priority): the real Atari 2600
    // fixture's "Forest" group has 4 titles, all defaulting to SHORT nameType, whose search
    // terms ("Forest (Two Player)", "Forest (Two Player) (Muted)", "Forest (One Player)",
    // "Forest (One Player) (Muted)") all fold to the identical short name "forest" (SHORT
    // strips every parenthetical group - see CloneListMatcher's class Javadoc). A game named
    // exactly "Forest (One Player)" therefore short-name-matches all 4 titles; picking the
    // first in file order (the old behavior) wrongly returns "Forest (Two Player)"'s priority
    // (1) instead of its own exact title's priority (3). The literal, case-sensitive equality
    // between the game's full name and a title's searchTerm is the most specific match
    // possible, and must win over every other title that only matches via short-name folding.
    @Test
    void mostSpecificMatchWinsOverFirstShortNameFold() {
        CloneListMatcher m = matcher(atari, SortingPreference.builder().build());

        Optional<CloneListMatcher.MatchResult> result = m.match(gameNamed("Forest (One Player)"));

        assertTrue(result.isPresent());
        assertEquals("Forest", result.get().group());
        assertEquals(
                3,
                result.get().priority(),
                "the exact-match title ('Forest (One Player)', priority 3) must win over the "
                        + "first short-name fold ('Forest (Two Player)', priority 1)");
    }

    @Test
    void unmatchedGameNameReturnsEmpty() {
        CloneListMatcher m = matcher(atari, SortingPreference.builder().build());
        assertTrue(m.match(gameNamed("Some Unrelated Homebrew (USA)")).isEmpty());
    }

    // Priority propagation: the "Alien's Return" group has one title at default priority 1 and
    // one explicit priority 2 (real fixture values).
    @Test
    void priorityPropagatesFromMatchedTitle() {
        CloneListMatcher m = matcher(atari, SortingPreference.builder().build());

        Optional<CloneListMatcher.MatchResult> defaultPriority = m.match(gameNamed("Alien's Return (USA)"));
        Optional<CloneListMatcher.MatchResult> explicitPriority = m.match(gameNamed("Col 'N' (Germany)"));

        assertEquals(1, defaultPriority.orElseThrow().priority());
        assertEquals("Alien's Return", defaultPriority.get().group());
        assertEquals(2, explicitPriority.orElseThrow().priority());
        assertEquals("Alien's Return", explicitPriority.get().group());
    }

    // SHORT nameType is case-insensitive (upstream's algorithm lowercases as its last step).
    @Test
    void shortNameTypeMatchesCaseInsensitively() {
        CloneList cloneList = new CloneList(
                new CloneListDescription("Test", "x", "1.0"),
                ImmutableList.of(new VariantGroup(
                        "Group",
                        ImmutableList.of(),
                        false,
                        ImmutableList.of(new VariantTitle(
                                "SOME GAME",
                                NameType.SHORT,
                                1,
                                ImmutableList.of(),
                                false,
                                false,
                                ImmutableMap.of(),
                                ImmutableList.of())),
                        ImmutableList.of(),
                        ImmutableList.of())));
        CloneListMatcher m = matcher(cloneList, SortingPreference.builder().build());

        assertTrue(m.match(gameNamed("Some Game (USA)")).isPresent());
        assertTrue(m.match(gameNamed("some game (Europe) (Rev 1)")).isPresent());
        assertTrue(m.match(gameNamed("Some Other Game (USA)")).isEmpty());
    }

    @Test
    void fullNameTypeMatchesExactlyAndCaseSensitively() {
        CloneList cloneList = singleTitleCloneList(NameType.FULL, "Some Game (USA)");
        CloneListMatcher m = matcher(cloneList, SortingPreference.builder().build());

        assertTrue(m.match(gameNamed("Some Game (USA)")).isPresent());
        assertTrue(m.match(gameNamed("some game (usa)")).isEmpty(), "FULL comparison is case-sensitive");
        assertTrue(m.match(gameNamed("Some Game (Europe)")).isEmpty());
    }

    // REGEX nameType uses whole-string matches(), not find() - a documented implementation
    // choice (upstream's docs do not specify). "Some.*Game" would match "Some Other Game (USA)"
    // under find() but not under matches() against the full name.
    @Test
    void regexNameTypeUsesWholeStringMatchesSemantics() {
        CloneList cloneList = singleTitleCloneList(NameType.REGEX, "Some.*Game \\(USA\\)");
        CloneListMatcher m = matcher(cloneList, SortingPreference.builder().build());

        assertTrue(m.match(gameNamed("Some Other Game (USA)")).isPresent());
        assertTrue(
                m.match(gameNamed("Prefix Some Other Game (USA)")).isEmpty(),
                "matches() is anchored to the whole name, unlike find()");
    }

    // REGION_FREE strips only the region/language parenthetical groups, keeping other tags
    // (e.g. "(Rev 1)") intact - unlike SHORT, which strips every parenthetical group.
    @Test
    void regionFreeNameTypeStripsOnlyRegionAndLanguageGroups() {
        CloneList cloneList = singleTitleCloneList(NameType.REGION_FREE, "Some Game (Rev 1)");
        CloneListMatcher m = matcher(cloneList, SortingPreference.builder().build());

        assertTrue(m.match(gameNamed("Some Game (USA) (Rev 1)")).isPresent());
        assertTrue(m.match(gameNamed("Some Game (Europe) (En,Fr) (Rev 1)")).isPresent());
        assertTrue(
                m.match(gameNamed("Some Game (Rev 2)")).isEmpty(),
                "a non-matching non-region tag must not be stripped away");
    }

    // Conditional filter: matchLanguages fires/doesn't.
    @Test
    void conditionalFilterMatchLanguagesFiresOnlyWhenGameHasLanguage() {
        CloneList cloneList = singleTitleWithFilter(
                new VariantFilter.Conditions(ImmutableList.of("En"), ImmutableList.of(), null, null),
                new VariantFilter.Results(null, 5, ImmutableList.of(), null, null, null, ImmutableMap.of()));
        CloneListMatcher m = matcher(cloneList, SortingPreference.builder().build());

        ParsedGame english = gameNamed("Some Game (USA)").toBuilder()
                .languages(ImmutableSet.of("en"))
                .build();
        ParsedGame french = gameNamed("Some Game (USA)").toBuilder()
                .languages(ImmutableSet.of("fr"))
                .build();

        assertEquals(5, m.match(english).orElseThrow().priority());
        assertEquals(1, m.match(french).orElseThrow().priority(), "no override: filter must not fire");
    }

    // Conditional filter: matchRegions fires/doesn't - clone list region names ("Europe") map to
    // this codebase's internal region codes ("EUR") via RegionData's patterns.
    @Test
    void conditionalFilterMatchRegionsFiresOnlyWhenGameHasRegion() {
        CloneList cloneList = singleTitleWithFilter(
                new VariantFilter.Conditions(ImmutableList.of(), ImmutableList.of("Europe"), null, null),
                new VariantFilter.Results(null, 5, ImmutableList.of(), null, null, null, ImmutableMap.of()));
        CloneListMatcher m = matcher(cloneList, SortingPreference.builder().build());

        ParsedGame europe = gameNamed("Some Game (USA)").toBuilder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .build();
        ParsedGame usa = gameNamed("Some Game (USA)").toBuilder()
                .regionData(getRegionByCode(regionData, "USA"))
                .build();

        assertEquals(5, m.match(europe).orElseThrow().priority());
        assertEquals(1, m.match(usa).orElseThrow().priority(), "no override: filter must not fire");
    }

    // Conditional filter: matchString fires/doesn't.
    @Test
    void conditionalFilterMatchStringFiresOnlyOnRegexMatch() {
        CloneList cloneList = singleTitleWithFilter(
                new VariantFilter.Conditions(ImmutableList.of(), ImmutableList.of(), "^Some Game \\(USA\\)$", null),
                new VariantFilter.Results(null, 5, ImmutableList.of(), null, null, null, ImmutableMap.of()));
        CloneListMatcher m = matcher(cloneList, SortingPreference.builder().build());

        assertEquals(5, m.match(gameNamed("Some Game (USA)")).orElseThrow().priority());
    }

    // Conditional filter: regionOrder condition flips with the user's region order - issue #19's
    // own acceptance criterion. Uses the real Nintendo DSi fixture's "TWL EVA. IMPORT" group:
    // "TWL EVA. IMPORT EUR (Ver1.3) (Program)" has base priority 2 and a filter that overrides to
    // priority 1 when Europe ranks above Japan and USA in the user's region order.
    @Test
    void regionOrderFilterFlipsWithUserRegionOrder() {
        Optional<CloneListMatcher.MatchResult> europeFirst = matcher(
                dsi,
                SortingPreference.builder().regions(ImmutableSet.of("EUR", "JPN", "USA")).build())
                .match(gameNamed("TWL EVA. IMPORT EUR (Ver1.3) (Program)"));
        Optional<CloneListMatcher.MatchResult> europeLast = matcher(
                dsi,
                SortingPreference.builder().regions(ImmutableSet.of("JPN", "USA", "EUR")).build())
                .match(gameNamed("TWL EVA. IMPORT EUR (Ver1.3) (Program)"));

        assertEquals(1, europeFirst.orElseThrow().priority(), "Europe ranked above Japan/USA: filter fires");
        assertEquals(2, europeLast.orElseThrow().priority(), "Europe ranked below Japan/USA: filter does not fire");
    }

    // "All other regions" placeholder (upstream: "the remaining regions will be calculated
    // automatically based on the array you've already populated") resolves against the user's own
    // region order.
    @Test
    void regionOrderAllOtherRegionsWildcardResolvesAgainstUserOrder() {
        VariantFilter.RegionOrder regionOrder = new VariantFilter.RegionOrder(
                ImmutableList.of("Japan"),
                ImmutableList.of("All other regions"));
        CloneList cloneList = singleTitleWithFilter(
                new VariantFilter.Conditions(ImmutableList.of(), ImmutableList.of(), null, regionOrder),
                new VariantFilter.Results(null, 5, ImmutableList.of(), null, null, null, ImmutableMap.of()));

        // Japan ranked first: higher than "all other regions" (EUR, USA) -> filter fires.
        Optional<CloneListMatcher.MatchResult> japanFirst = matcher(
                cloneList,
                SortingPreference.builder().regions(ImmutableSet.of("JPN", "EUR", "USA")).build())
                .match(gameNamed("Some Game (USA)"));
        // Japan ranked last: not higher than the others -> filter does not fire.
        Optional<CloneListMatcher.MatchResult> japanLast = matcher(
                cloneList,
                SortingPreference.builder().regions(ImmutableSet.of("EUR", "USA", "JPN")).build())
                .match(gameNamed("Some Game (USA)"));

        assertEquals(5, japanFirst.orElseThrow().priority());
        assertEquals(1, japanLast.orElseThrow().priority());
    }

    @Test
    void variantGroupWithIgnoreFlagIsSkipped() {
        CloneList cloneList = new CloneList(
                new CloneListDescription("Test", "x", "1.0"),
                ImmutableList.of(new VariantGroup(
                        "Ignored Group",
                        ImmutableList.of(),
                        true,
                        ImmutableList.of(new VariantTitle(
                                "Some Game",
                                NameType.SHORT,
                                1,
                                ImmutableList.of(),
                                false,
                                false,
                                ImmutableMap.of(),
                                ImmutableList.of())),
                        ImmutableList.of(),
                        ImmutableList.of())));
        CloneListMatcher m = matcher(cloneList, SortingPreference.builder().build());

        assertFalse(m.match(gameNamed("Some Game (USA)")).isPresent());
    }

    private static CloneList singleTitleCloneList(NameType nameType, String searchTerm) {
        return new CloneList(
                new CloneListDescription("Test", "x", "1.0"),
                ImmutableList.of(new VariantGroup(
                        "Group",
                        ImmutableList.of(),
                        false,
                        ImmutableList.of(new VariantTitle(
                                searchTerm,
                                nameType,
                                1,
                                ImmutableList.of(),
                                false,
                                false,
                                ImmutableMap.of(),
                                ImmutableList.of())),
                        ImmutableList.of(),
                        ImmutableList.of())));
    }

    private static CloneList singleTitleWithFilter(
            VariantFilter.Conditions conditions,
            VariantFilter.Results results) {
        return new CloneList(
                new CloneListDescription("Test", "x", "1.0"),
                ImmutableList.of(new VariantGroup(
                        "Group",
                        ImmutableList.of(),
                        false,
                        ImmutableList.of(new VariantTitle(
                                "Some Game",
                                NameType.SHORT,
                                1,
                                ImmutableList.of(),
                                false,
                                false,
                                ImmutableMap.of(),
                                ImmutableList.of(new VariantFilter(conditions, results)))),
                        ImmutableList.of(),
                        ImmutableList.of())));
    }
}
