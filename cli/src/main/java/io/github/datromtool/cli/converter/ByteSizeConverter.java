package io.github.datromtool.cli.converter;

import io.github.datromtool.ByteSize;
import picocli.CommandLine;

public final class ByteSizeConverter implements CommandLine.ITypeConverter<ByteSize> {

    @Override
    public ByteSize convert(String s) {
        return ByteSize.fromString(s);
    }
}
