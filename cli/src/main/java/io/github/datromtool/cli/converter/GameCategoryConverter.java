package io.github.datromtool.cli.converter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import io.github.datromtool.data.GameCategory;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import java.util.Arrays;

public final class GameCategoryConverter
        implements CommandLine.ITypeConverter<GameCategory>, Iterable<String> {

    private final static ImmutableList<String> names = Arrays.stream(GameCategory.values())
            .map(Enum::name)
            .collect(ImmutableList.toImmutableList());

    @Override
    @Nonnull
    public UnmodifiableIterator<String> iterator() {
        return names.iterator();
    }

    @Override
    public GameCategory convert(String value) {
        return names.stream()
                .filter(n -> n.equalsIgnoreCase(value))
                .findFirst()
                .map(GameCategory::valueOf)
                .orElseThrow(() -> new CommandLine.TypeConversionException(
                        String.format(
                                "'%s' is not a valid category value. It must be one of %s",
                                value,
                                names)));
    }
}
