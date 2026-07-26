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
 * field genuinely <em>absent</em> from {@code overlay} survives from {@code base} unchanged, but
 * an explicit {@code null} in {@code overlay} for a field <em>clears</em> that field entirely
 * (RFC 7386 JSON merge patch semantics) rather than being treated as absent — the field is
 * removed so binding falls back to the class's own default, exactly as if it had never been set
 * by any layered file.
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
                JsonNode overlayValue = entry.getValue();
                if (overlayValue.isNull()) {
                    // RFC 7386: explicit null clears the field -- never re-merge it against
                    // base, and never leave it behind as a literal null either.
                    result.remove(entry.getKey());
                } else {
                    result.set(entry.getKey(), merge(result.get(entry.getKey()), overlayValue));
                }
            }
            return result;
        }
        return overlay;
    }
}
