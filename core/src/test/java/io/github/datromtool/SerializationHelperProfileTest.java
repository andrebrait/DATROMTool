package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.config.Profile;
import io.github.datromtool.data.OutputMode;
import io.github.datromtool.util.ArchiveUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end {@link SerializationHelper#loadProfile} / {@link SerializationHelper#loadProfiles}
 * coverage for issue #15 step 2: real files on disk, extension-dispatched format detection,
 * multi-file layering, and error surfacing. Direct merge-rule unit tests live in
 * {@code io.github.datromtool.config.JsonNodeMergeTest}; single-{@link Profile} shape tests live
 * in {@code io.github.datromtool.config.ProfileTest}.
 */
class SerializationHelperProfileTest {

    private Path tempDir;
    private SerializationHelper helper;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("datromtool_profile_test_");
        helper = SerializationHelper.getInstance(tempDir);
    }

    @AfterEach
    void tearDown() throws Exception {
        ArchiveUtils.deleteFolder(tempDir);
    }

    private Path writeFile(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    // Row 1 (file-backed): an empty JSON file loads to a default Profile.
    @Test
    void loadProfileFromEmptyJsonFileYieldsDefaults() throws Exception {
        Path file = writeFile("empty.json", "{}");
        assertEquals(Profile.builder().build(), helper.loadProfile(file));
    }

    // Row 1 (file-backed): an empty YAML file loads to a default Profile.
    @Test
    void loadProfileFromEmptyYamlFileYieldsDefaults() throws Exception {
        Path file = writeFile("empty.yaml", "{}\n");
        assertEquals(Profile.builder().build(), helper.loadProfile(file));
    }

    // Row 4: layering two files - later file's list field replaces (4a), later file setting only
    // one field of a section keeps the earlier file's sibling field (4b), a scalar override
    // applies (4c), and a section entirely absent from the later file survives from the earlier
    // one (4d).
    @Test
    void loadProfilesLayersLaterFileOverEarlier() throws Exception {
        Path first = writeFile("first.json", """
                {
                  "filter": {"includeRegions": ["USA"], "includeLanguages": ["En"]},
                  "sort": {"regions": ["USA", "Japan"]},
                  "performance": {"scanner": {"threads": 4}}
                }
                """);
        Path second = writeFile("second.json", """
                {
                  "filter": {"includeLanguages": ["Ja"]},
                  "sort": {"regions": ["Europe"]},
                  "performance": {"scanner": {"threads": 8}}
                }
                """);

        Profile merged = helper.loadProfiles(List.of(first, second));

        // 4(b): includeRegions survived from the first file, includeLanguages came from the
        // second.
        assertEquals(ImmutableSet.of("USA"), merged.getFilter().getIncludeRegions());
        assertEquals(ImmutableSet.of("Ja"), merged.getFilter().getIncludeLanguages());
        // 4(a): sort.regions replaced wholesale by the second file's list.
        assertEquals(ImmutableSet.of("Europe"), merged.getSort().getRegions());
        // 4(c): scalar override.
        assertEquals(8, merged.getPerformance().getScanner().getThreads().value());
        // 4(d): postFilter absent from both files stays at its own default.
        assertEquals(io.github.datromtool.data.PostFilter.builder().build(), merged.getPostFilter());
    }

    // Row 4(d) isolated: a section present only in the first file survives untouched when the
    // second file doesn't mention it at all.
    @Test
    void loadProfilesSectionAbsentFromLaterFileSurvives() throws Exception {
        Path first = writeFile("first.json", "{\"filter\":{\"includeRegions\":[\"USA\"]}}");
        Path second = writeFile("second.json", "{\"sort\":{\"regions\":[\"Japan\"]}}");

        Profile merged = helper.loadProfiles(List.of(first, second));

        assertEquals(ImmutableSet.of("USA"), merged.getFilter().getIncludeRegions());
        assertEquals(ImmutableSet.of("Japan"), merged.getSort().getRegions());
    }

    // Row 4(e): a three-file chain, later files consistently taking precedence.
    @Test
    void loadProfilesThreeFileChain() throws Exception {
        Path first = writeFile("a.json", "{\"filter\":{\"includeRegions\":[\"USA\"],\"includeLanguages\":[\"En\"]}}");
        Path second = writeFile("b.json", "{\"filter\":{\"includeRegions\":[\"Japan\"]}}");
        Path third = writeFile("c.json", "{\"filter\":{\"includeLanguages\":[\"Ja\"]}}");

        Profile merged = helper.loadProfiles(List.of(first, second, third));

        assertEquals(ImmutableSet.of("Japan"), merged.getFilter().getIncludeRegions());
        assertEquals(ImmutableSet.of("Ja"), merged.getFilter().getIncludeLanguages());
    }

    // Row 6: first file YAML, second file JSON - extension-dispatched formats merge correctly.
    @Test
    void loadProfilesMergesMixedFormats() throws Exception {
        Path yamlFile = writeFile("first.yaml", """
                filter:
                  includeRegions: [USA]
                  includeLanguages: [En]
                """);
        Path jsonFile = writeFile("second.json", "{\"filter\":{\"includeLanguages\":[\"Ja\"]}}");

        Profile merged = helper.loadProfiles(List.of(yamlFile, jsonFile));

        assertEquals(ImmutableSet.of("USA"), merged.getFilter().getIncludeRegions());
        assertEquals(ImmutableSet.of("Ja"), merged.getFilter().getIncludeLanguages());
    }

    // Row 7: an empty path list yields a default Profile without touching disk.
    @Test
    void loadProfilesWithEmptyListYieldsDefaults() throws Exception {
        assertEquals(Profile.builder().build(), helper.loadProfiles(ImmutableList.of()));
    }

    // Row 7: FAIL_ON_UNKNOWN_PROPERTIES is disabled on both mappers (SerializationHelper's
    // createJsonMapper/createYamlMapper), so an unknown field in a profile is silently ignored
    // rather than rejected - pinning that actual, already-configured behavior for profiles too.
    @Test
    void loadProfileIgnoresUnknownField() throws Exception {
        Path file = writeFile("unknown-field.json",
                "{\"filter\":{\"includeRegions\":[\"USA\"]},\"notARealSection\":{\"foo\":1}}");
        Profile profile = helper.loadProfile(file);
        assertEquals(ImmutableSet.of("USA"), profile.getFilter().getIncludeRegions());
    }

    // Row 7: a bad enum value fails, and the wrapped exception names the offending file.
    @Test
    void loadProfileWrapsBadEnumValueWithFilePath() throws Exception {
        Path file = writeFile("bad-enum.json", "{\"sort\":{\"versions\":\"not-a-real-value\"}}");
        IOException thrown = assertThrows(IOException.class, () -> helper.loadProfile(file));
        assertTrue(
                thrown.getMessage().contains(file.toString()),
                "exception message must contain the offending file's path, got: " + thrown.getMessage());
    }

    // Row 5, file-backed: both output.file and output.text set in a loaded profile is rejected
    // by loadProfile (which calls Profile#validate()), again naming the offending file.
    @Test
    void loadProfileWrapsMutuallyExclusiveOutputSectionWithFilePath() throws Exception {
        Path file = writeFile("bad-output.json", """
                {
                  "output": {
                    "file": {"outputDir": "out", "alphabetic": false, "forceSubfolder": false},
                    "text": {"outputMode": "json"}
                  }
                }
                """);
        IOException thrown = assertThrows(IOException.class, () -> helper.loadProfile(file));
        assertTrue(
                thrown.getMessage().contains(file.toString()),
                "exception message must contain the offending file's path, got: " + thrown.getMessage());
        assertTrue(
                thrown.getMessage().contains("mutually exclusive"),
                "exception message must explain the mutual exclusivity, got: " + thrown.getMessage());
    }

    // Row 7 corollary: parsing a malformed file that isn't even valid JSON/YAML also names the
    // file in the wrapped exception.
    @Test
    void loadProfileWrapsUnparsableFileWithFilePath() throws Exception {
        Path file = writeFile("unparsable.json", "{ this is not valid json or yaml : [ ");
        IOException thrown = assertThrows(IOException.class, () -> helper.loadProfile(file));
        assertTrue(
                thrown.getMessage().contains(file.toString()),
                "exception message must contain the offending file's path, got: " + thrown.getMessage());
    }

    // Issue #31 review fix B: an explicit null in a later profile file CLEARS the earlier
    // file's value (RFC 7386 JSON merge patch semantics) rather than being treated as an
    // absent field. Layering a file-output profile with a second file that nulls
    // output.file and sets output.text must merge to text-only output that passes
    // Profile#validate() (output.file/output.text are mutually exclusive).
    @Test
    void loadProfilesExplicitNullClearsEarlierFileOutputSection() throws Exception {
        Path first = writeFile("first.json", """
                {
                  "output": {
                    "file": {"outputDir": "out", "alphabetic": false, "forceSubfolder": false}
                  }
                }
                """);
        Path second = writeFile("second.json", """
                {
                  "output": {
                    "file": null,
                    "text": {"outputMode": "json"}
                  }
                }
                """);

        Profile merged = helper.loadProfiles(List.of(first, second));

        assertDoesNotThrow(
                merged::validate,
                "explicit null on output.file must clear it, leaving only output.text set");
        assertNull(
                merged.getOutput().getFile(),
                "output.file must be cleared by the second file's explicit null");
        assertEquals(
                OutputMode.JSON,
                merged.getOutput().getText().outputMode(),
                "output.text must come from the second file");
    }

    // Issue #31 review fix C: a profile file whose entire document is a literal `null` (valid
    // YAML/JSON, but not an object) must not proceed to Profile#validate() on a null Profile
    // reference (NullPointerException) -- it must fail cleanly as an IOException naming the
    // file, exactly like every other malformed-profile case above.
    @Test
    void loadProfileRejectsNullYamlDocumentWithFilePath() throws Exception {
        Path file = writeFile("null-document.yaml", "null\n");
        IOException thrown = assertThrows(IOException.class, () -> helper.loadProfile(file));
        assertTrue(
                thrown.getMessage().contains(file.toString()),
                "exception message must contain the offending file's path, got: " + thrown.getMessage());
    }

    @Test
    void loadProfileRejectsNullJsonDocumentWithFilePath() throws Exception {
        Path file = writeFile("null-document.json", "null");
        IOException thrown = assertThrows(IOException.class, () -> helper.loadProfile(file));
        assertTrue(
                thrown.getMessage().contains(file.toString()),
                "exception message must contain the offending file's path, got: " + thrown.getMessage());
    }
}
