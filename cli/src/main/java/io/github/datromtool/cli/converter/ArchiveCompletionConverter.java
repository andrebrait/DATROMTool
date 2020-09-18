package io.github.datromtool.cli.converter;

import io.github.datromtool.cli.ArchiveCompletionCandidates;
import io.github.datromtool.io.ArchiveType;
import picocli.CommandLine;

import java.util.stream.StreamSupport;

public final class ArchiveCompletionConverter implements CommandLine.ITypeConverter<ArchiveType> {

    @Override
    public ArchiveType convert(String value) {
        String found = StreamSupport.stream(new ArchiveCompletionCandidates().spliterator(), false)
                .filter(value::equalsIgnoreCase)
                .findFirst()
                .orElseThrow(() -> new CommandLine.TypeConversionException(
                        String.format("'%s' is not a valid archive type value", value)));
        return ArchiveType.valueOf(found);
    }
}
