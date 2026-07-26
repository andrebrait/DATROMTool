package io.github.datromtool.config;

import io.github.datromtool.SerializationHelper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Direct tests for {@link JsonNodeMerge}, the tree-level deep-merge backing
 * {@link io.github.datromtool.SerializationHelper#loadProfiles} (issue #15 step 2, coverage
 * matrix row 4). Each test pins one clause of the documented rule: objects merge recursively,
 * field by field; everything else (scalars, arrays) is replaced wholesale by the overlay.
 */
class JsonNodeMergeTest {

    private static final JsonMapper MAPPER = SerializationHelper.getInstance().getJsonMapper();

    private static JsonNode node(String json) {
        return MAPPER.readTree(json);
    }

    // Row 4(a): array-valued fields (e.g. sort.regions) replace wholesale, never append/merge.
    @Test
    void arrayFieldReplacesWholesale() {
        JsonNode base = node("{\"sort\":{\"regions\":[\"USA\",\"Japan\"]}}");
        JsonNode overlay = node("{\"sort\":{\"regions\":[\"Europe\"]}}");
        assertEquals(node("{\"sort\":{\"regions\":[\"Europe\"]}}"), JsonNodeMerge.merge(base, overlay));
    }

    // Row 4(b): setting only filter.includeLanguages in the overlay keeps base's
    // filter.includeRegions untouched (object recursive merge, field by field).
    @Test
    void siblingObjectFieldSurvivesRecursiveMerge() {
        JsonNode base = node("{\"filter\":{\"includeRegions\":[\"USA\"],\"includeLanguages\":[\"En\"]}}");
        JsonNode overlay = node("{\"filter\":{\"includeLanguages\":[\"Ja\"]}}");
        assertEquals(
                node("{\"filter\":{\"includeRegions\":[\"USA\"],\"includeLanguages\":[\"Ja\"]}}"),
                JsonNodeMerge.merge(base, overlay));
    }

    // Row 4(c): scalar override (performance.scanner.threads).
    @Test
    void scalarFieldReplaces() {
        JsonNode base = node("{\"performance\":{\"scanner\":{\"threads\":4}}}");
        JsonNode overlay = node("{\"performance\":{\"scanner\":{\"threads\":8}}}");
        assertEquals(node("{\"performance\":{\"scanner\":{\"threads\":8}}}"), JsonNodeMerge.merge(base, overlay));
    }

    // Row 4(d): a section entirely absent from the overlay survives from base unchanged, while a
    // sibling section present in both still merges by field.
    @Test
    void sectionAbsentFromOverlaySurvives() {
        JsonNode base = node("{\"filter\":{\"includeRegions\":[\"USA\"]},\"sort\":{\"regions\":[\"USA\"]}}");
        JsonNode overlay = node("{\"filter\":{\"includeRegions\":[\"Japan\"]}}");
        assertEquals(
                node("{\"filter\":{\"includeRegions\":[\"Japan\"]},\"sort\":{\"regions\":[\"USA\"]}}"),
                JsonNodeMerge.merge(base, overlay));
    }

    // Row 4(e): three-file chain associativity spot check — folding left-to-right (as
    // loadProfiles does) must equal folding in the other grouping, since at any leaf path the
    // rightmost file that defines it always wins regardless of how the folds are grouped.
    @Test
    void threeWayChainIsOrderInsensitiveToGrouping() {
        JsonNode a = node("{\"filter\":{\"includeRegions\":[\"USA\"],\"includeLanguages\":[\"En\"]}}");
        JsonNode b = node("{\"filter\":{\"includeRegions\":[\"Japan\"]}}");
        JsonNode c = node("{\"filter\":{\"includeLanguages\":[\"Ja\"]}}");

        JsonNode leftFold = JsonNodeMerge.merge(JsonNodeMerge.merge(a, b), c);
        JsonNode rightFold = JsonNodeMerge.merge(a, JsonNodeMerge.merge(b, c));

        assertEquals(leftFold, rightFold, "grouping must not change the merged result");
        assertEquals(
                node("{\"filter\":{\"includeRegions\":[\"Japan\"],\"includeLanguages\":[\"Ja\"]}}"),
                leftFold);
    }

    @Test
    void overlayWithoutFieldLeavesBaseFieldUnchanged() {
        JsonNode base = node("{\"a\":1}");
        JsonNode overlay = MAPPER.createObjectNode();
        assertEquals(base, JsonNodeMerge.merge(base, overlay));
    }

    @Test
    void nullBaseReturnsOverlayVerbatim() {
        JsonNode overlay = node("{\"a\":1}");
        assertEquals(overlay, JsonNodeMerge.merge(null, overlay));
    }

    @Test
    void mismatchedNodeTypesReplaceWholesale() {
        JsonNode base = node("{\"filter\":{\"includeRegions\":[\"USA\"]}}");
        JsonNode overlay = node("{\"filter\":\"reset\"}");
        assertEquals(node("{\"filter\":\"reset\"}"), JsonNodeMerge.merge(base, overlay));
    }

    // Issue #31 review fix B: an explicit JSON/YAML null in the overlay CLEARS the base's
    // value for that field entirely (RFC 7386 JSON merge patch semantics) -- contrast with
    // overlayWithoutFieldLeavesBaseFieldUnchanged, where the field is genuinely MISSING and
    // survives from base instead.
    @Test
    void explicitNullInOverlayClearsBaseField() {
        JsonNode base = node("{\"a\":1,\"b\":2}");
        JsonNode overlay = node("{\"a\":null}");
        assertEquals(node("{\"b\":2}"), JsonNodeMerge.merge(base, overlay));
    }

    @Test
    void explicitNullClearsNestedObjectFieldWhileSiblingIsSet() {
        JsonNode base = node("{\"output\":{\"file\":{\"outputDir\":\"out\"}}}");
        JsonNode overlay = node("{\"output\":{\"file\":null,\"text\":{\"outputMode\":\"json\"}}}");
        assertEquals(
                node("{\"output\":{\"text\":{\"outputMode\":\"json\"}}}"),
                JsonNodeMerge.merge(base, overlay));
    }
}
