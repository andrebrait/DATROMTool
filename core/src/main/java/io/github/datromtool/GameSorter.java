package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.SortingPreference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
public class GameSorter {

    private final static Logger logger = LoggerFactory.getLogger(GameFilterer.class);

    @NonNull
    SortingPreference sortingPreference;

    @NonNull
    @Builder.Default
    ImmutableList<Pattern> excludes = ImmutableList.of();

    public ImmutableMap<ParsedGame, ImmutableList<ParsedGame>> sortAndGroupByParents(
            Collection<ParsedGame> parsedGames) {
        GameComparator comparator = GameComparator.builder()
                .sortingPreference(sortingPreference)
                .build();
        Map<String, ParsedGame> parentsMap = createParentsMap(parsedGames);
        Map<ParsedGame, List<ParsedGame>> groupedByParent = groupByParents(parsedGames, parentsMap);
        return groupedByParent.entrySet().stream()
                .filter(e -> CollectionUtils.isNotEmpty(e.getValue()))
                .peek(e -> e.getValue().sort(comparator))
                .filter(e -> excludes.stream()
                        .map(p -> p.matcher(e.getValue().get(0).getGame().getName()))
                        .noneMatch(Matcher::find))
                .collect(ImmutableMap.toImmutableMap(
                        Map.Entry::getKey,
                        e -> ImmutableList.copyOf(e.getValue())));
    }

    private static Map<ParsedGame, List<ParsedGame>> groupByParents(
            Collection<ParsedGame> parsedGames,
            Map<String, ParsedGame> parentsMap) {
        return parsedGames.stream()
                .collect(Collectors.groupingBy(g ->
                        parentsMap.getOrDefault(g.getGame().getCloneOf(), g)));
    }

    private static Map<String, ParsedGame> createParentsMap(Collection<ParsedGame> parsedGames) {
        return parsedGames.stream()
                .filter(ParsedGame::isParent)
                .collect(Collectors.toMap(
                        g -> g.getGame().getName(),
                        Function.identity(),
                        (i, j) -> {
                            logger.warn("Multiple games with the same name found: [{}, {}]", i, j);
                            return i;
                        }));
    }

}
