package io.github.datromtool.cli.converter;

import picocli.CommandLine;

public final class TrimmingUpperCaseConverter implements CommandLine.ITypeConverter<String> {

    @Override
    public String convert(String value) {
        return value.trim().toUpperCase();
    }
}
