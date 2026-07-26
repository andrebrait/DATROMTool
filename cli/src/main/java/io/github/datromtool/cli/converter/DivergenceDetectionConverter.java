package io.github.datromtool.cli.converter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import io.github.datromtool.GameParser.DivergenceDetection;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Locale;

public final class DivergenceDetectionConverter
        implements CommandLine.ITypeConverter<DivergenceDetection>, Iterable<String> {

    private final static ImmutableList<String> aliases = Arrays.stream(DivergenceDetection.values())
            .map(Enum::name)
            .map(n -> n.toLowerCase(Locale.ROOT))
            .collect(ImmutableList.toImmutableList());

    @Override
    @Nonnull
    public UnmodifiableIterator<String> iterator() {
        return aliases.iterator();
    }

    @Override
    public DivergenceDetection convert(String value) {
        String trimmed = value.trim();
        return aliases.stream()
                .filter(c -> c.equalsIgnoreCase(trimmed))
                .findFirst()
                .map(c -> c.toUpperCase(Locale.ROOT))
                .map(DivergenceDetection::valueOf)
                .orElseThrow(() -> new CommandLine.TypeConversionException(
                        String.format(
                                "'%s' is not a valid divergence value. It must be one of %s",
                                value,
                                aliases)));
    }
}
