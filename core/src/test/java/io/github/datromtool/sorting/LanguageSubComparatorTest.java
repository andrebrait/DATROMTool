package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.data.SortingPreference;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;

import static io.github.datromtool.util.TestUtils.createGame;
import static io.github.datromtool.util.TestUtils.getRegionByCode;
import static io.github.datromtool.util.TestUtils.loadRegionData;
import static org.testng.Assert.assertEquals;

public class LanguageSubComparatorTest {

    static RegionData regionData;

    @BeforeClass
    public static void beforeAll() throws Exception {
        regionData = loadRegionData();
    }

    @Test
    public void testCompare_shouldKeepOrderIfNotApplicable() {
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
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldPreferFirstLanguages_onlyRegions() {
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
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferFirstLanguages() {
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
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferSelectedLanguages() {
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
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferFirstLanguages_ignoreRegionIfLanguageSet() {
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
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

}