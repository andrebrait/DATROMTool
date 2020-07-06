package io.github.datromtool.data;

import com.google.common.collect.ImmutableSet;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.regex.Pattern;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class Filter {

    public enum Mode {
        DEFAULT,
        ALL_REGIONS,
        ALL_WITH_LANG,
        ONLY_WITH_LANG
    }

    @NonNull
    @Builder.Default
    ImmutableSet<String> regions = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableSet<String> languages = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableSet<Pattern> negativeFilters = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableSet<Pattern> excludes = ImmutableSet.of();

    @NonNull
    @Builder.Default
    Mode mode = Mode.DEFAULT;

}
