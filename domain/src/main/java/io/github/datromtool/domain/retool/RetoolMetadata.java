package io.github.datromtool.domain.retool;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * A Retool metadata file: title name (as it appears in the No-Intro/Redump DAT, no directory or
 * extension) to {@link MetadataEntry}, supplementing DATs lacking language data. Unlike {@link
 * CloneList}, upstream publishes no wrapper object for this file - the JSON root value
 * <em>is</em> the map, per
 * <a href="https://unexpectedpanda.github.io/retool/contribute-metadata-files/">
 * contribute-metadata-files</a> and confirmed against a real fixture (see core's test suite for
 * provenance) - so this type delegates (de)serialization straight to/from the map, the same
 * scalar-wrapper pattern this codebase uses for single-value types (e.g. core's
 * {@code ScanThreads}: a canonical constructor, a {@code @JsonValue} accessor, and a
 * {@code @JsonCreator} static factory), rather than modeling a synthetic top-level property that
 * would never appear in an actual file.
 */
public record RetoolMetadata(ImmutableMap<String, MetadataEntry> entries) {

    public RetoolMetadata {
        if (entries == null) entries = ImmutableMap.of();
    }

    @JsonValue
    public ImmutableMap<String, MetadataEntry> entries() {
        return entries;
    }

    @JsonCreator
    public static RetoolMetadata of(Map<String, MetadataEntry> entries) {
        return new RetoolMetadata(entries == null ? ImmutableMap.of() : ImmutableMap.copyOf(entries));
    }
}
