package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.SortingPreference;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public final class GameSorter {

    private final static Logger logger = LoggerFactory.getLogger(GameFilterer.class);

    @NonNull
    private final SortingPreference sortingPreference;

    // TODO: log in each step what was changed and why

    public ImmutableMap<String, ImmutableList<ParsedGame>> sortAndGroupByParent(
            Collection<ParsedGame> parsedGames) {
        GameComparator comparator = new GameComparator(sortingPreference);
        Map<String, List<ParsedGame>> groupedByParent = parsedGames.stream()
                .collect(Collectors.groupingBy(
                        p -> p.isParent()
                                ? p.getGame().getName()
                                : StringUtils.defaultIfBlank(
                                        p.getGame().getCloneOf(),
                                        p.getGame().getRomOf()),
                        LinkedHashMap::new,
                        Collectors.toList()));
        return groupedByParent.entrySet().stream()
                .filter(e -> CollectionUtils.isNotEmpty(e.getValue()))
                .peek(e -> e.getValue().sort(comparator))
                .collect(ImmutableMap.toImmutableMap(
                        Map.Entry::getKey,
                        e -> ImmutableList.copyOf(e.getValue())));
    }

}
