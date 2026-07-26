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
 * Issue #15 step 1, coverage matrix row 3: {@link PostFilter#getExcludes()} carries structured
 * {@link NameMatcher} entries and round-trips through JSON/YAML via {@link SerializationHelper}
 * with no {@code \Q...\E} leakage for literal entries.
 */
class PostFilterTest {

    @Test
    void defaultExcludesIsEmpty() {
        PostFilter postFilter = PostFilter.builder().build();
        assertTrue(
                postFilter.getExcludes().isEmpty(),
                "default PostFilter.excludes must be empty, got: " + postFilter.getExcludes());
    }

    @Test
    void excludesRoundTripThroughJsonWithNoQuoteLeakage() {
        PostFilter postFilter = PostFilter.builder()
                .excludes(ImmutableSet.of(NameMatcher.literal("a(b"), NameMatcher.regex("c.d")))
                .build();
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        String json = mapper.writeValueAsString(postFilter);
        assertTrue(
                json.replaceAll("\\s+", "").contains("\"type\":\"literal\""),
                "serialized literal exclude entry must contain lowercase type, got: " + json);
        assertTrue(
                json.replaceAll("\\s+", "").contains("\"type\":\"regex\""),
                "serialized regex exclude entry must contain lowercase type, got: " + json);
        assertFalse(
                json.contains("\\Q"),
                "serialized PostFilter with a literal matcher must not leak \\Q quoting, got: " + json);
        PostFilter roundTripped = mapper.readValue(json, PostFilter.class);
        assertEquals(
                postFilter,
                roundTripped,
                "PostFilter with an excludes NameMatcher set must round-trip through JSON, got: " + json);
    }

    @Test
    void excludesRoundTripThroughYaml() {
        PostFilter postFilter = PostFilter.builder()
                .excludes(ImmutableSet.of(NameMatcher.literal("a(b"), NameMatcher.regex("c.d")))
                .build();
        YAMLMapper mapper = SerializationHelper.getInstance().getYamlMapper();
        String yaml = mapper.writeValueAsString(postFilter);
        PostFilter roundTripped = mapper.readValue(yaml, PostFilter.class);
        assertEquals(
                postFilter,
                roundTripped,
                "PostFilter with an excludes NameMatcher set must round-trip through YAML, got: " + yaml);
    }
}
