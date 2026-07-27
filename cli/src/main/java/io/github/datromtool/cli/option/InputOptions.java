package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.cli.converter.ExistingDirectoryConverter;
import io.github.datromtool.cli.converter.ExistingPathConverter;
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
    public static final String CLONELIST_OPTION = "--clonelist";
    public static final String RETOOL_METADATA_OPTION = "--retool-metadata";

    @CommandLine.Option(
            names = IN_DIR_OPTION,
            paramLabel = "PATH",
            description = "Base directory for scanning ROM files",
            converter = ExistingDirectoryConverter.class)
    private List<Path> inputDirs = ImmutableList.of();

    @CommandLine.Option(
            names = CLONELIST_OPTION,
            paramLabel = "PATH",
            converter = ExistingPathConverter.class,
            description = "Retool clone list JSON file, or a directory of clone list files to "
                    + "auto-match by the DAT header name (issue #19). Drives 1G1R grouping for "
                    + "DATs without parent/clone information.")
    private Path clonelist;

    @CommandLine.Option(
            names = RETOOL_METADATA_OPTION,
            paramLabel = "PATH",
            converter = ExistingPathConverter.class,
            description = "Retool metadata JSON file, or a directory of metadata files to "
                    + "auto-match by the DAT header name (issue #19). Supplements language data "
                    + "missing from the DAT.")
    private Path retoolMetadata;

}
