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

public class SelectedLanguagesCountSubComparatorTest {

    static RegionData regionData;

    @BeforeClass
    public static void beforeAll() throws Exception {
        regionData = loadRegionData();
    }

    @Test
    public void testCompare_shouldKeepOrderIfNotApplicable() {
        SubComparator subComparator =
                new SelectedLanguagesCountSubComparator(SortingPreference.builder().build());
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
    public void testCompare_shouldPreferLeastLanguages_onlyRegions() {
        SubComparator subComparator = new SelectedLanguagesCountSubComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("en", "ja"))
                .build());
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
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferLeastLanguages() {
        SubComparator subComparator = new SelectedLanguagesCountSubComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("en", "ja"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .languages(ImmutableSet.of("en", "ja"))
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
    public void testCompare_shouldPreferLeastLanguages_ignoreRegionIfLanguageSet() {
        SubComparator subComparator = new SelectedLanguagesCountSubComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("en", "ja"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "JPN"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .languages(ImmutableSet.of("ja", "en"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

}