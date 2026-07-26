package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.domain.datafile.logiqx.Game;
import io.github.datromtool.sorting.GameComparator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public final class GameSorter {

    @NonNull
    private final GameComparator comparator;

    public ImmutableMap<String, ImmutableList<ParsedGame>> sortAndGroupByParent(
            Collection<ParsedGame> parsedGames) {
        return groupByParent(parsedGames)
                .entrySet()
                .stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(ImmutableMap.toImmutableMap(
                        Map.Entry::getKey,
                        this::toSortedCandidatesList));
    }

    private static Map<String, List<ParsedGame>> groupByParent(Collection<ParsedGame> parsedGames) {
        return parsedGames.stream()
                .collect(Collectors.groupingBy(
                        GameSorter::groupKey,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    /**
     * Grouping key for 1G1R candidate sets (issue #19 step 2): a Retool clone list match
     * ({@link ParsedGame#getClonelistGroup()}) takes precedence over the DAT-declared
     * parent/clone relationship ({@link ParsedGame#getParentName()}) when present, so a clone
     * list can unify games across DAT parent/clone boundaries the DAT itself does not declare
     * (e.g. the Atari 2600 "Air Raiders"/"Bogey Blaster"/"Top Gun" cross-region-rename group).
     * Absent a clone list match - including every run without clone list data at all, since
     * {@link ParsedGame#getClonelistGroup()} is then always {@code null} - this is byte-identical
     * to grouping by {@link ParsedGame#getParentName()} alone.
     */
    private static String groupKey(ParsedGame parsedGame) {
        String clonelistGroup = parsedGame.getClonelistGroup();
        return clonelistGroup != null ? clonelistGroup : parsedGame.getParentName();
    }

    private static void logBeforeSorting(Map.Entry<String, ? extends Collection<ParsedGame>> e) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Sorting entries for '{}': {}",
                    e.getKey(),
                    toGamesList(e));
        }
    }

    private static void logAfterSorting(Map.Entry<String, ? extends Collection<ParsedGame>> e) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Finished sorting entries for '{}': {}",
                    e.getKey(),
                    toGamesList(e));
        }
    }

    private static List<String> toGamesList(Map.Entry<String, ? extends Collection<ParsedGame>> e) {
        return e.getValue()
                .stream()
                .map(ParsedGame::getGame)
                .map(Game::getName)
                .toList();
    }

    private ImmutableList<ParsedGame> toSortedCandidatesList(Map.Entry<String, List<ParsedGame>> e) {
        logBeforeSorting(e);
        e.getValue().sort(comparator);
        logAfterSorting(e);
        return ImmutableList.copyOf(e.getValue());
    }
}
