package io.github.datromtool.domain.retool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.datromtool.SerializationHelper;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Domain model tests for {@link CloneList} and friends (issue #19 step 1: models only, no
 * grouping/filtering integration). Real-fixture tests parse actual files downloaded from
 * upstream's <a href="https://github.com/unexpectedpanda/retool-clonelists-metadata">
 * retool-clonelists-metadata</a> repository (commit as of 2026-07-26, {@code main} branch) and
 * assert values read directly from those files:
 *
 * <ul>
 *   <li>{@code retool/clonelists/atari-2600-no-intro.json} - byte-for-byte copy of
 *       {@code clonelists/Atari - Atari 2600 (No-Intro).json} (11556 bytes, untrimmed). Chosen
 *       because it contains the exact "Air Raiders"/"Bogey Blaster"/"Top Gun" grouping issue #19
 *       cites as its own worked example.</li>
 *   <li>{@code retool/clonelists/nintendo-dsi-no-intro.json} - byte-for-byte copy of
 *       {@code clonelists/Nintendo - Nintendo DSi (No-Intro).json} (6180 bytes, untrimmed).
 *       Chosen because the Atari 2600 file has zero {@code filters}/{@code regionOrder} usage,
 *       and this is the smallest upstream file that does (found via
 *       {@code gh api search/code?q=regionOrder...}).</li>
 * </ul>
 *
 * Neither fixture is trimmed - both are small enough to keep whole, so every value asserted
 * below can be cross-checked against the live GitHub file.
 */
class CloneListTest {

    private static final SerializationHelper HELPER = SerializationHelper.getInstance();

    private static Path resource(String name) throws URISyntaxException {
        return Paths.get(ClassLoader.getSystemResource(name).toURI());
    }

    // Matrix row 1: real clonelist fixture parses; assert real values (group names, a
    // searchTerm, a nameType, a priority).
    @Test
    void parsesRealAtari2600Fixture() throws Exception {
        CloneList cloneList = HELPER.loadCloneList(resource("retool/clonelists/atari-2600-no-intro.json"));

        assertEquals("Atari - Atari 2600 (No-Intro)", cloneList.description().name());
        assertEquals("2026-01-02 11:07:19", cloneList.description().lastUpdated());
        assertEquals("2.4.8", cloneList.description().minimumVersion());
        assertEquals(77, cloneList.variants().size());

        VariantGroup airRaiders = cloneList.variants().stream()
                .filter(v -> v.group().equals("Air Raiders"))
                .findFirst()
                .orElseThrow();
        assertEquals(
                ImmutableList.of("Air Raiders", "Bogey Blaster", "Top Gun"),
                airRaiders.titles().stream().map(VariantTitle::searchTerm).toList());
        // Every title in this group omits nameType/priority in the source file - defaults apply.
        for (VariantTitle title : airRaiders.titles()) {
            assertEquals(NameType.SHORT, title.nameType());
            assertEquals(1, title.priority());
        }

        VariantGroup aliensReturn = cloneList.variants().stream()
                .filter(v -> v.group().equals("Alien's Return"))
                .findFirst()
                .orElseThrow();
        VariantTitle colN = aliensReturn.titles().stream()
                .filter(t -> t.searchTerm().equals("Col 'N'"))
                .findFirst()
                .orElseThrow();
        assertEquals(2, colN.priority());

        VariantGroup basicProgramming = cloneList.variants().stream()
                .filter(v -> v.group().equals("BASIC Programming"))
                .findFirst()
                .orElseThrow();
        assertEquals(ImmutableList.of("Applications"), basicProgramming.categories());

        VariantGroup berzerk = cloneList.variants().stream()
                .filter(v -> v.group().equals("Berzerk"))
                .findFirst()
                .orElseThrow();
        assertEquals(1, berzerk.supersets().size());
        assertEquals("Berzerk (Enhanced Edition)", berzerk.supersets().get(0).searchTerm());
    }

    // Matrix row 1 (continued): real fixture with a regionOrder filter - the Atari 2600 fixture
    // has none, so a second real fixture covers this shape.
    @Test
    void parsesRealNintendoDsiRegionOrderFixture() throws Exception {
        CloneList cloneList = HELPER.loadCloneList(resource("retool/clonelists/nintendo-dsi-no-intro.json"));

        assertEquals("Nintendo - Nintendo DSi (No-Intro)", cloneList.description().name());
        assertEquals(11, cloneList.variants().size());

        VariantGroup twlEvaImport = cloneList.variants().stream()
                .filter(v -> v.group().equals("TWL EVA. IMPORT"))
                .findFirst()
                .orElseThrow();
        VariantTitle eur = twlEvaImport.titles().stream()
                .filter(t -> t.searchTerm().equals("TWL EVA. IMPORT EUR (Ver1.3) (Program)"))
                .findFirst()
                .orElseThrow();
        assertEquals(2, eur.priority());
        assertEquals(1, eur.filters().size());
        VariantFilter filter = eur.filters().get(0);
        assertEquals(
                ImmutableList.of("Europe"),
                filter.conditions().regionOrder().higherRegions());
        assertEquals(
                ImmutableList.of("Japan", "USA"),
                filter.conditions().regionOrder().lowerRegions());
        assertEquals(1, filter.results().priority());
        // Results-level fields not present in the source stay null (no fabricated default).
        assertNull(filter.results().group());

        VariantGroup demoVideo = cloneList.variants().stream()
                .filter(v -> v.group().equals("Nintendo DSi XL Demo Video (Demo)"))
                .findFirst()
                .orElseThrow();
        assertEquals(ImmutableList.of("Demos", "Videos"), demoVideo.categories());
    }

