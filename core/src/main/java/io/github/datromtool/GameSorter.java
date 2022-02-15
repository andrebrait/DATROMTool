package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.domain.datafile.Game;
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
                        ParsedGame::getParentName,
                        LinkedHashMap::new,
                        Collectors.toList()));
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
                .collect(Collectors.toList());
    }

    private ImmutableList<ParsedGame> toSortedCandidatesList(Map.Entry<String, List<ParsedGame>> e) {
        logBeforeSorting(e);
        e.getValue().sort(comparator);
        logAfterSorting(e);
        return ImmutableList.copyOf(e.getValue());
    }
}
