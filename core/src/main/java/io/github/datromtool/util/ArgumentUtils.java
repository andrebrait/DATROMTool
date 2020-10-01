package io.github.datromtool.util;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArgumentUtils {

    public static ImmutableList<Pattern> parsePatterns(Path file) throws IOException {
        if (file == null) {
            return ImmutableList.of();
        }
        return Files.readAllLines(file)
                .stream()
                .map(Pattern::compile)
                .collect(ImmutableList.toImmutableList());
    }

    public static ImmutableList<Pattern> combine(List<Pattern> patterns, List<Path> files)
            throws ArgumentException {
        ImmutableList.Builder<Pattern> builder = ImmutableList.builder();
        builder.addAll(patterns);
        for (Path file : files) {
            try {
                builder.addAll(ArgumentUtils.parsePatterns(file));
            } catch (Exception e) {
                throw new ArgumentException(String.format("Error processing '%s'", file), e);
            }
        }
        return builder.build();
    }

}
