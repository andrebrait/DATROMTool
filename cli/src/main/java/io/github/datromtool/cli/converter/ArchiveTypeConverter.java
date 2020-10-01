package io.github.datromtool.cli.converter;

import io.github.datromtool.cli.ArchiveCompletionCandidates;
import io.github.datromtool.io.ArchiveType;
import picocli.CommandLine;

public final class ArchiveTypeConverter implements CommandLine.ITypeConverter<ArchiveType> {

    @Override
    public ArchiveType convert(String value) {
        String actualValue = value.startsWith(".") ? value.replaceFirst("^\\.+", "") : value;
        ArchiveCompletionCandidates candidates = new ArchiveCompletionCandidates();
        String found = candidates.getCandidatesStream()
                .filter(actualValue::equalsIgnoreCase)
                .findFirst()
                .orElseThrow(() -> new CommandLine.TypeConversionException(
                        String.format(
                                "'%s' is not a valid archive type value. It must be one of %s",
                                value,
                                candidates)));
        return ArchiveType.fromNameOrAlias(found.toLowerCase());
    }
}
