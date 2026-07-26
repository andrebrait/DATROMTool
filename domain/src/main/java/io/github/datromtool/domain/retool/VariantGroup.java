package io.github.datromtool.domain.retool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

/**
 * A single entry in a clone list's {@code variants} array, per
 * <a href="https://unexpectedpanda.github.io/retool/contribute-clone-lists-variants/">
 * contribute-clone-lists-variants</a>: "The {@code group} value is used as the new group name
 * and short name for all of the titles" it contains. Upstream states a variant "must include at
 * least one of: titles, supersets, or compilations" - that cross-field invariant is left
 * unenforced at parse time (consistent with {@code Profile}'s pattern of deferring validation
 * past construction), since enforcing it here would reject documents Jackson can otherwise bind
 * cleanly; it is step 2/3's concern if it needs enforcing at all.
 *
 * <p>{@link #ignore()} "force removes the title from Retool's consideration" - upstream cautions
 * it "should almost never be used". Modeled as a plain {@code boolean} defaulting to
 * {@code false}.
 */
@JsonInclude(NON_DEFAULT)
public record VariantGroup(
        @Nonnull
        @JsonProperty(required = true)
        String group,

        @Nonnull
        ImmutableList<String> categories,

        boolean ignore,

        @Nonnull
        ImmutableList<VariantTitle> titles,

        @Nonnull
        ImmutableList<VariantTitle> supersets,

        @Nonnull
        ImmutableList<VariantCompilation> compilations) {

    public VariantGroup {
        if (categories == null) categories = ImmutableList.of();
        if (titles == null) titles = ImmutableList.of();
        if (supersets == null) supersets = ImmutableList.of();
        if (compilations == null) compilations = ImmutableList.of();
    }
}
