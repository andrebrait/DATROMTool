package io.github.datromtool.data;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.SerializationHelper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage matrix rows 3 and 7 of issue #14 step 1: {@link Filter#getExcludeCategories()}
 * defaults to empty, and round-trips through both JSON and YAML via {@link SerializationHelper}.
 */
class FilterTest {

    @Test
    void defaultExcludeCategoriesIsEmpty() {
        Filter filter = Filter.builder().build();
        assertTrue(
                filter.getExcludeCategories().isEmpty(),
                "default Filter.excludeCategories must be empty, got: " + filter.getExcludeCategories());
    }

    @Test
    void defaultExcludeCategoriesIsOmittedFromSerialization() {
        Filter filter = Filter.builder().build();
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        String json = mapper.writeValueAsString(filter);
        assertFalse(
                json.contains("excludeCategories"),
                "default excludeCategories must be omitted, got: " + json);
    }

    @Test
    void excludeCategoriesRoundTripsThroughJson() {
        Filter filter = Filter.builder()
                .excludeCategories(ImmutableSet.of(GameCategory.BAD, GameCategory.DLC))
                .build();
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        String json = mapper.writeValueAsString(filter);
        Filter roundTripped = mapper.readValue(json, Filter.class);
        assertEquals(
                filter,
                roundTripped,
                "Filter with excludeCategories must round-trip through JSON, got: " + json);
    }

    @Test
    void excludeCategoriesRoundTripsThroughYaml() {
        Filter filter = Filter.builder()
                .excludeCategories(ImmutableSet.of(GameCategory.PROTO, GameCategory.UPDATE))
                .build();
        YAMLMapper mapper = SerializationHelper.getInstance().getYamlMapper();
        String yaml = mapper.writeValueAsString(filter);
        Filter roundTripped = mapper.readValue(yaml, Filter.class);
        assertEquals(
                filter,
                roundTripped,
                "Filter with excludeCategories must round-trip through YAML, got: " + yaml);
    }

    // Issue #15 step 1, coverage matrix row 3: excludes/includes carry structured NameMatcher
    // entries, and a literal entry must not leak \Q...\E quoting into the serialized form.
    @Test
    void excludesAndIncludesRoundTripThroughJsonWithNoQuoteLeakage() {
        Filter filter = Filter.builder()
                .excludes(ImmutableSet.of(NameMatcher.literal("a(b")))
                .includes(ImmutableSet.of(NameMatcher.regex("c.d")))
                .build();
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        String json = mapper.writeValueAsString(filter);
        assertTrue(
                json.replaceAll("\\s+", "").contains("\"type\":\"literal\""),
                "serialized literal exclude entry must contain lowercase type, got: " + json);
        assertTrue(
                json.replaceAll("\\s+", "").contains("\"type\":\"regex\""),
                "serialized regex include entry must contain lowercase type, got: " + json);
        assertFalse(
                json.contains("\\Q"),
                "serialized Filter with a literal matcher must not leak \\Q quoting, got: " + json);
        Filter roundTripped = mapper.readValue(json, Filter.class);
        assertEquals(
                filter,
                roundTripped,
                "Filter with excludes/includes NameMatcher sets must round-trip through JSON, got: " + json);
    }

    @Test
    void excludesAndIncludesRoundTripThroughYaml() {
        Filter filter = Filter.builder()
                .excludes(ImmutableSet.of(NameMatcher.literal("a(b")))
                .includes(ImmutableSet.of(NameMatcher.regex("c.d")))
                .build();
        YAMLMapper mapper = SerializationHelper.getInstance().getYamlMapper();
        String yaml = mapper.writeValueAsString(filter);
        Filter roundTripped = mapper.readValue(yaml, Filter.class);
        assertEquals(
                filter,
                roundTripped,
                "Filter with excludes/includes NameMatcher sets must round-trip through YAML, got: " + yaml);
    }
}
