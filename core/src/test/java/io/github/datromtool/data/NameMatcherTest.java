package io.github.datromtool.data;

import io.github.datromtool.SerializationHelper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Coverage matrix rows 1-3 of issue #15 step 1: {@link NameMatcher} replicates the pre-existing
 * literal-quoting/regex-compiling semantics, is value-based in equality (the derived
 * {@link java.util.regex.Pattern} is excluded), and round-trips through JSON/YAML with a
 * lowercase {@code type} and no {@code \Q...\E} leakage for literal entries.
 */
class NameMatcherTest {

    // Matrix row 1: LITERAL matches exactly like today's Pattern.quote path.
    @Test
    void literalMatcherTreatsMetacharactersAsInert() {
        NameMatcher matcher = NameMatcher.literal("a(b");
        assertTrue(
                matcher.getPattern().matcher("xa(bz").find(),
                "literal matcher for 'a(b' must match text containing the literal 'a(b'");
        assertFalse(
                matcher.getPattern().matcher("ab").find(),
                "literal matcher for 'a(b' must NOT match 'ab' (would match if unquoted regex)");
    }

    // Matrix row 1: REGEX compiles and matches as a real regular expression.
    @Test
    void regexMatcherCompilesAndMatchesAsRegularExpression() {
        NameMatcher matcher = NameMatcher.regex("a.b");
        assertTrue(
                matcher.getPattern().matcher("aXb").find(),
                "regex matcher for 'a.b' must match 'aXb' as a regular expression");
    }

    // Matrix row 1: invalid REGEX value fails clearly at construction, message names the value.
    @Test
    void invalidRegexValueFailsAtConstructionWithOffendingValueInMessage() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> NameMatcher.regex("(["),
                "an invalid regex must fail at NameMatcher construction");
        assertTrue(
                e.getMessage().contains("(["),
                "exception message must mention the offending value, got: " + e.getMessage());
    }

    // Matrix row 2: equality is value-based; the compiled Pattern is excluded.
    @Test
    void equalityIsValueBasedIgnoringCompiledPattern() {
        NameMatcher a = NameMatcher.literal("Zelda");
        NameMatcher b = NameMatcher.literal("Zelda");
        assertEquals(a, b, "two literal matchers with the same value must be equal");
        assertEquals(a.hashCode(), b.hashCode(), "equal matchers must have equal hash codes");
        // Different compiled Pattern instances (Pattern has no value equality) must not affect it.
        assertNotEquals(a.getPattern(), b.getPattern(), "sanity: Pattern itself is identity-based");
    }

    @Test
    void equalityDistinguishesTypeForTheSameValue() {
        NameMatcher literal = NameMatcher.literal("Zelda");
        NameMatcher regex = NameMatcher.regex("Zelda");
        assertNotEquals(
                literal,
                regex,
                "a literal and a regex matcher with the same value must not be equal");
    }

    // Matrix row 3: JSON/YAML round-trip, lowercase type, case-insensitive read, no \Q leakage.
    @Test
    void literalMatcherRoundTripsThroughJsonWithLowercaseTypeAndNoQuoting() {
        NameMatcher matcher = NameMatcher.literal("Zelda");
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        String json = mapper.writeValueAsString(matcher);
        assertTrue(
                json.replaceAll("\\s+", "").contains("\"type\":\"literal\""),
                "serialized literal matcher must contain lowercase type, got: " + json);
        assertFalse(
                json.contains("\\Q"),
                "serialized literal matcher must not leak \\Q quoting, got: " + json);
        NameMatcher roundTripped = mapper.readValue(json, NameMatcher.class);
        assertEquals(matcher, roundTripped, "NameMatcher must round-trip through JSON, got: " + json);
    }

    @Test
    void regexMatcherRoundTripsThroughYaml() {
        NameMatcher matcher = NameMatcher.regex("a.b");
        YAMLMapper mapper = SerializationHelper.getInstance().getYamlMapper();
        String yaml = mapper.writeValueAsString(matcher);
        assertTrue(
                yaml.contains("type: \"regex\""),
                "serialized regex matcher must contain lowercase type, got: " + yaml);
        NameMatcher roundTripped = mapper.readValue(yaml, NameMatcher.class);
        assertEquals(matcher, roundTripped, "NameMatcher must round-trip through YAML, got: " + yaml);
    }

    @Test
    void typeIsReadCaseInsensitively() {
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        NameMatcher upper = mapper.readValue("{\"value\":\"Zelda\",\"type\":\"LITERAL\"}", NameMatcher.class);
        assertEquals(NameMatcher.literal("Zelda"), upper, "type must be read case-insensitively");
    }
}
