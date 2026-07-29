package io.github.datromtool.cli.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.github.datromtool.cli.util.TerminalUtils.sanitizeForTerminal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

/**
 * The sanitization point every terminal-bound label goes through. Covers the classes of character
 * a terminal acts on rather than displays; ordinary text must come back untouched, so escaping
 * never costs readability on the names users actually have.
 */
class TerminalUtilsSanitizeTest {

    static Stream<Arguments> cases() {
        return Stream.of(
                argumentSet("plain name", "Some Game (USA).rom", "Some Game (USA).rom"),
                argumentSet("non-ASCII stays readable", "Pokémon Rubí.zip", "Pokémon Rubí.zip"),
                argumentSet("escape", "a\u001Bb", "a\\u001Bb"),
                argumentSet("carriage return", "a\rb", "a\\u000Db"),
                argumentSet("line feed", "a\nb", "a\\u000Ab"),
                argumentSet("tab", "a\tb", "a\\u0009b"),
                argumentSet("NUL", "a\u0000b", "a\\u0000b"),
                argumentSet("DEL", "a\u007Fb", "a\\u007Fb"),
                argumentSet("bidi override", "a\u202Eb", "a\\u202Eb"),
                argumentSet("C1 CSI (8-bit control)", "a\u009Bb", "a\\u009Bb"),
                argumentSet("C1 NEL", "a\u0085b", "a\\u0085b"),
                argumentSet(
                        "C1 CSI colour injection",
                        "evil\u009B31mRED\u009B0m.rom",
                        "evil\\u009B31mRED\\u009B0m.rom"),
                argumentSet(
                        "OSC window-title injection",
                        "evil\u001B]0;pwned\u0007.rom",
                        "evil\\u001B]0;pwned\\u0007.rom"));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void rendersTerminalControlCharactersInertly(String input, String expected) {
        assertEquals(
                expected,
                sanitizeForTerminal(input),
                "a terminal-bound label must display, not execute, its input");
    }
}
