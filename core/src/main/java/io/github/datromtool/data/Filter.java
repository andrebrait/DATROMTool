package io.github.datromtool.data;

import com.google.common.collect.ImmutableSet;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.regex.Pattern;

@Value
@Builder
public class Filter {

    public enum Mode {
        DEFAULT, ALL_REGIONS, ALL_WITH_LANG, ONLY_WITH_LANG;
    }

    @NonNull
    ImmutableSet<String> regions;
    @NonNull
    ImmutableSet<String> languages;
    @NonNull
    ImmutableSet<Pattern> negativeFilters;
    @NonNull
    ImmutableSet<Pattern> excludes;
    @NonNull
    Mode mode;

}
