package io.github.datromtool.cli.converter;

import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;

public final class ExistingDirectoryConverter implements CommandLine.ITypeConverter<Path> {

    @Override
    public Path convert(String s) {
        Path path = Paths.get(s);
        if (!Files.isDirectory(path)) {
            throw new CommandLine.TypeConversionException(format("No such directory: %s", s));
        }
        return path;
    }
}
