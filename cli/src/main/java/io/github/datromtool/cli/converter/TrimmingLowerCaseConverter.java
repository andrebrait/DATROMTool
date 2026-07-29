package io.github.datromtool.cli.converter;

import picocli.CommandLine;

import java.util.Locale;

public final class TrimmingLowerCaseConverter implements CommandLine.ITypeConverter<String> {

    @Override
    public String convert(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
