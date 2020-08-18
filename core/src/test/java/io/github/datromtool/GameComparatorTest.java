package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.domain.datafile.Game;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class GameComparatorTest {

    static RegionData regionData;

    @BeforeAll
    static void beforeAll() throws Exception {
        regionData = SerializationHelper.getInstance()
                .loadRegionData(Paths.get(ClassLoader.getSystemResource("region-data.yaml")
                        .toURI()));
    }

    @Test
    void testCompare_shouldPreventBadDumps() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("USA", "EUR"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .bad(true)
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldNotPreventPrereleasesIfNotPreferPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("USA", "EUR"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(1L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferPrereleaseIfPreferPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("USA", "EUR"))
                .preferPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("EUR"))
                .beta(ImmutableList.of(1L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldSortByRegion() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("USA", "EUR"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldKeepOrderByRegion() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("EUR", "USA"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameRegions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("EUR", "USA"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("EUR"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameRegions_differentNumberOfRegions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("EUR", "USA"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA", "EUR"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameRegions_sameNumberOfRegions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("EUR", "USA"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA", "EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA", "EUR"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldSortByLanguage() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("en", "ja"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("JPN"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldKeepOrderByLanguage() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("ja", "en"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("JPN"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameLanguages() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("jp", "en"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameLanguages_sameNumberOfLanguages() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("ja", "en"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA", "JPN"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA", "JPN"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldPreferWithMostSelectedLanguages() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("ja", "en"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA", "JPN"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferWithMostLanguages() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("ja", "en"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA", "JPN"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA", "JPN", "BRA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferWithMostSelectedLanguagesOverMostLanguages() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("ja", "en"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA", "BRA", "ESP", "POL"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA", "JPN"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldSortByRegionAndLanguage() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("USA", "EUR"))
                .languages(ImmutableSet.of("ja", "en"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame tg3 = ParsedGame.builder()
                .regionData(getByCode("EUR"))
                .languages(ImmutableSet.of("ja"))
                .game(createGame("Test game 3"))
                .build();
        ParsedGame tg4 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .languages(ImmutableSet.of("ja"))
                .game(createGame("Test game 4"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2, tg3, tg4};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg4, tg2, tg3, tg1});
    }

    @Test
    void testCompare_shouldSortByRegionAndLanguage_prioritizeLanguages() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("USA", "EUR"))
                .languages(ImmutableSet.of("ja", "en"))
                .prioritizeLanguages(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame tg3 = ParsedGame.builder()
                .regionData(getByCode("EUR"))
                .languages(ImmutableSet.of("ja"))
                .game(createGame("Test game 3"))
                .build();
        ParsedGame tg4 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .languages(ImmutableSet.of("ja"))
                .game(createGame("Test game 4"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2, tg3, tg4};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg4, tg3, tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferReleaseIfAllElseIsEqual() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("USA", "EUR"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(1L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferParents_ifPreferParents() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .preferParents(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .parent(true)
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferRecentRevisions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferRecentRevisions_ignoreParent() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .parent(true)
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameRevision() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldPreferRecentRevision_minorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferRecentRevision_majorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferEarlyRevisionsIfEarlyRevisions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyRevisions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameRevision_earlyRevisions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyRevisions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldPreferEarlyRevision_minorDiff_earlyRevisions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyRevisions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferEarlyRevision_majorDiff_earlyRevisions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyRevisions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferRecentVersions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferRecentVersions_ignoreParent() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .parent(true)
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameVersion() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldPreferRecentVersion_minorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferRecentVersion_majorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferEarlyVersionsIfEarlyVersions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyVersions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameVersion_earlyVersions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyVersions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldPreferEarlyVersion_minorDiff_earlyVersions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyVersions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferEarlyVersion_majorDiff_earlyVersions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyVersions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferRecentSamples() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(0L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameSample() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldPreferRecentSample_minorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferRecentSample_majorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferEarlySamplesIfEarlySamples() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(0L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameSample_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldPreferEarlySample_minorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferEarlySample_majorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferRecentDemos() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(0L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameDemo() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldPreferRecentDemo_minorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .demo(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferRecentDemo_majorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .demo(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferEarlyDemosIfEarlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameDemo_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldPreferEarlyDemo_minorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .demo(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferEarlyDemo_majorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .demo(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferRecentBetas() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(0L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameBeta() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldPreferRecentBeta_minorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferRecentBeta_majorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferEarlyBetasIfEarlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameBeta_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldPreferEarlyBeta_minorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferEarlyBeta_majorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferRecentProtos() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(0L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameProto() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldPreferRecentProto_minorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferRecentProto_majorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferEarlyProtosIfEarlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldKeepOrderIfSameProto_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    void testCompare_shouldPreferEarlyProto_minorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferEarlyProto_majorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    void testCompare_shouldPreferParents_ifAllElseIsEqual() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getByCode("USA"))
                .parent(true)
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertArrayEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    private Game createGame(String s) {
        return Game.builder()
                .name(s)
                .description(s)
                .build();
    }

    private RegionData getByCode(String code, String... codes) {
        List<String> codeList = Arrays.asList(codes);
        return RegionData.builder().regions(regionData.getRegions()
                .stream()
                .filter(r -> code.equals(r.getCode()) || codeList.contains(r.getCode()))
                .collect(ImmutableSet.toImmutableSet()))
                .build();
    }
}