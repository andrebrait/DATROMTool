package io.github.datromtool.data;

import com.google.common.collect.ImmutableSet;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.regex.Pattern;

@Value
@Builder
public class RegionData {

    @Value
    @Builder
    public static class RegionDataEntry {
        @NonNull
        String code;
        @NonNull
        Pattern pattern;
        @NonNull
        ImmutableSet<String> languages;
    }

    @NonNull
    ImmutableSet<RegionDataEntry> regions;

}
