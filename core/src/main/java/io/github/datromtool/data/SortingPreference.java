package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableSet;
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
public class SortingPreference {

    @NonNull
    @Builder.Default
    ImmutableSet<String> regions = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableSet<String> languages = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableSet<Pattern> prefers = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableSet<Pattern> avoids = ImmutableSet.of();

    @Builder.Default
    boolean prioritizeLanguages = false;

    @Builder.Default
    boolean earlyVersions = false;

    @Builder.Default
    boolean earlyRevisions = false;

    @Builder.Default
    boolean earlyPrereleases = false;

    @Builder.Default
    boolean preferParents = false;

    @Builder.Default
    boolean preferPrereleases = false;

}
