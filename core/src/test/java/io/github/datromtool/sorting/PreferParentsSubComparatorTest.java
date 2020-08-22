package io.github.datromtool.sorting;

import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;

import static io.github.datromtool.util.TestUtils.createGame;
import static io.github.datromtool.util.TestUtils.getRegionByCode;
import static io.github.datromtool.util.TestUtils.loadRegionData;
import static org.testng.Assert.assertEquals;

public class PreferParentsSubComparatorTest {

    static RegionData regionData;

    @BeforeClass
    public static void beforeAll() throws Exception {
        regionData = loadRegionData();
    }

    @Test
    public void testCompare_shouldKeepOrderIfNotApplicable() {
        SubComparator subComparator = new PreferParentsSubComparator();
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
    public void testCompare_shouldPreferIfParent() {
        SubComparator subComparator = new PreferParentsSubComparator();
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .parent(true)
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

}