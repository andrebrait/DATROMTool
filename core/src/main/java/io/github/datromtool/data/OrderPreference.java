package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Replaces the direction-ambiguous {@code early*} boolean pairs on {@link SortingPreference}
 * ({@code earlyVersions}/{@code earlyRevisions}/{@code earlyPrereleases}) with an explicit,
 * self-describing order.
 */
public enum OrderPreference {
    LATEST,
    EARLIEST;

    /**
     * Serializes lowercase, matching the {@code type}/{@code archiveType} lowercase contract
     * elsewhere in the profile schema ({@link NameMatcher.MatchType},
     * {@link io.github.datromtool.io.ArchiveType}). Reads stay case-insensitive via
     * {@code MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS} on the shared mappers.
     */
    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
