package io.github.datromtool.cli.converter;

import io.github.datromtool.cli.argument.DatafileArgument;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Paths;

public final class DatafileConverter implements CommandLine.ITypeConverter<DatafileArgument> {

    @Override
    public DatafileArgument convert(String s) {
        try {
            return DatafileArgument.from(Paths.get(s));
        } catch (IOException e) {
            throw new CommandLine.TypeConversionException(
                    String.format("Cannot read DAT from file '%s' (%s)", s, e));
        } catch (Exception e) {
            throw new CommandLine.TypeConversionException(
                    String.format("Internal error while reading DAT from file '%s' (%s)", s, e));
        }
    }
}
