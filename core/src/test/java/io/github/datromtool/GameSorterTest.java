package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.domain.datafile.logiqx.Game;
import io.github.datromtool.sorting.GameComparator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #19 step 2: {@link GameSorter#sortAndGroupByParent} groups by
 * {@link ParsedGame#getClonelistGroup()} when a clone list assigned one, falling back to the
 * DAT-declared parent/clone relationship ({@link ParsedGame#getParentName()}) otherwise - so a
 * run without clone list data (every {@link ParsedGame#getClonelistGroup()} is {@code null})
 * groups exactly as it did before issue #19.
 *
 * <p>Review round: a clone-list-derived key is namespaced with a {@code "clonelist:"} prefix
 * (see {@code GameSorter}'s private {@code CLONELIST_GROUP_KEY_PREFIX}), so every {@code
 * clonelistGroup}-driven expected key below is {@code "clonelist:<group name>"}, not the bare
 * group name - proving this test still pins the exact same grouping *behavior* (same games
 * unified, same games kept apart), just against the corrected, collision-proof key shape.
 */
class GameSorterTest {

    private static final RegionData EMPTY_REGION_DATA = new RegionData(ImmutableSet.of());

    private static GameSorter defaultSorter() {
        return new GameSorter(new GameComparator(SortingPreference.builder().build()));
    }

    private static ParsedGame game(String name, String cloneOf) {
        return ParsedGame.builder()
                .game(Game.builder().name(name).description(name).cloneOf(cloneOf).build())
                .regionData(EMPTY_REGION_DATA)
                .build();
    }

    private static ParsedGame parentGame(String name) {
        return ParsedGame.builder()
                .game(Game.builder().name(name).description(name).build())
                .regionData(EMPTY_REGION_DATA)
                .parent(true)
                .build();
    }

    // Oracle: no clone list in play (every ParsedGame#getClonelistGroup() null) groups by DAT
    // parent/clone exactly as before issue #19.
    @Test
    void groupsByParentNameWhenNoClonelistGroupAssigned() {
        ParsedGame parent = parentGame("Some Game (USA)");
        ParsedGame clone = game("Some Game (Europe)", "Some Game (USA)");

        ImmutableMap<String, ImmutableList<ParsedGame>> grouped =
                defaultSorter().sortAndGroupByParent(ImmutableList.of(parent, clone));

        assertEquals(1, grouped.size());
        assertTrue(grouped.containsKey("Some Game (USA)"));
        assertEquals(2, grouped.get("Some Game (USA)").size());
    }

    // Two games with unrelated DAT parent/clone data (different names, no cloneOf relating them)
    // are unified when a clone list assigns them the same group - the Atari 2600 "Air Raiders"
    // shape from issue #19's own acceptance example.
    @Test
    void clonelistGroupUnifiesGamesWithNoDatParentCloneRelationship() {
        ParsedGame airRaiders = parentGame("Air Raiders (USA)").toBuilder()
                .clonelistGroup("Air Raiders")
                .build();
        ParsedGame bogeyBlaster = parentGame("Bogey Blaster (Europe)").toBuilder()
                .clonelistGroup("Air Raiders")
                .build();
        ParsedGame topGun = parentGame("Top Gun (Germany)").toBuilder()
                .clonelistGroup("Air Raiders")
                .build();

        ImmutableMap<String, ImmutableList<ParsedGame>> grouped = defaultSorter()
                .sortAndGroupByParent(ImmutableList.of(airRaiders, bogeyBlaster, topGun));

        assertEquals(1, grouped.size());
        assertTrue(grouped.containsKey("clonelist:Air Raiders"));
        List<ParsedGame> group = grouped.get("clonelist:Air Raiders");
        assertEquals(3, group.size());
        assertTrue(group.containsAll(List.of(airRaiders, bogeyBlaster, topGun)));
    }

    // A clone list can also override *within* an existing DAT parent/clone family - the group
    // key it assigns wins over that family's own parent name.
    @Test
    void clonelistGroupOverridesExistingDatParentCloneFamily() {
        ParsedGame parent = parentGame("Some Game (USA)").toBuilder()
                .clonelistGroup("Renamed Group")
                .build();
        ParsedGame clone = game("Some Game (Europe)", "Some Game (USA)").toBuilder()
                .clonelistGroup("Renamed Group")
                .build();

        ImmutableMap<String, ImmutableList<ParsedGame>> grouped =
                defaultSorter().sortAndGroupByParent(ImmutableList.of(parent, clone));

        assertEquals(1, grouped.size());
        assertTrue(grouped.containsKey("clonelist:Renamed Group"));
        assertEquals(2, grouped.get("clonelist:Renamed Group").size());
    }

    // Games the clone list didn't match (clonelistGroup null) keep grouping by their own DAT
    // parent/clone relationship, even in a run where *other* games did get a clone list group.
    @Test
    void unmatchedGamesKeepDatParentCloneGrouping() {
        ParsedGame matchedParent = parentGame("Air Raiders (USA)").toBuilder()
                .clonelistGroup("Air Raiders")
                .build();
        ParsedGame matchedClone = parentGame("Bogey Blaster (Europe)").toBuilder()
                .clonelistGroup("Air Raiders")
                .build();
        ParsedGame unmatchedParent = parentGame("Unrelated Game (USA)");
        ParsedGame unmatchedClone = game("Unrelated Game (Europe)", "Unrelated Game (USA)");

        ImmutableMap<String, ImmutableList<ParsedGame>> grouped = defaultSorter().sortAndGroupByParent(
                ImmutableList.of(matchedParent, matchedClone, unmatchedParent, unmatchedClone));

        assertEquals(2, grouped.size());
        assertEquals(2, grouped.get("clonelist:Air Raiders").size());
        assertEquals(2, grouped.get("Unrelated Game (USA)").size());
    }
}
