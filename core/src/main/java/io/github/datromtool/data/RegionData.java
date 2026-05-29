package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.Patterns;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@JsonInclude(NON_DEFAULT)
public record RegionData(@Nonnull ImmutableSet<RegionDataEntry> regions) {

    public RegionData {
        if (regions == null) regions = ImmutableSet.of();
    }

    @JsonInclude(NON_DEFAULT)
    public record RegionDataEntry(
            @Nonnull String code,
            @Nonnull Pattern pattern,
            @Nonnull ImmutableSet<String> languages) {

        public RegionDataEntry {
            if (pattern == null) pattern = Patterns.NO_MATCH;
            if (languages == null) languages = ImmutableSet.of();
        }

        public RegionDataEntry(String code) {
            this(code, Patterns.NO_MATCH, ImmutableSet.of());
        }
    }
}
