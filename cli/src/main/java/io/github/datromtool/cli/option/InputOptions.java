package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.cli.converter.ExistingDirectoryConverter;
import lombok.Data;
import lombok.NoArgsConstructor;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@Data
@NoArgsConstructor
@JsonInclude(NON_DEFAULT)
public final class InputOptions {

    public static final String IN_DIR_OPTION = "--in-dir";

    @CommandLine.Option(
            names = IN_DIR_OPTION,
            paramLabel = "PATH",
            description = "Base directory for scanning ROM files",
            converter = ExistingDirectoryConverter.class)
    private List<Path> inputDirs = ImmutableList.of();

}
