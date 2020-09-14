package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.domain.datafile.Game;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public final class GameFilterer {

    private final static Logger logger = LoggerFactory.getLogger(GameFilterer.class);

    @NonNull
    private final Filter filter;

    @NonNull
    private final PostFilter postFilter;

    public ImmutableList<ParsedGame> filter(Collection<ParsedGame> input) {
        if (logger.isDebugEnabled()) {
            logger.debug(
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
        if (logger.isDebugEnabled()) {
            logger.debug(
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
            logger.debug("BIOS filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterProto(ParsedGame p) {
        boolean result = filter.isAllowProto() || p.getProto().isEmpty();
        if (!result) {
            logger.debug("Proto filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterBeta(ParsedGame p) {
        boolean result = filter.isAllowBeta() || p.getBeta().isEmpty();
        if (!result) {
            logger.debug("Beta filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterDemo(ParsedGame p) {
        boolean result = filter.isAllowDemo() || p.getDemo().isEmpty();
        if (!result) {
            logger.debug("Demo filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterSample(ParsedGame p) {
        boolean result = filter.isAllowSample() || p.getSample().isEmpty();
        if (!result) {
            logger.debug("Sample filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterIncludeRegion(ParsedGame p) {
        boolean result = containsAny(p::getRegionsStream, filter.getIncludeRegions());
        if (!result) {
            logger.debug("Region filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterIncludeLanguage(ParsedGame p) {
        boolean result = containsAny(p::getLanguagesStream, filter.getIncludeLanguages());
        if (!result) {
            logger.debug("Language filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterExcludeRegion(ParsedGame p) {
        boolean result = containsNone(p::getRegionsStream, filter.getExcludeRegions());
        if (!result) {
            logger.debug("Region exclusion filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    private boolean filterExcludeLanguage(ParsedGame p) {
        boolean result = containsNone(p::getLanguagesStream, filter.getExcludeLanguages());
        if (!result) {
            logger.debug("Language exclusion filter removed '{}'", p.getGame().getName());
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
        if (!result) {
            logger.debug("Excludes filter removed '{}'", p.getGame().getName());
        }
        return result;
    }

    public ImmutableList<ParsedGame> postFilter(Collection<ParsedGame> input) {
        if (logger.isDebugEnabled()) {
            logger.debug(
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
                if (logger.isDebugEnabled()) {
                    logger.debug(
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

}
