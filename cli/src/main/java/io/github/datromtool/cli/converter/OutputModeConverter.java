package io.github.datromtool.cli.converter;

import io.github.datromtool.cli.OutputMode;
import io.github.datromtool.cli.OutputModeCompletionCandidates;
import picocli.CommandLine;

public final class OutputModeConverter implements CommandLine.ITypeConverter<OutputMode> {

    @Override
    public OutputMode convert(String value) {
        OutputModeCompletionCandidates candidates = new OutputModeCompletionCandidates();
        String found = candidates.getCandidatesStream()
                .filter(value::equalsIgnoreCase)
                .findFirst()
                .orElseThrow(() -> new CommandLine.TypeConversionException(
                        String.format(
                                "'%s' is not a valid archive type value. It must be one of %s",
                                value,
                                candidates)));
        return OutputMode.valueOf(found.toUpperCase());
    }
}
