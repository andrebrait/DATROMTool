package io.github.datromtool.data;

import io.github.datromtool.Patterns;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The categories a game can be excluded by, replacing the old scattered
 * {@code --bad --proto --beta --demo --sample --bios --program --chip --pirate --promo
 * --unlicensed --dlc --update} negatable flags plus the {@code --no-all} tri-state.
 *
 * <p>{@link #PROTO}, {@link #BETA}, {@link #DEMO}, {@link #SAMPLE} and {@link #BIOS} are
 * structural: {@link GameFilterer} decides membership from {@link ParsedGame} fields parsed out
 * of the name (no regex needed here, so {@link #getPattern()} is empty). The remaining
 * categories are pattern-backed: membership is decided by matching the game name against the
 * category's {@link Patterns} regex.
 */
public enum GameCategory {

    BAD(Patterns.BAD),
    PROTO(null),
    BETA(null),
    DEMO(null),
    SAMPLE(null),
    BIOS(null),
    PROGRAM(Patterns.PROGRAM),
    CHIP(Patterns.ENHANCEMENT_CHIP),
    PIRATE(Patterns.PIRATE),
    PROMO(Patterns.PROMO),
    UNLICENSED(Patterns.UNLICENSED),
    DLC(Patterns.DLC),
    UPDATE(Patterns.UPDATE);

    @Nullable
    private final Pattern pattern;

    GameCategory(@Nullable Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * The regex that decides membership in this category, if this is a pattern-backed category.
     * Empty for structural categories, whose membership is decided from parsed
     * {@link ParsedGame} fields instead.
     */
    public Optional<Pattern> getPattern() {
        return Optional.ofNullable(pattern);
    }
}
