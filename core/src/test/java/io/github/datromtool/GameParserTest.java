package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.domain.datafile.logiqx.Datafile;
import io.github.datromtool.domain.datafile.logiqx.Game;
import io.github.datromtool.domain.datafile.logiqx.Release;
import io.github.datromtool.domain.datafile.logiqx.enumerations.YesNo;
import io.github.datromtool.util.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #18 coverage matrix row 5: pins that {@link GameParser.DivergenceDetection} modes
 * produce an observably different {@link ParsedGame#getDivergences()} result for the same input,
 * directly at the seam that carries divergence data out of {@link GameParser} (previously only a
 * {@code log.warn}, invisible outside the log file).
 */
class GameParserTest {

    // A name-implied region ("Europe" -> EUR) with no <release> element at all: "provided" is
    // empty. ONE_WAY/TWO_WAY require both sides non-empty before comparing, so this must NOT be
    // flagged under ONE_WAY; ALWAYS compares regardless of emptiness, so it MUST be flagged.
    private static Game gameWithNameOnlyRegion() {
        return Game.builder()
                .name("Test Game (Europe)")
                .description("Test Game (Europe)")
                .build();
    }

    private static Datafile datafileOf(Game game) {
        return Datafile.builder().games(ImmutableList.of(game)).build();
    }

    @Test
    void oneWayModeIgnoresADivergenceWhenTheDatDeclaresNoRegionAtAll() throws Exception {
        RegionData regionData = TestUtils.loadRegionData();
        GameParser gameParser = new GameParser(regionData, GameParser.DivergenceDetection.ONE_WAY);

        ParsedGame parsedGame = gameParser.parse(datafileOf(gameWithNameOnlyRegion())).get(0);

        assertTrue(parsedGame.getDivergences().isEmpty(),
                "ONE_WAY must not flag a divergence when the DAT declares no region at all, got: "
                        + parsedGame.getDivergences());
    }

    @Test
    void alwaysModeFlagsTheSameCaseOneWayIgnores() throws Exception {
        RegionData regionData = TestUtils.loadRegionData();
        GameParser gameParser = new GameParser(regionData, GameParser.DivergenceDetection.ALWAYS);

        ParsedGame parsedGame = gameParser.parse(datafileOf(gameWithNameOnlyRegion())).get(0);

        assertEquals(1, parsedGame.getDivergences().size(),
                "ALWAYS must flag a divergence when name-implied metadata has no DAT-declared "
                        + "counterpart at all, got: " + parsedGame.getDivergences());
        ParsedGame.Divergence divergence = parsedGame.getDivergences().get(0);
        assertEquals("region", divergence.field());
        assertEquals(Set.of("EUR"), divergence.detected());
        assertTrue(divergence.provided().isEmpty());
    }

    @Test
    void ignoreModeNeverFlagsEvenAnOutrightMismatch() throws Exception {
        RegionData regionData = TestUtils.loadRegionData();
        GameParser gameParser = new GameParser(regionData, GameParser.DivergenceDetection.IGNORE);
        Game game = Game.builder()
                .name("Test Game (Europe)")
                .description("Test Game (Europe)")
                .releases(ImmutableList.of(
                        new Release("Test Game", "ITA", null, null, YesNo.NO)))
                .build();

        ParsedGame parsedGame = gameParser.parse(datafileOf(game)).get(0);

        assertTrue(parsedGame.getDivergences().isEmpty(),
                "IGNORE must never flag a divergence, got: " + parsedGame.getDivergences());
    }

    @Test
    void twoWayModeFlagsAPartialOverlapThatOneWayAccepts() throws Exception {
        RegionData regionData = TestUtils.loadRegionData();
        // Name implies only Europe; DAT declares both EUR and USA. ONE_WAY only checks
        // provided.containsAll(detected) = {EUR,USA}.containsAll({EUR}) = true -> not flagged.
        // TWO_WAY requires exact equality ({EUR,USA} != {EUR}) -> flagged.
        Game game = Game.builder()
                .name("Test Game (Europe)")
                .description("Test Game (Europe)")
                .releases(ImmutableList.of(
                        new Release("Test Game (EUR)", "EUR", null, null, YesNo.NO),
                        new Release("Test Game (USA)", "USA", null, null, YesNo.NO)))
                .build();

        GameParser oneWay = new GameParser(regionData, GameParser.DivergenceDetection.ONE_WAY);
        ParsedGame oneWayParsed = oneWay.parse(datafileOf(game)).get(0);
        assertTrue(oneWayParsed.getDivergences().isEmpty(),
                "ONE_WAY must accept a provided superset of detected, got: "
                        + oneWayParsed.getDivergences());

        GameParser twoWay = new GameParser(regionData, GameParser.DivergenceDetection.TWO_WAY);
        ParsedGame twoWayParsed = twoWay.parse(datafileOf(game)).get(0);
        assertEquals(1, twoWayParsed.getDivergences().size(),
                "TWO_WAY must flag a provided superset that isn't an exact match, got: "
                        + twoWayParsed.getDivergences());
    }
}
