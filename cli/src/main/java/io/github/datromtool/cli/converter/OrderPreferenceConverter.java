package io.github.datromtool.cli.converter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import io.github.datromtool.data.OrderPreference;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Locale;

public final class OrderPreferenceConverter
        implements CommandLine.ITypeConverter<OrderPreference>, Iterable<String> {

    private final static ImmutableList<String> aliases = Arrays.stream(OrderPreference.values())
            .map(Enum::name)
            .map(n -> n.toLowerCase(Locale.ROOT))
            .collect(ImmutableList.toImmutableList());

    @Override
    @Nonnull
    public UnmodifiableIterator<String> iterator() {
        return aliases.iterator();
    }

    @Override
    public OrderPreference convert(String value) {
        String trimmed = value.trim();
        return aliases.stream()
                .filter(c -> c.equalsIgnoreCase(trimmed))
                .findFirst()
                .map(c -> c.toUpperCase(Locale.ROOT))
                .map(OrderPreference::valueOf)
                .orElseThrow(() -> new CommandLine.TypeConversionException(
                        String.format(
                                "'%s' is not a valid order value. It must be one of %s",
                                value,
                                aliases)));
    }
}
