package io.github.datromtool.retool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.GameFilterer;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.domain.retool.RetoolMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.github.datromtool.util.TestUtils.createGame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link MetadataEnricher} against the real trimmed upstream fixture already pinned by
 * {@code domain}'s {@code RetoolMetadataTest} (issue #19 step 1), plus the issue's own
 * acceptance criterion: enrichment makes {@link GameFilterer} language filtering work on a DAT
 * lacking language flags.
 */
class MetadataEnricherTest {

    private static final RegionData EMPTY_REGION_DATA = new RegionData(ImmutableSet.of());

    private static RetoolMetadata metadata;

    @BeforeAll
    static void beforeAll() throws Exception {
        metadata = SerializationHelper.getInstance()
                .loadRetoolMetadata(resource("retool/metadata/atari-2600-no-intro-trimmed.json"));
    }

    private static Path resource(String name) throws URISyntaxException {
        return Paths.get(ClassLoader.getSystemResource(name).toURI());
    }

    private static ParsedGame plainGame(String name) {
        return ParsedGame.builder()
                .game(createGame(name))
                .regionData(EMPTY_REGION_DATA)
                .build();
    }

    // Empty-language game gains metadata languages, lowercased to match this codebase's
    // convention (the fixture stores "En" title-case).
    @Test
    void emptyLanguageGameGainsMetadataLanguages() {
        ParsedGame game = plainGame("Air Raiders (USA)");

        ImmutableList<ParsedGame> enriched = MetadataEnricher.enrich(ImmutableList.of(game), metadata);

        assertEquals(ImmutableSet.of("en"), enriched.get(0).getLanguages());
    }

    @Test
    void multiLanguageMetadataEntryIsLowercasedAndPreserved() {
        ParsedGame game = plainGame("RealSports Baseball (USA, Europe) (En,Fr,De,Es,It)");

        ImmutableList<ParsedGame> enriched = MetadataEnricher.enrich(ImmutableList.of(game), metadata);

        assertEquals(ImmutableSet.of("de", "en", "es", "fr", "it"), enriched.get(0).getLanguages());
    }

    // Non-empty ParsedGame.languages (i.e. name-parsing or DAT release data already found some)
    // is left untouched, even if the same title exists in the metadata file.
    @Test
    void nonEmptyLanguagesAreUntouched() {
        ParsedGame game = plainGame("Air Raiders (USA)").toBuilder()
                .languages(ImmutableSet.of("fr"))
                .build();

        ImmutableList<ParsedGame> enriched = MetadataEnricher.enrich(ImmutableList.of(game), metadata);

        assertEquals(ImmutableSet.of("fr"), enriched.get(0).getLanguages());
    }

    // A title absent from the metadata file is untouched (still empty).
    @Test
    void gameNotInMetadataIsUntouched() {
        ParsedGame game = plainGame("Some Unrelated Homebrew (USA)");

        ImmutableList<ParsedGame> enriched = MetadataEnricher.enrich(ImmutableList.of(game), metadata);

        assertTrue(enriched.get(0).getLanguages().isEmpty());
    }

    @Test
    void nullMetadataIsANoOpCopy() {
        ParsedGame game = plainGame("Air Raiders (USA)");

        ImmutableList<ParsedGame> enriched = MetadataEnricher.enrich(ImmutableList.of(game), null);

        assertSame(game, enriched.get(0));
    }

    // Issue #19 acceptance criterion: "Metadata enrichment makes language filtering work on a
    // DAT lacking language flags." Air Raiders (USA) has no name-parseable language tag and no
    // DAT release data here - only metadata enrichment supplies "en", which
    // GameFilterer#filterIncludeLanguage then keys off of.
    @Test
    void enrichmentMakesLanguageIncludeFilterWorkOnALanguagelessDat() {
        ParsedGame withoutLanguageTag = plainGame("Air Raiders (USA)");
        Filter includeEnglish = Filter.builder()
                .includeLanguages(ImmutableSet.of("en"))
                .build();
        GameFilterer filterer = new GameFilterer(includeEnglish, PostFilter.builder().build());

        // Before enrichment: no language data at all, so the include filter removes it.
        assertTrue(filterer.filter(ImmutableList.of(withoutLanguageTag)).isEmpty());

        // After enrichment: metadata supplies "en", so the include filter keeps it.
        ImmutableList<ParsedGame> enriched =
                MetadataEnricher.enrich(ImmutableList.of(withoutLanguageTag), metadata);
        assertEquals(1, filterer.filter(enriched).size());
    }
}
