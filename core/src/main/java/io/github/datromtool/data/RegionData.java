package io.github.datromtool.data;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Value;

import java.util.regex.Pattern;

@Value
@Builder
public class RegionData {

    @Value
    @Builder
    public static class RegionDataEntry {

        String code;
        Pattern pattern;
        ImmutableList<String> languages;
    }

    ImmutableList<RegionDataEntry> regions;

}
