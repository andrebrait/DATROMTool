package io.github.datromtool.domain.retool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

/**
 * A single title entry inside a clone list variant's {@code titles} or {@code supersets} array -
 * upstream documents both as sharing this exact field set:
 * <a href="https://unexpectedpanda.github.io/retool/contribute-clone-lists-variants-titles/">
 * contribute-clone-lists-variants-titles</a> and
 * <a href="https://unexpectedpanda.github.io/retool/contribute-clone-lists-variants-supersets/">
 * contribute-clone-lists-variants-supersets</a>. {@code compilations} entries have a different,
 * narrower shape - see {@link VariantCompilation}.
 *
 * <p>Only {@link #searchTerm()} is required; the rest default per spec: {@link #nameType()} to
 * {@link NameType#SHORT}, {@link #priority()} to {@code 1}, {@link #englishFriendly()} and
 * {@link #isOldest()} to {@code false}. {@code priority} is modeled as a primitive {@code int}
 * per the spec's stated type ("priority (int, default 1)"); since JSON omission and an explicit
 * {@code 0} are indistinguishable for a primitive, and a real {@code 0} priority is not a
 * meaningful value under the spec's own "1 is highest priority" rule, the compact constructor
 * folds both cases to {@code 1}.
 *
 * <p>Upstream gives no indication titles ever use a bare-string shorthand instead of this object
 * shape (checked against the live spec pages above, which show object examples exclusively) - a
 * bare string here is therefore treated as a parse error, not modeled as an alternate shape.
 */
@JsonInclude(NON_DEFAULT)
public record VariantTitle(
        @Nonnull
        @JsonProperty(required = true)
        String searchTerm,

        @Nonnull
        NameType nameType,

        int priority,

        @Nonnull
        ImmutableList<String> categories,

        boolean englishFriendly,

        boolean isOldest,

        @Nonnull
        ImmutableMap<String, String> localNames,

        @Nonnull
        ImmutableList<VariantFilter> filters) {

    public VariantTitle {
        if (nameType == null) nameType = NameType.SHORT;
        if (priority == 0) priority = 1;
        if (categories == null) categories = ImmutableList.of();
        if (localNames == null) localNames = ImmutableMap.of();
        if (filters == null) filters = ImmutableList.of();
    }
}
