package io.github.datromtool.cli.converter;

import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;

/**
 * Validates that a value names an existing regular file <em>or</em> directory (issue #19 step
 * 3: {@code --clonelist}/{@code --retool-metadata}, whose value may be either a single data file
 * or a directory to auto-match a file within by DAT header name - see
 * {@code io.github.datromtool.retool.RetoolFileResolver}). Unlike {@link ExistingFileConverter}
 * ({@code --profile}, file-only) and {@link ExistingDirectoryConverter} ({@code --in-dir},
 * directory-only), which each accept exactly one kind of path.
 */
public final class ExistingPathConverter implements CommandLine.ITypeConverter<Path> {

    @Override
    public Path convert(String s) {
        Path path = Paths.get(s);
        if (!Files.isRegularFile(path) && !Files.isDirectory(path)) {
            throw new CommandLine.TypeConversionException(format("No such file or directory: %s", s));
        }
        return path;
    }
}
