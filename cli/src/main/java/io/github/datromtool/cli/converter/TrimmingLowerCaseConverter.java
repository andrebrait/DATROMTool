package io.github.datromtool.cli.converter;

import picocli.CommandLine;

public final class TrimmingLowerCaseConverter implements CommandLine.ITypeConverter<String> {

    @Override
    public String convert(String value) {
        return value.trim().toLowerCase();
    }
}
