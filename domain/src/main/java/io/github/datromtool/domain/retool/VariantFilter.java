package io.github.datromtool.domain.retool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

/**
 * A conditional filter attached to a {@link VariantTitle}, per
 * <a href="https://unexpectedpanda.github.io/retool/contribute-clone-lists-variants-filters/">
 * contribute-clone-lists-variants-filters</a>: "Treat some titles found by a search term
 * differently based on conditions." Applying these filters (matching {@link Conditions} against
 * a parsed game and the user's region order, then overlaying {@link Results}) is step 2/3's
 * concern (issue #19) - this type only models the data shape.
 *
 * <p>{@link Results} fields are modeled as nullable/absent-by-default (no forced spec default),
 * unlike the equivalent fields on {@link VariantTitle} itself: a result is an <em>override</em>
 * slot, and collapsing "absent" into a default value (e.g. {@code priority} defaulting to 1)
 * would fabricate an override that was never present in the source file. This is a deliberate
 * modeling choice, not a spec quote - the upstream docs reuse the same field-level prose
 * ("optional, defaults to 1") for both the title-level and results-level {@code priority}
 * field, which reads like shared boilerplate rather than a considered statement about override
 * semantics.
 */
@JsonInclude(NON_DEFAULT)
public record VariantFilter(Conditions conditions, Results results) {

    /**
     * The conditions that must hold for {@link #results()} to apply. All fields are optional;
     * multiple present fields are implicitly ANDed together (semantics for step 2).
     */
    @JsonInclude(NON_DEFAULT)
    public record Conditions(
            @Nonnull ImmutableList<String> matchLanguages,
            @Nonnull ImmutableList<String> matchRegions,
            String matchString,
            RegionOrder regionOrder) {

        public Conditions {
            if (matchLanguages == null) matchLanguages = ImmutableList.of();
            if (matchRegions == null) matchRegions = ImmutableList.of();
        }
    }

    /**
     * "If any of the regions in the {@code higherRegions} array is higher in the user region
     * order than all of the regions in the {@code lowerRegions} array, then the condition is
     * true." Either array may contain the literal placeholder {@code "All other regions"} for
     * automatic calculation (modeled as a plain string - no special-casing at this step).
     */
    @JsonInclude(NON_DEFAULT)
    public record RegionOrder(
            @Nonnull ImmutableList<String> higherRegions,
            @Nonnull ImmutableList<String> lowerRegions) {

        public RegionOrder {
            if (higherRegions == null) higherRegions = ImmutableList.of();
            if (lowerRegions == null) lowerRegions = ImmutableList.of();
        }
    }

    /**
     * The overrides applied when {@link Conditions} matches. See the type-level Javadoc for why
     * these fields are nullable rather than defaulted.
     */
    @JsonInclude(NON_DEFAULT)
    public record Results(
            String group,
            Integer priority,
            @Nonnull ImmutableList<String> categories,
            Boolean superset,
            Boolean englishFriendly,
            Boolean isOldest,
            @Nonnull ImmutableMap<String, String> localNames) {

        public Results {
            if (categories == null) categories = ImmutableList.of();
            if (localNames == null) localNames = ImmutableMap.of();
        }
    }
}
