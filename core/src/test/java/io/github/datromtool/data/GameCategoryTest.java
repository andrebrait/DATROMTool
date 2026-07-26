package io.github.datromtool.data;

import io.github.datromtool.SerializationHelper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Issue #31 F5: {@link GameCategory} serializes lowercase, matching the lowercase contract
 * already established by {@code NameMatcher.MatchType} ({@code type: "literal"}) and
 * {@link io.github.datromtool.io.ArchiveType} ({@code archiveType: "zip"}), while still reading
 * either casing (existing profile files written before this fix, or hand-authored uppercase
 * YAML, keep parsing).
 */
class GameCategoryTest {

    @Test
    void badSerializesLowercaseThroughJson() {
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        String json = mapper.writeValueAsString(GameCategory.BAD);
        assertEquals("\"bad\"", json, "GameCategory.BAD must serialize lowercase, got: " + json);
    }

    @Test
    void dlcSerializesLowercaseThroughYaml() {
        YAMLMapper mapper = SerializationHelper.getInstance().getYamlMapper();
        String yaml = mapper.writeValueAsString(GameCategory.DLC);
        assertFalse(yaml.contains("DLC"), "GameCategory.DLC must not serialize uppercase through YAML, got: " + yaml);
        GameCategory roundTripped = mapper.readValue(yaml, GameCategory.class);
        assertEquals(GameCategory.DLC, roundTripped, "GameCategory must round-trip through YAML, got: " + yaml);
    }

    @Test
    void lowercaseValueIsReadCorrectly() {
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        assertEquals(
                GameCategory.PROTO,
                mapper.readValue("\"proto\"", GameCategory.class),
                "lowercase 'proto' must parse to GameCategory.PROTO");
    }

    @Test
    void uppercaseValueIsStillReadCaseInsensitively() {
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        assertEquals(
                GameCategory.PROTO,
                mapper.readValue("\"PROTO\"", GameCategory.class),
                "uppercase 'PROTO' must still parse to GameCategory.PROTO (case-insensitive reads)");
    }
}
