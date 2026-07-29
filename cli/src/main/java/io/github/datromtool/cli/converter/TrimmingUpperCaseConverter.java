package io.github.datromtool.cli.converter;

import picocli.CommandLine;

import java.util.Locale;

public final class TrimmingUpperCaseConverter implements CommandLine.ITypeConverter<String> {

    @Override
    public String convert(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
