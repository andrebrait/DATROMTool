package io.github.datromtool.cli.converter;

import picocli.CommandLine;

import java.nio.file.Path;

import static java.lang.String.format;

public final class ExecutableFileConverter extends ExistingFileConverter {

    @Override
    public Path convert(String s) {
        Path path = super.convert(s);
        if (!path.toFile().canExecute()) {
            throw new CommandLine.TypeConversionException(format("File is not an executable: %s", s));
        }
        return path;
    }
}
