package io.github.datromtool.cli.argument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
public final class PatternsFileArgument {

    private final Path path;
    @JsonIgnore
    private final ImmutableList<Pattern> patterns;

    @JsonCreator
    public static PatternsFileArgument from(@JsonProperty("path") Path path) throws IOException {
        ImmutableList<Pattern> patterns = Files.readAllLines(path).stream()
                .map(Pattern::compile)
                .collect(ImmutableList.toImmutableList());
        return new PatternsFileArgument(path, patterns);
    }

}

