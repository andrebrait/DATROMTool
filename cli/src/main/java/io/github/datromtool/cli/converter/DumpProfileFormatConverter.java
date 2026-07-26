package io.github.datromtool.cli.converter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import io.github.datromtool.cli.command.DumpProfileFormat;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Locale;

public final class DumpProfileFormatConverter
        implements CommandLine.ITypeConverter<DumpProfileFormat>, Iterable<String> {

    private static final ImmutableList<String> names = Arrays.stream(DumpProfileFormat.values())
            .map(Enum::name)
            .map(n -> n.toLowerCase(Locale.ROOT))
            .collect(ImmutableList.toImmutableList());

    @Override
    @Nonnull
    public UnmodifiableIterator<String> iterator() {
        return names.iterator();
    }

    @Override
    public DumpProfileFormat convert(String value) {
        String trimmed = value.trim();
        return names.stream()
                .filter(n -> n.equalsIgnoreCase(trimmed))
                .findFirst()
                .map(n -> n.toUpperCase(Locale.ROOT))
                .map(DumpProfileFormat::valueOf)
                .orElseThrow(() -> new CommandLine.TypeConversionException(
                        String.format(
                                "'%s' is not a valid profile dump format. It must be one of %s",
                                value,
                                names)));
    }
}
