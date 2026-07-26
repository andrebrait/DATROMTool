package io.github.datromtool.cli.converter;

import io.github.datromtool.config.ScanThreads;
import picocli.CommandLine;

public final class ScanThreadsConverter implements CommandLine.ITypeConverter<ScanThreads> {

    @Override
    public ScanThreads convert(String value) {
        return new ScanThreads(Integer.parseInt(value));
    }
}
