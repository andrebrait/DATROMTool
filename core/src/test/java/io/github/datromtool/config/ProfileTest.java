package io.github.datromtool.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.FileOutputOptions;
import io.github.datromtool.data.GameCategory;
import io.github.datromtool.data.NameMatcher;
import io.github.datromtool.data.OrderPreference;
import io.github.datromtool.data.OutputMode;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.data.TextOutputOptions;
import io.github.datromtool.io.ArchiveType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Coverage matrix for issue #15 step 2's {@link Profile} aggregate: default omission, full
 * round-tripping through both formats, section optionality, and output exclusivity validation.
 * Merge behavior itself is covered directly in {@link JsonNodeMergeTest} and end-to-end in
 * {@code io.github.datromtool.SerializationHelperProfileTest}.
 */
class ProfileTest {

    private static final JsonMapper JSON = SerializationHelper.getInstance().getJsonMapper();
    private static final YAMLMapper YAML = SerializationHelper.getInstance().getYamlMapper();

    // Row 1: an empty file (JSON "{}" / equivalent empty YAML mapping) yields a default Profile,
    // and a default-built Profile serializes back to "{}" (NON_DEFAULT omission).
    @Test
    void emptyJsonYieldsDefaultProfile() {
        assertEquals(Profile.builder().build(), JSON.readValue("{}", Profile.class));
    }

    @Test
    void emptyYamlYieldsDefaultProfile() {
        assertEquals(Profile.builder().build(), YAML.readValue("{}\n", Profile.class));
    }

    @Test
    void defaultProfileSerializesToEmptyJsonObject() {
        String json = JSON.writeValueAsString(Profile.builder().build());
        assertEquals("{}", json.replaceAll("\\s+", ""), "default Profile must serialize to {}, got: " + json);
    }

    // Row 2: a fully populated profile (every section, NameMatcher entries, GameCategory
    // excludes, OrderPreference sorts, wrapper-record performance values) round-trips through
    // both JSON and YAML, and both round trips agree with each other.
    @Test
    void fullProfileRoundTripsThroughJsonAndYamlEqual() {
        Profile profile = fullProfile();

        String json = JSON.writeValueAsString(profile);
        Profile fromJson = JSON.readValue(json, Profile.class);
        assertEquals(profile, fromJson, "full Profile must round-trip through JSON, got: " + json);

        String yaml = YAML.writeValueAsString(profile);
        Profile fromYaml = YAML.readValue(yaml, Profile.class);
        assertEquals(profile, fromYaml, "full Profile must round-trip through YAML, got: " + yaml);

        assertEquals(fromJson, fromYaml, "JSON and YAML round trips of the same Profile must be equal");
    }

    private static Profile fullProfile() {
        return Profile.builder()
                .input(Profile.InputSection.builder()
                        .dats(ImmutableList.of(Paths.get("game.dat")))
                        .dirs(ImmutableList.of(Paths.get("roms")))
                        .build())
                .filter(Filter.builder()
                        .includeRegions(ImmutableSet.of("USA"))
                        .excludeRegions(ImmutableSet.of("Japan"))
                        .includeLanguages(ImmutableSet.of("En"))
                        .excludeLanguages(ImmutableSet.of("Ja"))
                        .excludes(ImmutableSet.of(NameMatcher.literal("a(b")))
                        .includes(ImmutableSet.of(NameMatcher.regex("c.d")))
                        .excludeCategories(ImmutableSet.of(GameCategory.BAD, GameCategory.DLC))
                        .build())
                .sort(SortingPreference.builder()
                        .regions(ImmutableSet.of("USA"))
                        .languages(ImmutableSet.of("En"))
                        .prefers(ImmutableSet.of(NameMatcher.literal("Rev")))
                        .avoids(ImmutableSet.of(NameMatcher.regex("Beta.*")))
                        .prioritizeLanguages(true)
                        .versions(OrderPreference.EARLIEST)
                        .revisions(OrderPreference.EARLIEST)
                        .prereleases(OrderPreference.EARLIEST)
                        .preferParents(true)
                        .preferPrereleases(true)
                        .build())
                .postFilter(PostFilter.builder()
                        .excludes(ImmutableSet.of(NameMatcher.literal("[BIOS]")))
                        .build())
                .output(Profile.OutputSection.builder()
                        .file(new FileOutputOptions(Paths.get("out"), true, ArchiveType.ZIP, true))
                        .build())
                .performance(AppConfig.builder()
                        .scanner(AppConfig.FileScannerConfig.builder()
                                .threads(new ScanThreads(7))
                                .defaultBufferSize(new ScanBufferSize(65536))
                                .maxBufferSize(new ScanMaxBufferSize(1048576))
                                .build())
                        .copier(AppConfig.FileCopierConfig.builder()
                                .threads(new CopyThreads(9))
                                .bufferSize(new CopyBufferSize(4096))
                                .build())
                        .build())
                .build();
    }

    // Row 3: a profile with only "filter" populated leaves sort/postFilter/performance/input/
    // output at their own defaults.
    @Test
    void onlyFilterSectionSetLeavesOtherSectionsDefault() {
        Profile profile = JSON.readValue("{\"filter\":{\"includeRegions\":[\"USA\"]}}", Profile.class);
        assertEquals(ImmutableSet.of("USA"), profile.getFilter().getIncludeRegions());
        assertEquals(SortingPreference.builder().build(), profile.getSort());
        assertEquals(PostFilter.builder().build(), profile.getPostFilter());
        assertEquals(AppConfig.builder().build(), profile.getPerformance());
        assertEquals(Profile.InputSection.builder().build(), profile.getInput());
        assertEquals(Profile.OutputSection.builder().build(), profile.getOutput());
    }

    // Row 5: output.file and output.text are mutually exclusive.
    @Test
    void bothFileAndTextOutputFailsValidation() {
        Profile profile = Profile.builder()
                .output(Profile.OutputSection.builder()
                        .file(new FileOutputOptions(Paths.get("out"), false, null, false))
                        .text(new TextOutputOptions(null, OutputMode.JSON))
                        .build())
                .build();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, profile::validate);
        assertEquals("output.file and output.text are mutually exclusive", thrown.getMessage());
    }

    @Test
    void fileOnlyOutputPassesValidation() {
        Profile profile = Profile.builder()
                .output(Profile.OutputSection.builder()
                        .file(new FileOutputOptions(Paths.get("out"), false, null, false))
                        .build())
                .build();
        assertDoesNotThrow(profile::validate);
    }

    @Test
    void textOnlyOutputPassesValidation() {
        Profile profile = Profile.builder()
                .output(Profile.OutputSection.builder()
                        .text(new TextOutputOptions(null, OutputMode.JSON))
                        .build())
                .build();
        assertDoesNotThrow(profile::validate);
    }

    @Test
    void neitherFileNorTextOutputPassesValidation() {
        assertDoesNotThrow(() -> Profile.builder().build().validate());
    }
}
