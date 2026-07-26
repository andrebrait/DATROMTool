package io.github.datromtool.domain.retool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

/**
 * Root type of a Retool clone list JSON file, per
 * <a href="https://unexpectedpanda.github.io/retool/contribute-clone-lists-top-level-keys/">
 * contribute-clone-lists-top-level-keys</a>: "{@code description (obj[str, str])} - Required"
 * and "{@code variants (array[obj])} - Optional. All keys are optional, except for
 * {@code description}."
 *
 * <p>Unknown JSON fields anywhere under this type are tolerated, not rejected - the shared
 * {@code jsonMapper} in {@code SerializationHelper} has {@code FAIL_ON_UNKNOWN_PROPERTIES}
 * disabled - since upstream clone list files carry fields (and whole sub-shapes, e.g. future
 * spec additions) this step does not yet model.
 */
@JsonInclude(NON_DEFAULT)
public record CloneList(
        @Nonnull
        @JsonProperty(required = true)
        CloneListDescription description,

        @Nonnull
        ImmutableList<VariantGroup> variants) {

    public CloneList {
        if (variants == null) variants = ImmutableList.of();
    }
}
