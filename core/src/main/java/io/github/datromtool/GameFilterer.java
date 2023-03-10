package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.Pair;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.domain.datafile.logiqx.Game;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public final class GameFilterer {

    @NonNull
    private final Filter filter;

    @NonNull
    private final PostFilter postFilter;

    public ImmutableList<ParsedGame> filter(Collection<ParsedGame> input) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Starting filtering {} with {}",
                    input.stream()
                            .map(ParsedGame::getGame)
                            .map(Game::getName)
                            .collect(Collectors.toList()),
                    filter);
        }
        ImmutableList<ParsedGame> result = input.stream()
                .filter(this::filterBioses)
                .filter(this::filterProto)
                .filter(this::filterBeta)
                .filter(this::filterDemo)
                .filter(this::filterSample)
                .filter(this::filterIncludeRegion)
                .filter(this::filterIncludeLanguage)
                .filter(this::filterExcludeRegion)
                .filter(this::filterExcludeLanguage)
                .filter(this::filterExcludes)
                .collect(ImmutableList.toImmutableList());
        if (log.isDebugEnabled()) {
            log.debug(
                    "Finished filtering {}",
                    result.stream()
                            .map(ParsedGame::getGame)
                            .map(Game::getName)
                            .collect(Collectors.toList()));
        }
        return result;
    }

    private boolean filterBioses(ParsedGame p) {
        boolean result = filter.isAllowBios() || !p.isBios();
        if (!result) {
            log.debug("BIOS filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterProto(ParsedGame p) {
        boolean result = filter.isAllowProto() || p.getProto().isEmpty();
        if (!result) {
            log.debug("Proto filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterBeta(ParsedGame p) {
        boolean result = filter.isAllowBeta() || p.getBeta().isEmpty();
        if (!result) {
            log.debug("Beta filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterDemo(ParsedGame p) {
        boolean result = filter.isAllowDemo() || p.getDemo().isEmpty();
        if (!result) {
            log.debug("Demo filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterSample(ParsedGame p) {
        boolean result = filter.isAllowSample() || p.getSample().isEmpty();
        if (!result) {
            log.debug("Sample filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterIncludeRegion(ParsedGame p) {
        boolean result = containsAny(p::getRegionsStream, filter.getIncludeRegions());
        if (!result) {
            log.debug("Region filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterIncludeLanguage(ParsedGame p) {
        boolean result = containsAny(p::getLanguagesStream, filter.getIncludeLanguages());
        if (!result) {
            log.debug("Language filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterExcludeRegion(ParsedGame p) {
        boolean result = containsNone(p::getRegionsStream, filter.getExcludeRegions());
        if (!result) {
            log.debug("Region exclusion filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterExcludeLanguage(ParsedGame p) {
        boolean result = containsNone(p::getLanguagesStream, filter.getExcludeLanguages());
        if (!result) {
            log.debug("Language exclusion filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private static boolean containsNone(
            Supplier<Stream<String>> streamSupplier,
            Collection<String> filter) {
        return filter.isEmpty() || streamSupplier.get().noneMatch(filter::contains);
    }

    private static boolean containsAny(
            Supplier<Stream<String>> streamSupplier,
            Collection<String> filter) {
        return filter.isEmpty() || streamSupplier.get().anyMatch(filter::contains);
    }

    private boolean filterExcludes(ParsedGame p) {
        boolean result = filter.getExcludes().stream()
                .map(e -> e.matcher(p.getGame().getName()))
                .noneMatch(Matcher::find);
        result |= filter.getIncludes().stream()
                .map(e -> e.matcher(p.getGame().getName()))
                .anyMatch(Matcher::find);
        if (!result) {
            log.debug("Excludes filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    public ImmutableList<ParsedGame> postFilter(Collection<ParsedGame> input) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Starting post-filtering {} with {}",
                    input.stream()
                            .map(ParsedGame::getGame)
                            .map(Game::getName)
                            .collect(Collectors.toList()),
                    filter);
        }
        for (Pattern exclude : postFilter.getExcludes()) {
            if (input.stream()
                    .map(ParsedGame::getGame)
                    .map(Game::getName)
                    .map(exclude::matcher)
                    .anyMatch(Matcher::find)) {
                if (log.isDebugEnabled()) {
                    log.debug(
                            "Post-filter exclusions removed {}",
                            input.stream()
                                    .map(ParsedGame::getGame)
                                    .map(Game::getName)
                                    .collect(Collectors.toList()));
                }
                return ImmutableList.of();
            }
        }
        return ImmutableList.copyOf(input);
    }

    public ImmutableMap<String, ImmutableList<ParsedGame>> postFilter(
            Map<String, ? extends Collection<ParsedGame>> input) {
        return input.entrySet().stream()
                .map(e -> Pair.of(e.getKey(), postFilter(e.getValue())))
                .filter(p -> !p.getRight().isEmpty())
                .collect(ImmutableMap.toImmutableMap(Pair::getLeft, Pair::getRight));
    }

}
