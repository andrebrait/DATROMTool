package io.github.datromtool.sorting;

import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.github.datromtool.util.TestUtils.createGame;
import static io.github.datromtool.util.TestUtils.getRegionByCode;
import static io.github.datromtool.util.TestUtils.loadRegionData;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue #19 step 2: {@link PrioritySubComparator} orders by
 * {@link ParsedGame#getClonelistPriority()} (lower wins, upstream's "1 is highest priority"), and
 * stays neutral whenever either compared game lacks a clone-list-assigned priority.
 */
class PrioritySubComparatorTest {

    static RegionData regionData;

    @BeforeAll
    static void beforeAll() throws Exception {
        regionData = loadRegionData();
    }

    @Test
    void testCompare_shouldKeepOrderIfNeitherHasPriority() {
        SubComparator subComparator = new PrioritySubComparator();
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
        assertArrayEquals(new ParsedGame[]{tg1, tg2}, parsedGames);
    }

    @Test
    void testCompare_shouldKeepOrderIfOnlyOneHasPriority() {
        SubComparator subComparator = new PrioritySubComparator();
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 1"))
                .clonelistPriority(1)
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertArrayEquals(new ParsedGame[]{tg1, tg2}, parsedGames, "neutral when one side has no priority");
    }

    @Test
    void testCompare_shouldPreferLowerPriorityNumber() {
        SubComparator subComparator = new PrioritySubComparator();
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 1"))
                .clonelistPriority(2)
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .clonelistPriority(1)
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertArrayEquals(new ParsedGame[]{tg2, tg1}, parsedGames, "priority 1 beats priority 2");
    }

    @Test
    void testCompare_shouldKeepOrderIfSamePriority() {
        SubComparator subComparator = new PrioritySubComparator();
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 1"))
                .clonelistPriority(3)
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .clonelistPriority(3)
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, subComparator);
        assertArrayEquals(new ParsedGame[]{tg1, tg2}, parsedGames);
    }

    @Test
    void testGetCriteria() {
        assertEquals("Clone list priority", new PrioritySubComparator().getCriteria());
    }
}
