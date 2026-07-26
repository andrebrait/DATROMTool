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
 * Coverage matrix row 5 of issue #14 step 2: {@link SortingPreference#getVersions()},
 * {@link SortingPreference#getRevisions()} and {@link SortingPreference#getPrereleases()}
 * default to {@link OrderPreference#LATEST} and round-trip non-default {@link OrderPreference}
 * values through both JSON and YAML via {@link SerializationHelper}.
 */
class SortingPreferenceTest {

    @Test
    void defaultOrderPreferencesAreLatest() {
        SortingPreference preference = SortingPreference.builder().build();
        assertEquals(OrderPreference.LATEST, preference.getVersions(),
                "default versions must be LATEST");
        assertEquals(OrderPreference.LATEST, preference.getRevisions(),
                "default revisions must be LATEST");
        assertEquals(OrderPreference.LATEST, preference.getPrereleases(),
                "default prereleases must be LATEST");
    }

    @Test
    void defaultOrderPreferencesAreOmittedFromSerialization() {
        SortingPreference preference = SortingPreference.builder().build();
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        String json = mapper.writeValueAsString(preference);
        assertFalse(json.contains("versions"), "default versions must be omitted, got: " + json);
        assertFalse(json.contains("revisions"), "default revisions must be omitted, got: " + json);
        assertFalse(json.contains("prereleases"), "default prereleases must be omitted, got: " + json);
    }

    @Test
    void nonDefaultOrderPreferencesRoundTripThroughJson() {
        SortingPreference preference = SortingPreference.builder()
                .versions(OrderPreference.EARLIEST)
                .revisions(OrderPreference.EARLIEST)
                .prereleases(OrderPreference.EARLIEST)
                .build();
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        String json = mapper.writeValueAsString(preference);
        SortingPreference roundTripped = mapper.readValue(json, SortingPreference.class);
        assertEquals(
                preference,
                roundTripped,
                "SortingPreference with EARLIEST order preferences must round-trip through JSON, got: " + json);
    }

    @Test
    void nonDefaultOrderPreferencesRoundTripThroughYaml() {
        SortingPreference preference = SortingPreference.builder()
                .versions(OrderPreference.EARLIEST)
                .revisions(OrderPreference.EARLIEST)
                .prereleases(OrderPreference.EARLIEST)
                .build();
        YAMLMapper mapper = SerializationHelper.getInstance().getYamlMapper();
        String yaml = mapper.writeValueAsString(preference);
        SortingPreference roundTripped = mapper.readValue(yaml, SortingPreference.class);
        assertEquals(
                preference,
                roundTripped,
                "SortingPreference with EARLIEST order preferences must round-trip through YAML, got: " + yaml);
    }

    // Issue #15 step 1, coverage matrix row 3: prefers/avoids carry structured NameMatcher
    // entries, and a literal entry must not leak \Q...\E quoting into the serialized form.
    @Test
    void prefersAndAvoidsRoundTripThroughJsonWithNoQuoteLeakage() {
        SortingPreference preference = SortingPreference.builder()
                .prefers(ImmutableSet.of(NameMatcher.literal("a(b")))
                .avoids(ImmutableSet.of(NameMatcher.regex("c.d")))
                .build();
        JsonMapper mapper = SerializationHelper.getInstance().getJsonMapper();
        String json = mapper.writeValueAsString(preference);
        assertTrue(
                json.replaceAll("\\s+", "").contains("\"type\":\"literal\""),
                "serialized literal prefers entry must contain lowercase type, got: " + json);
        assertTrue(
                json.replaceAll("\\s+", "").contains("\"type\":\"regex\""),
                "serialized regex avoids entry must contain lowercase type, got: " + json);
        assertFalse(
                json.contains("\\Q"),
                "serialized SortingPreference with a literal matcher must not leak \\Q quoting, got: " + json);
        SortingPreference roundTripped = mapper.readValue(json, SortingPreference.class);
        assertEquals(
                preference,
                roundTripped,
                "SortingPreference with prefers/avoids NameMatcher sets must round-trip through JSON, got: " + json);
    }

    @Test
    void prefersAndAvoidsRoundTripThroughYaml() {
        SortingPreference preference = SortingPreference.builder()
                .prefers(ImmutableSet.of(NameMatcher.literal("a(b")))
                .avoids(ImmutableSet.of(NameMatcher.regex("c.d")))
                .build();
        YAMLMapper mapper = SerializationHelper.getInstance().getYamlMapper();
        String yaml = mapper.writeValueAsString(preference);
        SortingPreference roundTripped = mapper.readValue(yaml, SortingPreference.class);
        assertEquals(
                preference,
                roundTripped,
                "SortingPreference with prefers/avoids NameMatcher sets must round-trip through YAML, got: " + yaml);
    }
}
