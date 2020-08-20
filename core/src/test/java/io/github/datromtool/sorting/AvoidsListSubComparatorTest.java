package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.data.SortingPreference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.regex.Pattern;

import static io.github.datromtool.util.TestUtils.createGame;
import static io.github.datromtool.util.TestUtils.getRegionByCode;
import static io.github.datromtool.util.TestUtils.loadRegionData;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AvoidsListSubComparatorTest {

    static RegionData regionData;

    @BeforeAll
    static void beforeAll() throws Exception {
        regionData = loadRegionData();
    }

    @Test
    void compare_shouldKeepOrderIfNotApplicable() {
        SubComparator subComparator =
                new AvoidsListSubComparator(SortingPreference.builder().build());
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
    void testCompare_shouldAvoidIfInAvoidsList() {
        SubComparator subComparator = new AvoidsListSubComparator(SortingPreference.builder()
                .avoids(ImmutableSet.of(Pattern.compile("(?i)game 1")))
                .build());
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
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferIfInAvoidsList() {
        SubComparator subComparator = new AvoidsListSubComparator(SortingPreference.builder()
                .avoids(ImmutableSet.of(Pattern.compile("(?i)game 2")))
                .build())
                .reversed();
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
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }
}