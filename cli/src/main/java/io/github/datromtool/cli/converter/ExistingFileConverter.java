package io.github.datromtool.cli.converter;

import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;

/**
 * Validates that a {@code --profile} value names an existing regular file, mirroring
 * {@link ExistingDirectoryConverter}'s existence check for {@code --in-dir}. Content validation
 * (JSON/YAML parseability, {@link io.github.datromtool.config.Profile} binding) happens later,
 * when the file is actually loaded via {@code SerializationHelper#loadProfiles}.
 */
public final class ExistingFileConverter implements CommandLine.ITypeConverter<Path> {

    @Override
    public Path convert(String s) {
        Path path = Paths.get(s);
        if (!Files.isRegularFile(path)) {
            throw new CommandLine.TypeConversionException(format("No such file: %s", s));
        }
        return path;
    }
}
