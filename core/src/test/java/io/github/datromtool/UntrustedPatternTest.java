package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.NameMatcher;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.domain.datafile.logiqx.Game;
import io.github.datromtool.retool.CloneListMatcher;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.domain.retool.CloneList;
import io.github.datromtool.domain.retool.CloneListDescription;
import io.github.datromtool.domain.retool.NameType;
import io.github.datromtool.domain.retool.VariantGroup;
import io.github.datromtool.domain.retool.VariantTitle;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A regex a user points the tool at — a clone list's searchTerm, an {@code --exclude-regex}, a
 * patterns file, a profile — is untrusted input. Whatever it does to the engine must come back as
 * an ordinary failure naming the offending pattern, never as an {@link Error} that unwinds the
 * whole run.
 *
 * <p>The probe is depth, not backtracking: {@code java.util.regex} recurses as it matches, so a
 * pattern can exhaust the stack having read a fraction of the read budget that bounds
 * backtracking. The two failure modes need two different bounds.
 */
class UntrustedPatternTest {

    /**
     * Cheap to compile, but the engine recurses once per repetition while matching, so it
     * exhausts the stack after ~20,000 characters — a fifth of the read budget that guards
     * backtracking, which never fires here.
     */
    private static final String DEEP_PATTERN = "(a|b)*";
    private static final String INPUT = "a".repeat(20_000);

    private static ParsedGame gameNamed(String name) {
        return ParsedGame.builder()
                .game(Game.builder().name(name).description(name).build())
                .regionData(new RegionData(ImmutableSet.of()))
                .build();
    }

    @Test
    void aPatternDeepEnoughToExhaustTheStackFailsAsAnExceptionWhenFiltering() {
        Filter filter = Filter.builder()
                .excludes(ImmutableSet.of(NameMatcher.regex(DEEP_PATTERN)))
                .build();
        GameFilterer filterer = new GameFilterer(filter, PostFilter.builder().build());

        assertThrows(
                RuntimeException.class,
                () -> filterer.filter(ImmutableList.of(gameNamed(INPUT))),
                "an exclude pattern that exhausts the engine must fail as an exception, not an Error");
    }

    @Test
    void aPatternDeepEnoughToExhaustTheStackFailsAsAnExceptionWhenMatchingACloneList() {
        CloneList cloneList = new CloneList(
                new CloneListDescription("Test", "x", "1.0"),
                ImmutableList.of(new VariantGroup(
                        "Group",
                        ImmutableList.of(),
                        false,
                        ImmutableList.of(new VariantTitle(
                                DEEP_PATTERN,
                                NameType.REGEX,
                                1,
                                ImmutableList.of(),
                                false,
                                false,
                                ImmutableMap.of(),
                                ImmutableList.of())),
                        ImmutableList.of(),
                        ImmutableList.of())));
        CloneListMatcher matcher = new CloneListMatcher(
                cloneList,
                new RegionData(ImmutableSet.of()),
                SortingPreference.builder().build());

        assertThrows(
                RuntimeException.class,
                () -> matcher.match(gameNamed(INPUT)),
                "a clone list pattern that exhausts the engine must fail as an exception, not an Error");
    }
}
