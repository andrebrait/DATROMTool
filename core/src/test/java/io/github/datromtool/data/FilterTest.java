package io.github.datromtool.data;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.SerializationHelper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
