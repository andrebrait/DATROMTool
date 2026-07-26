package io.github.datromtool.domain.retool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

/**
 * A single entry inside a clone list variant's {@code compilations} array, per
 * <a href="https://unexpectedpanda.github.io/retool/contribute-clone-lists-variants-compilations/">
 * contribute-clone-lists-variants-compilations</a>. Unlike {@link VariantTitle} (used for
 * {@code titles}/{@code supersets}), upstream documents compilations with a narrower field set -
 * no {@code englishFriendly}, {@code isOldest}, or {@code filters} - plus the compilation-only
 * {@link #titlePosition()}.
 *
 * <p>{@link #titlePosition()} addresses No-Intro's {@code +} separator notation in language tags
 * for compilations, assigning different languages to each title bundled in the compilation:
 * "The value indicates which title's position within the compilation should be extracted." It
 * has no spec-stated default (absent means "not a multi-title-tagged compilation") so it is
 * modeled as a nullable {@link Integer}, not a primitive - applying that slicing is step 2/3's
 * concern.
 *
 * <p>{@link #nameType()} and {@link #priority()} default identically to {@link VariantTitle}'s
 * (see that type's Javadoc for the primitive-{@code priority}-vs-{@code 0} note).
 */
@JsonInclude(NON_DEFAULT)
public record VariantCompilation(
        @Nonnull
        @JsonProperty(required = true)
        String searchTerm,

        @Nonnull
        NameType nameType,

        int priority,

        @Nonnull
        ImmutableList<String> categories,

        Integer titlePosition,

        @Nonnull
        ImmutableMap<String, String> localNames) {

    public VariantCompilation {
        if (nameType == null) nameType = NameType.SHORT;
        if (priority == 0) priority = 1;
        if (categories == null) categories = ImmutableList.of();
        if (localNames == null) localNames = ImmutableMap.of();
    }
}
