package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.Patterns;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@Value
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
public class RegionData {

    @Value
    @Jacksonized
    @Builder(toBuilder = true)
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PRIVATE, force = true)
    @JsonInclude(NON_DEFAULT)
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
