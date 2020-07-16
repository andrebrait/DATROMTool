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
public class Filter {

    @NonNull
    @Builder.Default
    ImmutableSet<String> regions = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableSet<String> languages = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableSet<Pattern> excludes = ImmutableSet.of();

    /*
     * Adding the filters below here saves re-checking all names
     */

    @Builder.Default
    boolean noProto = false;

    @Builder.Default
    boolean noBeta = false;

    @Builder.Default
    boolean noDemo = false;

    @Builder.Default
    boolean noSample = false;

    @Builder.Default
    boolean noBios = false;

}
