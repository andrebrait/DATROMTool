package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.domain.datafile.Game;
import io.github.datromtool.sorting.GameComparator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public final class GameSorter {

    private final static Logger logger = LoggerFactory.getLogger(GameSorter.class);

    @NonNull
    private final GameComparator comparator;

    public ImmutableMap<String, ImmutableList<ParsedGame>> sortAndGroupByParent(
            Collection<ParsedGame> parsedGames) {
        Map<String, List<ParsedGame>> groupedByParent = parsedGames.stream()
                .collect(Collectors.groupingBy(
                        ParsedGame::getParentName,
                        LinkedHashMap::new,
                        Collectors.toList()));
        return groupedByParent.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .peek(e -> sortList(comparator, e))
                .collect(ImmutableMap.toImmutableMap(
                        Map.Entry::getKey,
                        e -> ImmutableList.copyOf(e.getValue())));
    }

    private void sortList(GameComparator comparator, Map.Entry<String, List<ParsedGame>> e) {
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Sorting entries for '{}': {}",
                    e.getKey(),
                    e.getValue()
                            .stream()
                            .map(ParsedGame::getGame)
                            .map(Game::getName)
                            .collect(Collectors.toList()));
        }
        e.getValue().sort(comparator);
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Finished sorting entries for '{}': {}",
                    e.getKey(),
                    e.getValue()
                            .stream()
                            .map(ParsedGame::getGame)
                            .map(Game::getName)
                            .collect(Collectors.toList()));
        }
    }

}
