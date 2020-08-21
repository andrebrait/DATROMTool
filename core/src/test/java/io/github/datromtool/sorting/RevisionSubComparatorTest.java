package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.github.datromtool.util.TestUtils.createGame;
import static io.github.datromtool.util.TestUtils.getRegionByCode;
import static io.github.datromtool.util.TestUtils.loadRegionData;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class RevisionSubComparatorTest {

    static RegionData regionData;

    @BeforeAll
    static void beforeAll() throws Exception {
        regionData = loadRegionData();
    }

    @Test
    void testCompare_shouldKeepOrderIfNotApplicable() {
        SubComparator subComparator = new RevisionSubComparator();
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
    void testCompare_shouldPreferEarlyRevisions() {
        SubComparator subComparator = new RevisionSubComparator();
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(0L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferEarlyRevisions_unsetIsZero() {
        SubComparator subComparator = new RevisionSubComparator();
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 2L))
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



}