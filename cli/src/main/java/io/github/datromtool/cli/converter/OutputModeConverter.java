package io.github.datromtool.cli.converter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import io.github.datromtool.data.OutputMode;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Locale;

public final class OutputModeConverter
        implements CommandLine.ITypeConverter<OutputMode>, Iterable<String> {

    private final static ImmutableList<String> aliases = Arrays.stream(OutputMode.values())
            .map(Enum::name)
            .map(s -> s.toLowerCase(Locale.ROOT))
            .collect(ImmutableList.toImmutableList());

    @Override
    @Nonnull
    public UnmodifiableIterator<String> iterator() {
        return aliases.iterator();
    }

    @Override
    public OutputMode convert(String value) {
        return aliases.stream()
                .filter(c -> c.equalsIgnoreCase(value))
                .findFirst()
                .map(s -> s.toUpperCase(Locale.ROOT))
                .map(OutputMode::valueOf)
                .orElseThrow(() -> new CommandLine.TypeConversionException(
                        String.format(
                                "'%s' is not a valid output mode value. It must be one of %s",
                                value,
                                aliases)));
    }
}
