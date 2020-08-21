package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.data.SortingPreference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.github.datromtool.util.TestUtils.createGame;
import static io.github.datromtool.util.TestUtils.getRegionByCode;
import static io.github.datromtool.util.TestUtils.loadRegionData;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class LanguageSubComparatorTest {

    static RegionData regionData;

    @BeforeAll
    static void beforeAll() throws Exception {
        regionData = loadRegionData();
    }

    @Test
    void testCompare_shouldKeepOrderIfNotApplicable() {
        SubComparator subComparator =
                new LanguageSubComparator(SortingPreference.builder().build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldPreferFirstLanguages_onlyRegions() {
        SubComparator subComparator = new LanguageSubComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("en", "ja"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "JPN"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferFirstLanguages() {
        SubComparator subComparator = new LanguageSubComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("en", "ja"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .languages(ImmutableSet.of("ja"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferSelectedLanguages() {
        SubComparator subComparator = new LanguageSubComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("ja", "en"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .languages(ImmutableSet.of("pl"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferFirstLanguages_ignoreRegionIfLanguageSet() {
        SubComparator subComparator = new LanguageSubComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("en", "ja"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "JPN"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .languages(ImmutableSet.of("ja"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

}