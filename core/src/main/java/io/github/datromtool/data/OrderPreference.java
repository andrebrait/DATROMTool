package io.github.datromtool.data;

/**
 * Replaces the direction-ambiguous {@code early*} boolean pairs on {@link SortingPreference}
 * ({@code earlyVersions}/{@code earlyRevisions}/{@code earlyPrereleases}) with an explicit,
 * self-describing order.
 */
public enum OrderPreference {
    LATEST,
    EARLIEST
}
