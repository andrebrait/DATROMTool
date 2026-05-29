package io.github.datromtool.cli.argument;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record StringFilterArgument(@Nonnull List<String> strings, @Nonnull List<Pattern> patterns) {

    public StringFilterArgument {
        strings = strings != null ? strings : ImmutableList.of();
        patterns = patterns != null ? patterns : ImmutableList.of();
    }
}