    // Matrix row 3: synthetic minimal clonelist covering every modeled field, including all 4
    // nameTypes, a regionOrder filter, and a compilation with titlePosition - round-trips
    // through the JSON mapper equal.
    @Test
    void syntheticCloneListWithEveryFieldRoundTrips() throws Exception {
        CloneList original = new CloneList(
                new CloneListDescription("Test System (Test Group)", "01 January 2026", "2.4.8"),
                ImmutableList.of(
                        new VariantGroup(
                                "Full Coverage Group",
                                ImmutableList.of("Games", "Demos"),
                                false,
                                ImmutableList.of(
                                        new VariantTitle(
                                                "Full Coverage Game (Short)",
                                                NameType.SHORT,
                                                1,
                                                ImmutableList.of("Games"),
                                                true,
                                                false,
                                                ImmutableMap.of("english", "Full Coverage Game"),
                                                ImmutableList.of(
                                                        new VariantFilter(
                                                                new VariantFilter.Conditions(
                                                                        ImmutableList.of("en"),
                                                                        ImmutableList.of("USA"),
                                                                        "^Full Coverage.*$",
                                                                        new VariantFilter.RegionOrder(
                                                                                ImmutableList.of("Europe"),
                                                                                ImmutableList.of("Japan", "USA"))),
                                                                new VariantFilter.Results(
                                                                        "Full Coverage Group (EUR)",
                                                                        1,
                                                                        ImmutableList.of("Games"),
                                                                        true,
                                                                        true,
                                                                        true,
                                                                        ImmutableMap.of("german", "Voll Abdeckung Spiel"))))),
                                        new VariantTitle(
                                                "Full Coverage Game (Full name search term)",
                                                NameType.FULL,
                                                2,
                                                ImmutableList.of(),
                                                false,
                                                false,
                                                ImmutableMap.of(),
                                                ImmutableList.of()),
                                        new VariantTitle(
                                                "Full Coverage Game (Region free)",
                                                NameType.REGION_FREE,
                                                3,
                                                ImmutableList.of(),
                                                false,
                                                false,
                                                ImmutableMap.of(),
                                                ImmutableList.of()),
                                        new VariantTitle(
                                                "^Full Coverage Game \\(Regex.*\\)$",
                                                NameType.REGEX,
                                                4,
                                                ImmutableList.of(),
                                                false,
                                                false,
                                                ImmutableMap.of(),
                                                ImmutableList.of())),
                                ImmutableList.of(
                                        new VariantTitle(
                                                "Full Coverage Game - Special Edition",
                                                NameType.SHORT,
                                                1,
                                                ImmutableList.of(),
                                                false,
                                                false,
                                                ImmutableMap.of(),
                                                ImmutableList.of())),
                                ImmutableList.of(
                                        new VariantCompilation(
                                                "Full Coverage Game & Full Coverage Game 2",
                                                NameType.SHORT,
                                                1,
                                                ImmutableList.of("Compilations"),
                                                1,
                                                ImmutableMap.of("english", "Full Coverage Game & Full Coverage Game 2"))))));

        String json = HELPER.getJsonMapper().writeValueAsString(original);
        CloneList roundTripped = HELPER.getJsonMapper().readValue(json, CloneList.class);

        assertEquals(original, roundTripped);
    }

    // Matrix row 4: title with only searchTerm set -> nameType short, priority 1.
    @Test
    void defaultsApplyWhenOnlySearchTermIsSet() throws Exception {
        String json = "{\"searchTerm\": \"Only Search Term\"}";
        VariantTitle title = HELPER.getJsonMapper().readValue(json, VariantTitle.class);

        assertEquals("Only Search Term", title.searchTerm());
        assertEquals(NameType.SHORT, title.nameType());
        assertEquals(1, title.priority());
        assertFalse(title.englishFriendly());
        assertFalse(title.isOldest());
        assertTrue(title.categories().isEmpty());
        assertTrue(title.localNames().isEmpty());
        assertTrue(title.filters().isEmpty());
    }

    // Matrix row 5: unknown-field tolerance - a fixture with an invented key parses cleanly.
    @Test
    void unknownFieldsAreToleratedNotRejected() throws Exception {
        String json = "{"
                + "\"description\": {\"name\": \"Test\", \"lastUpdated\": \"x\", \"minimumVersion\": \"1.0\","
                + " \"futureUpstreamField\": \"invented, not modeled\"},"
                + "\"variants\": [{\"group\": \"G\", \"titles\": [{\"searchTerm\": \"T\","
                + " \"anotherInventedField\": 42}]}],"
                + "\"topLevelInventedField\": true"
                + "}";

        CloneList cloneList = HELPER.getJsonMapper().readValue(json, CloneList.class);

        assertEquals("Test", cloneList.description().name());
        assertEquals(1, cloneList.variants().size());
        assertEquals("T", cloneList.variants().get(0).titles().get(0).searchTerm());
    }

