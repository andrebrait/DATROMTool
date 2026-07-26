package io.github.datromtool.cli.converter;

import io.github.datromtool.config.CopyThreads;
import picocli.CommandLine;

public final class CopyThreadsConverter implements CommandLine.ITypeConverter<CopyThreads> {

    @Override
    public CopyThreads convert(String value) {
        return new CopyThreads(Integer.parseInt(value));
    }
}
