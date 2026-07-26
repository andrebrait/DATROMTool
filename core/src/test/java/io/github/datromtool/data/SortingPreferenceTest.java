package io.github.datromtool.data;

import io.github.datromtool.SerializationHelper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
