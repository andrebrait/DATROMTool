package io.github.datromtool.data;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.Patterns;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.regex.Pattern;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class RegionData {

    @Value
    @Jacksonized
    @Builder(toBuilder = true)
    public static class RegionDataEntry {

        @NonNull
        String code;

        @NonNull
        @Builder.Default
        Pattern pattern = Patterns.NO_MATCH;

        @NonNull
        @Builder.Default
        ImmutableSet<String> languages = ImmutableSet.of();

    }

    @NonNull
    @Builder.Default
    ImmutableSet<RegionDataEntry> regions = ImmutableSet.of();

}
