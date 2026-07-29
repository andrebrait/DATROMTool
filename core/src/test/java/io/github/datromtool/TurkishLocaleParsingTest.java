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
import org.junitpioneer.jupiter.DefaultLocale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parsing must not depend on the machine's locale. Turkish is the standard probe: its dotless-i
 * rules make {@code "It".toLowerCase()} yield {@code "ıt"} and {@code "ita".toUpperCase()} yield
 * {@code "İTA"}, so any case conversion done without {@link java.util.Locale#ROOT} quietly stops
 * recognising language tags and region codes for users in that locale.
 */
@DefaultLocale("tr-TR")
class TurkishLocaleParsingTest {

    private static Datafile datafileOf(Game game) {
        return Datafile.builder().games(ImmutableList.of(game)).build();
    }

    private static ParsedGame parse(Game game) throws Exception {
        RegionData regionData = TestUtils.loadRegionData();
        return new GameParser(regionData, GameParser.DivergenceDetection.ONE_WAY)
                .parse(datafileOf(game))
                .get(0);
    }

    @Test
    void languageTagInTheNameIsRecognisedRegardlessOfLocale() throws Exception {
        ParsedGame parsed = parse(Game.builder()
                .name("Test Game (Europe) (It)")
                .description("Test Game (Europe) (It)")
                .build());

        assertTrue(
                parsed.getLanguages().contains("it"),
                () -> "the Italian language tag must be detected under any locale, got: "
                        + parsed.getLanguages());
    }

    @Test
    void declaredReleaseLanguageIsRecognisedRegardlessOfLocale() throws Exception {
        ParsedGame parsed = parse(Game.builder()
                .name("Test Game (Europe)")
                .description("Test Game (Europe)")
                .releases(ImmutableList.of(
                        new Release("Test Game (Europe)", "EUR", "It", null, YesNo.NO)))
                .build());

        assertTrue(
                parsed.getLanguages().contains("it"),
                () -> "a DAT-declared language must be detected under any locale, got: "
                        + parsed.getLanguages());
    }

    @Test
    void declaredReleaseRegionIsRecognisedRegardlessOfLocale() throws Exception {
        ParsedGame parsed = parse(Game.builder()
                .name("Test Game")
                .description("Test Game")
                .releases(ImmutableList.of(
                        new Release("Test Game", "ita", null, null, YesNo.NO)))
                .build());

        assertTrue(
                parsed.getRegionsStream().anyMatch("ITA"::equals),
                () -> "a DAT-declared region code must be upper-cased under any locale, got: "
                        + parsed.getRegionData());
    }
}
