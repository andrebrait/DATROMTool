package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.github.datromtool.util.TestUtils.createGame;
import static io.github.datromtool.util.TestUtils.getRegionByCode;
import static io.github.datromtool.util.TestUtils.loadRegionData;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class LanguagesCountSubComparatorTest {

    static RegionData regionData;

    @BeforeAll
    static void beforeAll() throws Exception {
        regionData = loadRegionData();
    }

    @Test
    void compare_shouldKeepOrderIfNotApplicable() {
        SubComparator subComparator = new LanguagesCountSubComparator();
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
    void testCompare_shouldPreferLeastLanguagesCounts() {
        SubComparator subComparator = new LanguagesCountSubComparator();
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .languages(ImmutableSet.of("ja", "en"))
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
    void testCompare_shouldPreferMostLanguagesCounts() {
        SubComparator subComparator = new LanguagesCountSubComparator().reversed();
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .languages(ImmutableSet.of("ja", "en"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferLeastLanguagesCounts_useRegionsIfNoLanguagesSet() {
        SubComparator subComparator = new LanguagesCountSubComparator();
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "JPN"))
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
    void testCompare_shouldPreferMostLanguagesCounts_useRegionsIfNoLanguagesSet() {
        SubComparator subComparator = new LanguagesCountSubComparator().reversed();
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "JPN"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferLeastLanguagesCounts_ignoreRegionsIfLanguagesSet() {
        SubComparator subComparator = new LanguagesCountSubComparator();
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .languages(ImmutableSet.of("en", "es", "fr"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "JPN"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferMostLanguagesCounts_ignoreRegionsIfLanguagesSet() {
        SubComparator subComparator = new LanguagesCountSubComparator().reversed();
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "JPN"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .languages(ImmutableSet.of("en", "es", "fr"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }


}