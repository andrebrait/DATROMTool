package io.github.datromtool.cli.converter;

import io.github.datromtool.cli.argument.PatternsFileArgument;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Paths;

public final class PatternsFileConverter
        implements CommandLine.ITypeConverter<PatternsFileArgument> {

    @Override
    public PatternsFileArgument convert(String s) {
        try {
            return PatternsFileArgument.from(Paths.get(s));
        } catch (IOException e) {
            throw new CommandLine.TypeConversionException(
                    String.format("Cannot read patterns from file '%s' (%s)", s, e));
        } catch (Exception e) {
            throw new CommandLine.TypeConversionException(
                    String.format(
                            "Internal error while reading patterns from file '%s' (%s)",
                            s,
                            e));
        }
    }
}
