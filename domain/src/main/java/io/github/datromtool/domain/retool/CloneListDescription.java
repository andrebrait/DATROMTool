package io.github.datromtool.domain.retool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

/**
 * The {@code description} block of a Retool clone list, per
 * <a href="https://unexpectedpanda.github.io/retool/contribute-clone-lists-description/">
 * contribute-clone-lists-description</a>. All three fields are documented as required, though
 * upstream notes only {@link #minimumVersion()} is actually consumed by Retool itself; {@link
 * #name()} and {@link #lastUpdated()} exist for human maintainers. The compatibility gate that
 * reads {@link #minimumVersion()} is out of scope for this step (tracked for issue #19 step
 * 2/3) - it is modeled here so the data is available when that gate lands.
 */
@JsonInclude(NON_DEFAULT)
public record CloneListDescription(
        @Nonnull
        @JsonProperty(required = true)
        String name,

        @Nonnull
        @JsonProperty(required = true)
        String lastUpdated,

        @Nonnull
        @JsonProperty(required = true)
        String minimumVersion) {
}
