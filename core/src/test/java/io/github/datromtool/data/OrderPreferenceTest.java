package io.github.datromtool.data;

import io.github.datromtool.SerializationHelper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Issue #31 F5: {@link OrderPreference} serializes lowercase, matching the lowercase contract
 * already established by {@code NameMatcher.MatchType} ({@code type: "literal"}) and
 * {@link io.github.datromtool.io.ArchiveType} ({@code archiveType: "zip"}), while still reading
 * either casing (existing profile files written before this fix, or hand-authored uppercase
 * YAML, keep parsing).
 */
class OrderPreferenceTest {

    @Test
    void earliestSerializesLowercaseThroughJson() {
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        String json = mapper.writeValueAsString(OrderPreference.EARLIEST);
        assertEquals("\"earliest\"", json, "OrderPreference.EARLIEST must serialize lowercase, got: " + json);
    }

    @Test
    void latestSerializesLowercaseThroughJson() {
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        String json = mapper.writeValueAsString(OrderPreference.LATEST);
        assertEquals("\"latest\"", json, "OrderPreference.LATEST must serialize lowercase, got: " + json);
    }

    @Test
    void earliestSerializesLowercaseThroughYaml() {
        YAMLMapper mapper = SerializationHelper.getInstance().getYamlMapper();
        String yaml = mapper.writeValueAsString(OrderPreference.EARLIEST);
        assertFalse(yaml.contains("EARLIEST"), "OrderPreference.EARLIEST must not serialize uppercase through YAML, got: " + yaml);
        OrderPreference roundTripped = mapper.readValue(yaml, OrderPreference.class);
        assertEquals(OrderPreference.EARLIEST, roundTripped, "OrderPreference must round-trip through YAML, got: " + yaml);
    }

    @Test
    void lowercaseValueIsReadCorrectly() {
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        assertEquals(
                OrderPreference.EARLIEST,
                mapper.readValue("\"earliest\"", OrderPreference.class),
                "lowercase 'earliest' must parse to OrderPreference.EARLIEST");
    }

    @Test
    void uppercaseValueIsStillReadCaseInsensitively() {
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        assertEquals(
                OrderPreference.EARLIEST,
                mapper.readValue("\"EARLIEST\"", OrderPreference.class),
                "uppercase 'EARLIEST' must still parse to OrderPreference.EARLIEST (case-insensitive reads)");
    }
}
