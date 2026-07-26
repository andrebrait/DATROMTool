package io.github.datromtool.cli.command;

import io.github.datromtool.cli.GitVersionProvider;
import picocli.CommandLine;

/**
 * Command group for DAT-file-level operations (currently just {@code convert}). Deliberately
 * not {@code Callable}: picocli's default handling of a bare group prints
 * "Missing required subcommand" plus usage on stderr and exits with the usage code, matching
 * the top-level {@code datrom} group's behavior.
 */
@CommandLine.Command(
        name = "dat",
        description = "Work with DAT files (Logiqx XML, DATROMTool JSON, and DATROMTool YAML)",
        sortOptions = false,
        abbreviateSynopsis = true,
        versionProvider = GitVersionProvider.class,
        mixinStandardHelpOptions = true,
        subcommands = {DatConvertCommand.class})
public final class DatCommand {
}
