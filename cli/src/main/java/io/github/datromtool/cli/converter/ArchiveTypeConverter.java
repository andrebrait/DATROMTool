package io.github.datromtool.cli.converter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import io.github.datromtool.io.ArchiveType;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import java.util.Arrays;

public final class ArchiveTypeConverter
        implements CommandLine.ITypeConverter<ArchiveType>, Iterable<String> {

    private final static ImmutableList<String> aliases = Arrays.stream(ArchiveType.values())
            .filter(ArchiveType::isAvailableAsOutput)
            .map(ArchiveType::getAlias)
            .collect(ImmutableList.toImmutableList());

    @Override
    @Nonnull
    public UnmodifiableIterator<String> iterator() {
        return aliases.iterator();
    }

    @Override
    public ArchiveType convert(String value) {
        String actualValue = value.startsWith(".")
                ? value.replaceFirst("^\\.+", "")
                : value;
        return aliases.stream()
                .filter(c -> c.equalsIgnoreCase(actualValue))
                .findFirst()
                .map(ArchiveType::fromAlias)
                .orElseThrow(() -> new CommandLine.TypeConversionException(
                        String.format(
                                "'%s' is not a valid archive type value. It must be one of %s",
                                value,
                                aliases)));
    }
}
