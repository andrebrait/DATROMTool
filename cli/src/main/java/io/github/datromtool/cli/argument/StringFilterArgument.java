package io.github.datromtool.cli.argument;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static lombok.AccessLevel.PRIVATE;

@Value
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_NULL)
public class StringFilterArgument {

    @NonNull
    @Builder.Default
    List<String> strings = ImmutableList.of();

    @NonNull
    @Builder.Default
    List<Pattern> patterns = ImmutableList.of();
}
