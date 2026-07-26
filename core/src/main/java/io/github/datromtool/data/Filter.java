package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableSet;
import lombok.*;
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
public class Filter {

    @NonNull
    @Builder.Default
    ImmutableSet<String> includeRegions = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableSet<String> excludeRegions = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableSet<String> includeLanguages = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableSet<String> excludeLanguages = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableSet<Pattern> excludes = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableSet<Pattern> includes = ImmutableSet.of();

    /*
     * Adding the filter below here saves re-checking all names
     */

    @NonNull
    @Builder.Default
    ImmutableSet<GameCategory> excludeCategories = ImmutableSet.of();
}
