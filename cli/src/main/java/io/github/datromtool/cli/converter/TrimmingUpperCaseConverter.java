package io.github.datromtool.cli.converter;

import picocli.CommandLine;

import java.util.Locale;

public final class TrimmingUpperCaseConverter implements CommandLine.ITypeConverter<String> {

    @Override
    public String convert(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            // A blank value reads as "no restriction" but would become a literal empty code,
            // which matches no game at all - a silently empty result set.
            throw new CommandLine.TypeConversionException(
                    "'" + value + "' is blank: omit the option entirely to apply no restriction");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }
}
