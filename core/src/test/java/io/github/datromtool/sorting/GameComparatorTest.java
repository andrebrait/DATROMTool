package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.data.SortingPreference;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Pattern;

import static io.github.datromtool.util.TestUtils.createGame;
import static io.github.datromtool.util.TestUtils.getRegionByCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.testng.Assert.assertEquals;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class GameComparatorTest {

    static RegionData regionData;

    @BeforeClass
    public static void beforeAll() throws Exception {
        regionData = SerializationHelper.getInstance()
                .loadRegionData(Paths.get(ClassLoader.getSystemResource("region-data.yaml")
                        .toURI()));
    }

    @Test
    public void testCompare_stopAtFirstNonZero() {
        SubComparatorProvider mock = mock(SubComparatorProvider.class);
        ImmutableList<SubComparator> mocks = ImmutableList.of(
                mock(SubComparator.class),
                mock(SubComparator.class),
                mock(SubComparator.class));
        given(mock.toList(any())).willReturn(mocks);
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .bad(true)
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        given(mocks.get(0).compare(tg1, tg2)).willReturn(-1);
        given(mocks.get(0).compare(tg2, tg1)).willReturn(1);

        GameComparator comparator = new GameComparator(SortingPreference.builder().build(), mock);
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
        verify(mocks.get(0)).compare(any(), any());
        verifyNoInteractions(mocks.get(1), mocks.get(2));
    }

    @Test
    public void testCompare_stopAtFirstNonZero_2() {
        SubComparatorProvider mock = mock(SubComparatorProvider.class);
        ImmutableList<SubComparator> mocks = ImmutableList.of(
                mock(SubComparator.class),
                mock(SubComparator.class),
                mock(SubComparator.class));
        given(mock.toList(any())).willReturn(mocks);
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .bad(true)
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        given(mocks.get(0).compare(any(), any())).willReturn(0);
        given(mocks.get(1).compare(tg1, tg2)).willReturn(-1);
        given(mocks.get(1).compare(tg2, tg1)).willReturn(1);

        GameComparator comparator = new GameComparator(SortingPreference.builder().build(), mock);
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
        verify(mocks.get(0)).compare(any(), any());
        verify(mocks.get(1)).compare(any(), any());
        verifyNoInteractions(mocks.get(2));
    }

    @Test
    public void testCompare_stopAtFirstNonZero_3() {
        SubComparatorProvider mock = mock(SubComparatorProvider.class);
        ImmutableList<SubComparator> mocks = ImmutableList.of(
                mock(SubComparator.class),
                mock(SubComparator.class),
                mock(SubComparator.class));
        given(mock.toList(any())).willReturn(mocks);
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .bad(true)
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        given(mocks.get(0).compare(any(), any())).willReturn(0);
        given(mocks.get(1).compare(any(), any())).willReturn(0);
        given(mocks.get(2).compare(tg1, tg2)).willReturn(-1);
        given(mocks.get(2).compare(tg2, tg1)).willReturn(1);

        GameComparator comparator = new GameComparator(SortingPreference.builder().build(), mock);
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
        verify(mocks.get(0)).compare(any(), any());
        verify(mocks.get(1)).compare(any(), any());
        verify(mocks.get(2)).compare(any(), any());
    }

    @Test
    public void testCompare_shouldPreventBadDumps() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("USA", "EUR"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .bad(true)
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldNotPreventPrereleasesIfNotPreferPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("USA", "EUR"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(1L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferPrereleaseIfPreferPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("USA", "EUR"))
                .preferPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .beta(ImmutableList.of(1L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldSortByRegion() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("USA", "EUR"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldKeepOrderByRegion() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("EUR", "USA"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameRegions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("EUR", "USA"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameRegions_differentNumberOfRegions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("EUR", "USA"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "EUR"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameRegions_sameNumberOfRegions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("EUR", "USA"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "EUR"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldSortByLanguage() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
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
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldKeepOrderByLanguage() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("ja", "en"))
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
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameLanguages() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("jp", "en"))
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
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameLanguages_sameNumberOfLanguages() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("ja", "en"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "JPN"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "JPN"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldPreferWithMostSelectedLanguages() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("ja", "en"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "JPN"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferWithMostLanguages() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("ja", "en"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "JPN"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "JPN", "BRA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferWithMostSelectedLanguagesOverMostLanguages() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .languages(ImmutableSet.of("ja", "en"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "BRA", "ESP", "POL"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA", "JPN"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldSortByRegionAndLanguage() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("USA", "EUR"))
                .languages(ImmutableSet.of("ja", "en"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame tg3 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .languages(ImmutableSet.of("ja"))
                .game(createGame("Test game 3"))
                .build();
        ParsedGame tg4 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .languages(ImmutableSet.of("ja"))
                .game(createGame("Test game 4"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2, tg3, tg4};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg4, tg2, tg3, tg1});
    }

    @Test
    public void testCompare_shouldSortByRegionAndLanguage_prioritizeLanguages() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("USA", "EUR"))
                .languages(ImmutableSet.of("ja", "en"))
                .prioritizeLanguages(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame tg3 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "EUR"))
                .languages(ImmutableSet.of("ja"))
                .game(createGame("Test game 3"))
                .build();
        ParsedGame tg4 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .languages(ImmutableSet.of("ja"))
                .game(createGame("Test game 4"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2, tg3, tg4};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg4, tg3, tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferReleaseIfAllElseIsEqual() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .regions(ImmutableSet.of("USA", "EUR"))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(1L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferParents_ifPreferParents() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .preferParents(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .parent(true)
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferRecentRevisions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferRecentRevisions_ignoreParent() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .parent(true)
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameRevision() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldPreferRecentRevision_minorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferRecentRevision_majorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferEarlyRevisionsIfEarlyRevisions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyRevisions(true)
                .build());
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
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameRevision_earlyRevisions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyRevisions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldPreferEarlyRevision_minorDiff_earlyRevisions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyRevisions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferEarlyRevision_majorDiff_earlyRevisions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyRevisions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .revision(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferRecentVersions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferRecentVersions_ignoreParent() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .parent(true)
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameVersion() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldPreferRecentVersion_minorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferRecentVersion_majorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferEarlyVersionsIfEarlyVersions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyVersions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameVersion_earlyVersions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyVersions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldPreferEarlyVersion_minorDiff_earlyVersions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyVersions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferEarlyVersion_majorDiff_earlyVersions() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyVersions(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .version(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferRecentSamples() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(0L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameSample() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldPreferRecentSample_minorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferRecentSample_majorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferEarlySamplesIfEarlySamples() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(0L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameSample_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldPreferEarlySample_minorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferEarlySample_majorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .sample(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferRecentDemos() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(0L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameDemo() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldPreferRecentDemo_minorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .demo(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferRecentDemo_majorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .demo(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferEarlyDemosIfEarlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameDemo_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldPreferEarlyDemo_minorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .demo(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferEarlyDemo_majorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .demo(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .demo(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferRecentBetas() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(0L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameBeta() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldPreferRecentBeta_minorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferRecentBeta_majorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferEarlyBetasIfEarlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameBeta_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldPreferEarlyBeta_minorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferEarlyBeta_majorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .beta(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferRecentProtos() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(0L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameProto() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldPreferRecentProto_minorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferRecentProto_majorDiff() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferEarlyProtosIfEarlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldKeepOrderIfSameProto_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg1, tg2});
    }

    @Test
    public void testCompare_shouldPreferEarlyProto_minorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(1L, 3L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferEarlyProto_majorDiff_earlyPrereleases() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .earlyPrereleases(true)
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(2L, 2L))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .proto(ImmutableList.of(1L, 2L))
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldAvoid_ifInAvoidsList() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .avoids(ImmutableSet.of(Pattern.compile("(?i)game 1")))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .parent(true)
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPrefer_ifInPrefersList() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .prefers(ImmutableSet.of(Pattern.compile("(?i)game 2")))
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .parent(true)
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }

    @Test
    public void testCompare_shouldPreferParents_ifAllElseIsEqual() {
        GameComparator comparator = new GameComparator(SortingPreference.builder()
                .build());
        ParsedGame tg1 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .game(createGame("Test game 1"))
                .build();
        ParsedGame tg2 = ParsedGame.builder()
                .regionData(getRegionByCode(regionData, "USA"))
                .parent(true)
                .game(createGame("Test game 2"))
                .build();
        ParsedGame[] parsedGames = new ParsedGame[]{tg1, tg2};
        Arrays.sort(parsedGames, comparator);
        assertEquals(parsedGames, new ParsedGame[]{tg2, tg1});
    }
}