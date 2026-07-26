package io.github.datromtool.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * Deep-merge for two config trees, later ("overlay") winning over earlier ("base"). Used by
 * {@link io.github.datromtool.SerializationHelper#loadProfiles} to layer multiple
 * {@link Profile} files (issue #15) before binding the merged tree to Java, sidestepping update
 * semantics on the immutable {@code @Value} stage classes entirely.
 *
 * <p>The rule: JSON/YAML <em>objects</em> merge recursively, field by field; everything else —
 * scalars, arrays, and mismatched node types — is replaced wholesale by the overlay's value. A
 * field present only in {@code base} (absent from {@code overlay}) survives unchanged.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonNodeMerge {

    public static JsonNode merge(JsonNode base, JsonNode overlay) {
        if (overlay == null || overlay.isMissingNode() || overlay.isNull()) {
            return base;
        }
        if (base == null || base.isMissingNode()) {
            return overlay;
        }
        if (base.isObject() && overlay.isObject()) {
            ObjectNode result = ((ObjectNode) base).deepCopy();
            for (Map.Entry<String, JsonNode> entry : overlay.properties()) {
                result.set(entry.getKey(), merge(result.get(entry.getKey()), entry.getValue()));
            }
            return result;
        }
        return overlay;
    }
}
