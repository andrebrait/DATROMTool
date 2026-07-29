package io.github.datromtool.cli.command;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.GameParser;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.cli.GitVersionProvider;
import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.converter.DatafileConverter;
import io.github.datromtool.cli.converter.DivergenceDetectionConverter;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.domain.datafile.logiqx.Game;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import static java.lang.String.format;

import static io.github.datromtool.cli.util.TerminalUtils.sanitizeForTerminal;

/**
 * Issue #18: surfaces {@link GameParser}'s divergence detection (previously only visible as a
 * {@code log.warn}, invisible to a normal run) as a standalone report. Detection logic stays in
 * core: this command only runs {@link GameParser#parse} per DAT and formats
 * {@link ParsedGame#getDivergences()} it already collects, grouped per input DAT. Exit code
 * mirrors {@code scan}'s convention: 0 when every DAT is clean, 1 when any divergence is found.
 *
 * <p>Correction round: {@code --divergence} defaults to
 * {@link GameParser.DivergenceDetection#ONE_WAY}, not {@link GameParser.DivergenceDetection#ALWAYS}.
 * A verifier probe found {@code ALWAYS} (the previously hardcoded mode) flags 5/5 games on a
 * release-less DAT (name implies a region, zero {@code <release>} elements at all) — the common
 * real-world No-Intro DAT shape, since {@code ALWAYS} compares name-detected metadata against
 * DAT-declared metadata even when the DAT declares none at all. {@code ONE_WAY} (mirroring
 * {@code 1g1r}'s own default) only compares when both sides are non-empty, so a release-less DAT
 * reports clean by default; {@code --divergence always} remains available as an explicit,
 * stricter opt-in.
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

    @CommandLine.Option(
            names = "--divergence",
            paramLabel = "MODE",
            converter = DivergenceDetectionConverter.class,
            completionCandidates = DivergenceDetectionConverter.class,
            description = "How strictly to flag divergences between No-Intro parsed names and "
                    + "DAT-declared region/language metadata. Options: ${COMPLETION-CANDIDATES} "
                    + "(default: one_way).")
    private GameParser.DivergenceDetection divergenceDetection = GameParser.DivergenceDetection.ONE_WAY;

    @Override
    public Integer call() {
        GameParser gameParser;
        try {
            gameParser = new GameParser(
                    SerializationHelper.getInstance().loadRegionData(),
                    divergenceDetection);
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
                System.out.printf("%s: no divergences%n", sanitizeForTerminal(datafileArgument.getPath().toString()));
                continue;
            }
            anyDivergence = true;
            System.out.printf("%s: %d game(s) with divergences%n",
                    sanitizeForTerminal(datafileArgument.getPath().toString()), divergent.size());
            for (ParsedGame parsedGame : divergent) {
                Game game = parsedGame.getGame();
                for (ParsedGame.Divergence divergence : parsedGame.getDivergences()) {
                    System.out.printf(
                            "  %s: %s divergence: detected=%s, provided=%s%n",
                            sanitizeForTerminal(game.getName()),
                            divergence.field(),
                            sanitizeForTerminal(String.valueOf(divergence.detected())),
                            sanitizeForTerminal(String.valueOf(divergence.provided())));
                }
            }
        }
        return anyDivergence ? 1 : 0;
    }
}
