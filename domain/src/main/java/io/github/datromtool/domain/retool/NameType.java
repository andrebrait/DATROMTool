package io.github.datromtool.domain.retool;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * How a {@link VariantTitle#searchTerm()}/{@link VariantCompilation#searchTerm()} is matched
 * against names in the input DAT file, per the Retool clone list spec
 * (<a href="https://unexpectedpanda.github.io/retool/contribute-clone-lists-variants-titles/">
 * contribute-clone-lists-variants-titles</a>): "What name type the search term is, so Retool
 * can match it accurately against names in the input DAT file." {@code short} is the documented
 * default when the field is omitted.
 *
 * <p>{@code short} is a Java keyword, hence the {@code SHORT} constant name with an explicit
 * lowercase {@link JsonProperty} mapping, consistent with this codebase's lowercase-enum
 * contract (see {@link io.github.datromtool.domain.datafile.logiqx.enumerations.YesNo}).
 */
public enum NameType {

    @JsonProperty("short")
    @JsonEnumDefaultValue
    SHORT,

    @JsonProperty("full")
    FULL,

    @JsonProperty("regionFree")
    REGION_FREE,

    @JsonProperty("regex")
    REGEX
}