    // Matrix row 6a: upstream's titles spec shows object shape exclusively (verified against the
    // live docs - see VariantTitle's Javadoc); a bare string is a parse error, not a shorthand.
    // Pinned to the actual exception: tools.jackson.databind.exc.MismatchedInputException,
    // "Cannot construct instance of `io.github.datromtool.domain.retool.VariantTitle` (although
    // at least one Creator exists): no String-argument constructor/factory method to
    // deserialize from String value ('Bare String Title')" (verified via a throwaway probe
    // against the real jsonMapper before pinning, not reasoned from memory).
    @Test
    void bareStringTitleIsRejectedNotParsedAsShorthand() {
        String json = "{\"group\": \"G\", \"titles\": [\"Bare String Title\"]}";

        JacksonException exception = assertThrows(
                JacksonException.class,
                () -> HELPER.getJsonMapper().readValue(json, VariantGroup.class));
        assertEquals(MismatchedInputException.class, exception.getClass());
        assertTrue(
                exception.getMessage().contains("Cannot construct instance")
                        && exception.getMessage().contains("VariantTitle")
                        && exception.getMessage().contains("Bare String Title"),
                "expected a construction-failure message naming VariantTitle and the offending"
                        + " value, got: " + exception.getMessage());
    }

    // Issue #19 step 2: CloneList#isCompatibleWith, the minimumVersion compat *check* (no
    // enforcement wiring yet - that's step 3). Real fixture declares minimumVersion "2.4.8".
    @Test
    void isCompatibleWith_exactVersionMatches() throws Exception {
        CloneList cloneList = HELPER.loadCloneList(resource("retool/clonelists/atari-2600-no-intro.json"));
        assertTrue(cloneList.isCompatibleWith("2.4.8"));
    }

    @Test
    void isCompatibleWith_higherVersionIsCompatible() throws Exception {
        CloneList cloneList = HELPER.loadCloneList(resource("retool/clonelists/atari-2600-no-intro.json"));
        assertTrue(cloneList.isCompatibleWith("2.5.0"));
        assertTrue(cloneList.isCompatibleWith("3.0.0"));
        assertTrue(cloneList.isCompatibleWith("2.4.9"));
    }

    @Test
    void isCompatibleWith_lowerVersionIsIncompatible() throws Exception {
        CloneList cloneList = HELPER.loadCloneList(resource("retool/clonelists/atari-2600-no-intro.json"));
        assertFalse(cloneList.isCompatibleWith("2.4.7"));
        assertFalse(cloneList.isCompatibleWith("2.3.9"));
        assertFalse(cloneList.isCompatibleWith("1.9.9"));
    }

    @Test
    void isCompatibleWith_missingTrailingSegmentTreatedAsZero() {
        CloneList cloneList = new CloneList(
                new CloneListDescription("Test", "01 January 2026", "2.4"),
                ImmutableList.of());
        assertTrue(cloneList.isCompatibleWith("2.4.0"));
        assertTrue(cloneList.isCompatibleWith("2.4"));
        assertFalse(cloneList.isCompatibleWith("2.3.9"));
    }

    @Test
    void isCompatibleWith_nonNumericSegmentFallsBackToLexicographicCompare() {
        CloneList cloneList = new CloneList(
                new CloneListDescription("Test", "01 January 2026", "2.4.8-rc1"),
                ImmutableList.of());
        assertTrue(cloneList.isCompatibleWith("2.4.8-rc1"));
        assertTrue(cloneList.isCompatibleWith("2.4.8-rc2"));
        assertFalse(cloneList.isCompatibleWith("2.4.8-rc0"));
    }

    // Matrix row 6b: invalid nameType value -> clear error (pinned).
    // Pinned to the actual exception: tools.jackson.databind.exc.InvalidFormatException,
    // "Cannot deserialize value of type `io.github.datromtool.domain.retool.NameType` from
    // String "not-a-real-name-type": not one of the values accepted for Enum class:
    // [regionFree, short, regex, full]" (verified via the same probe as above).
    @Test
    void invalidNameTypeValueIsRejected() {
        String json = "{\"searchTerm\": \"T\", \"nameType\": \"not-a-real-name-type\"}";

        JacksonException exception = assertThrows(
                JacksonException.class,
                () -> HELPER.getJsonMapper().readValue(json, VariantTitle.class));
        assertEquals(InvalidFormatException.class, exception.getClass());
        assertTrue(
                exception.getMessage().contains("not-a-real-name-type")
                        && exception.getMessage().contains("NameType")
                        && exception.getMessage().contains("short")
                        && exception.getMessage().contains("full")
                        && exception.getMessage().contains("regionFree")
                        && exception.getMessage().contains("regex"),
                "expected the offending value and all 4 accepted enum values in the error"
                        + " message, got: " + exception.getMessage());
    }
}
