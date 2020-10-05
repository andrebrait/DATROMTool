package io.github.datromtool.command;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.GameParser;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.data.FileInputOutput;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.data.TextOutput;
import io.github.datromtool.domain.datafile.Datafile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public final class OneGameOneRom {

    private final Filter filter;
    private final PostFilter postFilter;
    private final SortingPreference sortingPreference;

    public void generate(
            @Nonnull Collection<Datafile> datafiles,
            @Nullable FileInputOutput fileInputOutput,
            @Nullable TextOutput textOutput,
            @Nonnull Consumer<? extends Collection<String>> resultConsumer) throws Exception {
        GameParser gameParser = new GameParser(
                SerializationHelper.getInstance().loadRegionData(),
                GameParser.DivergenceDetection.ONE_WAY);
        ImmutableList<ParsedGame> parsedGames = datafiles.stream()
                .map(gameParser::parse)
                .flatMap(Collection::stream)
                .collect(ImmutableList.toImmutableList());
        if (parsedGames.isEmpty()) {
            throw new CommandLine.ExecutionException(
                    commandSpec.commandLine(),
                    "Cannot generate 1G1R set. Reason: DAT files contain no valid entries");
        }
        if (parsedGames.stream().allMatch(ParsedGame::isParent)) {
            throw new CommandLine.ExecutionException(
                    commandSpec.commandLine(),
                    "Cannot generate 1G1R set. Reason: DAT files lack Parent/Clone information");
        }
    }

}
