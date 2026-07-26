package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.GameCategory;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.domain.datafile.logiqx.Game;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link GameFilterer}'s category exclusion behavior (issue #14 step 1): the five
 * structural categories (PROTO/BETA/DEMO/SAMPLE/BIOS) are decided from parsed {@link ParsedGame}
 * fields, and the remaining eight are decided by matching the game name against the category's
 * {@link Patterns} regex.
 */
class GameFiltererTest {

    private static final RegionData EMPTY_REGION_DATA = new RegionData(ImmutableSet.of());

    private static ParsedGame plainGame(String name) {
        return ParsedGame.builder()
                .game(Game.builder().name(name).description(name).build())
                .regionData(EMPTY_REGION_DATA)
                .build();
    }

    private static GameFilterer filtererExcluding(GameCategory... categories) {
        Filter filter = Filter.builder()
                .excludeCategories(ImmutableSet.copyOf(categories))
                .build();
        return new GameFilterer(filter, PostFilter.builder().build());
    }

    private static boolean isKept(GameFilterer filterer, ParsedGame game) {
        return filterer.filter(ImmutableList.of(game)).contains(game);
    }

    // Coverage matrix row 4: structural categories, parameterized over PROTO/BETA/DEMO/SAMPLE/BIOS
    private enum Structural {
        PROTO(GameCategory.PROTO, p -> p.toBuilder().proto(ImmutableList.of(1L)).build()),
        BETA(GameCategory.BETA, p -> p.toBuilder().beta(ImmutableList.of(1L)).build()),
        DEMO(GameCategory.DEMO, p -> p.toBuilder().demo(ImmutableList.of(1L)).build()),
        SAMPLE(GameCategory.SAMPLE, p -> p.toBuilder().sample(ImmutableList.of(1L)).build()),
        BIOS(GameCategory.BIOS, p -> p.toBuilder().bios(true).build());

        final GameCategory category;
        final UnaryOperator<ParsedGame> withTrait;

        Structural(GameCategory category, UnaryOperator<ParsedGame> withTrait) {
            this.category = category;
            this.withTrait = withTrait;
        }
    }

    @ParameterizedTest
    @EnumSource(Structural.class)
    void structuralCategoryFiltersGameExhibitingTraitWhenExcluded(Structural s) {
        ParsedGame game = s.withTrait.apply(plainGame("Some Game"));
        GameFilterer filterer = filtererExcluding(s.category);
        assertFalse(
                isKept(filterer, game),
                s.category + " must remove a game exhibiting the " + s.category + " trait when excluded");
    }

    @ParameterizedTest
    @EnumSource(Structural.class)
    void structuralCategoryKeepsGameExhibitingTraitWhenNotExcluded(Structural s) {
        ParsedGame game = s.withTrait.apply(plainGame("Some Game"));
        GameFilterer filterer = filtererExcluding(); // nothing excluded
        assertTrue(
                isKept(filterer, game),
                s.category + " must keep a game exhibiting the " + s.category
                        + " trait when not excluded");
    }

    @ParameterizedTest
    @EnumSource(Structural.class)
    void structuralCategoryKeepsGameNotExhibitingTraitEvenWhenExcluded(Structural s) {
        ParsedGame game = plainGame("Some Game"); // no trait set
        GameFilterer filterer = filtererExcluding(s.category);
        assertTrue(
                isKept(filterer, game),
                s.category + " must keep a game NOT exhibiting the " + s.category
                        + " trait, even when excluded");
    }

    // Coverage matrix row 5: pattern-backed categories, parameterized over BAD/PROGRAM/CHIP/
    // PIRATE/PROMO/UNLICENSED/DLC/UPDATE. Names taken to match the actual Patterns.* regexes.
    private enum PatternBacked {
        BAD(GameCategory.BAD, "Some Game [b]"),
        PROGRAM(GameCategory.PROGRAM, "Some Game (Program)"),
        CHIP(GameCategory.CHIP, "Some Game (Enhancement Chip)"),
        PIRATE(GameCategory.PIRATE, "Some Game (Pirate)"),
        PROMO(GameCategory.PROMO, "Some Game (Promo)"),
        UNLICENSED(GameCategory.UNLICENSED, "Some Game (Unl)"),
        DLC(GameCategory.DLC, "Some Game (DLC)"),
        UPDATE(GameCategory.UPDATE, "Some Game (Update)");

        final GameCategory category;
        final String matchingName;

        PatternBacked(GameCategory category, String matchingName) {
            this.category = category;
            this.matchingName = matchingName;
        }
    }

    @ParameterizedTest
    @EnumSource(PatternBacked.class)
    void patternBackedCategoryFiltersMatchingNameWhenExcluded(PatternBacked p) {
        assertTrue(
                p.category.getPattern().orElseThrow().matcher(p.matchingName).find(),
                "test fixture name must actually match Patterns." + p.category);
        ParsedGame game = plainGame(p.matchingName);
        GameFilterer filterer = filtererExcluding(p.category);
        assertFalse(
                isKept(filterer, game),
                p.category + " must remove a game whose name matches its pattern when excluded");
    }

    @ParameterizedTest
    @EnumSource(PatternBacked.class)
    void patternBackedCategoryKeepsMatchingNameWhenNotExcluded(PatternBacked p) {
        ParsedGame game = plainGame(p.matchingName);
        GameFilterer filterer = filtererExcluding(); // nothing excluded
        assertTrue(
                isKept(filterer, game),
                p.category + " must keep a game whose name matches its pattern when not excluded");
    }

    @Test
    void multipleExcludedCategoriesCombineWithOr() {
        GameFilterer filterer = filtererExcluding(GameCategory.PROTO, GameCategory.DLC);
        ParsedGame protoGame = ParsedGame.builder()
                .game(Game.builder().name("Proto Game").description("Proto Game").build())
                .regionData(EMPTY_REGION_DATA)
                .proto(ImmutableList.of(1L))
                .build();
        ParsedGame dlcGame = plainGame("DLC Game (DLC)");
        ParsedGame unrelatedGame = plainGame("Plain Game");

        ImmutableList<ParsedGame> result = filterer.filter(
                ImmutableList.of(protoGame, dlcGame, unrelatedGame));

        assertTrue(result.contains(unrelatedGame), "unrelated game must be kept");
        assertFalse(result.contains(protoGame), "proto game must be removed");
        assertFalse(result.contains(dlcGame), "dlc game must be removed");
    }

    @Test
    void noExcludedCategoriesKeepsEverything() {
        GameFilterer filterer = filtererExcluding();
        ParsedGame game = plainGame("Anything (Proto) [b] (DLC)");
        assertTrue(isKept(filterer, game), "empty excludeCategories must keep every game");
    }

    @Test
    void testPostFilter() {
        GameFilterer filterer = new GameFilterer(
                Filter.builder().build(),
                PostFilter.builder()
                        .excludes(ImmutableSet.of(Pattern.compile("Banned")))
                        .build());
        ParsedGame banned = plainGame("Banned Game");
        ParsedGame allowed = plainGame("Allowed Game");

        assertTrue(
                filterer.postFilter(ImmutableList.of(allowed)).contains(allowed),
                "post-filter must keep a set with no banned name");
        assertTrue(
                filterer.postFilter(ImmutableList.of(banned, allowed)).isEmpty(),
                "post-filter must drop the whole set if any entry matches an exclude pattern");
    }
}
