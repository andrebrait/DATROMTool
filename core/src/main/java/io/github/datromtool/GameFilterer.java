package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.ParsedGame;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RequiredArgsConstructor
public final class GameFilterer {

    private final static Logger logger = LoggerFactory.getLogger(GameFilterer.class);

    @NonNull
    private final Filter filter;

    public ImmutableList<ParsedGame> filter(List<ParsedGame> input) {
        return input.stream()
                .filter(p -> {
                    switch (filter.getMode()) {
                        case DEFAULT:
                            return p.containsAnyRegion(filter.getRegions());
                        case ONLY_WITH_LANG:
                            return p.containsAnyRegion(filter.getRegions())
                                    && p.containsAnyLanguage(filter.getLanguages());
                        case ALL_WITH_LANG:
                            return p.containsAnyRegion(filter.getRegions())
                                    || p.containsAnyLanguage(filter.getLanguages());
                        case ALL_REGIONS:
                        default:
                            return true;
                    }
                }).collect(ImmutableList.toImmutableList());
    }



}
