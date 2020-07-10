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
import java.util.stream.Stream;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@RequiredArgsConstructor
public final class GameFilterer {

    private final static Logger logger = LoggerFactory.getLogger(GameFilterer.class);

    @NonNull
    private final Filter filter;

    @NonNull
    private final PostFilter postFilter;

    public ImmutableList<ParsedGame> filter(Collection<ParsedGame> input) {
        return input.stream()
                .filter(p -> !filter.isNoProto() || isEmpty(p.getProto()))
                .filter(p -> !filter.isNoBeta() || isEmpty(p.getBeta()))
                .filter(p -> !filter.isNoDemo() || isEmpty(p.getDemo()))
                .filter(p -> !filter.isNoSample() || isEmpty(p.getSample()))
                .filter(p -> containsAny(p::getRegionsStream, filter.getRegions()))
                .filter(p -> containsAny(p::getLanguagesStream, filter.getLanguages()))
                .filter(p -> filter.getExcludes().stream()
                        .map(e -> e.matcher(p.getGame().getName()))
                        .noneMatch(Matcher::find))
                .collect(ImmutableList.toImmutableList());
    }

    public ImmutableList<ParsedGame> postFilter(Collection<ParsedGame> input) {
        for (Pattern exclude : postFilter.getExcludes()) {
            if (input.stream()
                    .map(ParsedGame::getGame)
                    .map(Game::getName)
                    .map(exclude::matcher)
                    .anyMatch(Matcher::find)) {
                return ImmutableList.of();
            }
        }
        return ImmutableList.copyOf(input);
    }

    private static boolean containsAny(
            Supplier<Stream<String>> streamSupplier,
            Collection<String> filter) {
        return isEmpty(filter) || streamSupplier.get().anyMatch(filter::contains);
    }

}
