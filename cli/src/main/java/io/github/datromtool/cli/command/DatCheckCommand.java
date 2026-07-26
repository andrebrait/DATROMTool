package io.github.datromtool.cli.command;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.GameParser;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.cli.GitVersionProvider;
import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.converter.DatafileConverter;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.domain.datafile.logiqx.Game;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import static java.lang.String.format;

/**
 * Issue #18: surfaces {@link GameParser}'s divergence detection (previously only visible as a
 * {@code log.warn}, invisible to a normal run) as a standalone report. Detection logic stays in
 * core: this command only runs {@link GameParser#parse} per DAT (in
 * {@link GameParser.DivergenceDetection#ALWAYS}, the strictest mode, so name-implied metadata is
 * compared against DAT-declared metadata unconditionally) and formats
 * {@link ParsedGame#getDivergences()} it already collects, grouped per input DAT. Exit code
 * mirrors {@code scan}'s convention: 0 when every DAT is clean, 1 when any divergence is found.
 */
@CommandLine.Command(
        name = "check",
        description = "Report divergences between No-Intro parsed names and DAT-declared "
                + "region/language metadata",
        sortOptions = false,
        abbreviateSynopsis = true,
        versionProvider = GitVersionProvider.class,
        mixinStandardHelpOptions = true)
public final class DatCheckCommand implements Callable<Integer> {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec commandSpec;

    @CommandLine.Parameters(
            description = "DAT file(s) to check for divergences",
            arity = "1..*",
            paramLabel = "DAT_FILE",
            converter = DatafileConverter.class)
    private List<DatafileArgument> datafiles;

    @Override
    public Integer call() {
        GameParser gameParser;
        try {
            gameParser = new GameParser(
                    SerializationHelper.getInstance().loadRegionData(),
                    GameParser.DivergenceDetection.ALWAYS);
        } catch (IOException e) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    format("Could not load region data: %s", e.getMessage()));
        }

        boolean anyDivergence = false;
        for (DatafileArgument datafileArgument : datafiles) {
            ImmutableList<ParsedGame> parsedGames = gameParser.parse(datafileArgument.getDatafile());
            List<ParsedGame> divergent = parsedGames.stream()
                    .filter(pg -> !pg.getDivergences().isEmpty())
                    .toList();
            if (divergent.isEmpty()) {
                System.out.printf("%s: no divergences%n", datafileArgument.getPath());
                continue;
            }
            anyDivergence = true;
            System.out.printf("%s: %d game(s) with divergences%n",
                    datafileArgument.getPath(), divergent.size());
            for (ParsedGame parsedGame : divergent) {
                Game game = parsedGame.getGame();
                for (ParsedGame.Divergence divergence : parsedGame.getDivergences()) {
                    System.out.printf(
                            "  %s: %s divergence: detected=%s, provided=%s%n",
                            game.getName(),
                            divergence.field(),
                            divergence.detected(),
                            divergence.provided());
                }
            }
        }
        return anyDivergence ? 1 : 0;
    }
}
