package io.github.datromtool.cli.command;

import io.github.datromtool.cli.GitVersionProvider;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * Issue #16: command group for DAT-file-level operations (currently just {@code convert}).
 * Prints its own usage and exits non-zero when invoked with no subcommand, rather than falling
 * through to picocli's default {@code ExecutionException} ("is not a Method, Runnable or
 * Callable") for a non-{@link Callable} group command.
 */
@CommandLine.Command(
        name = "dat",
        description = "Work with DAT files (Logiqx XML, DATROMTool JSON, and DATROMTool YAML)",
        sortOptions = false,
        abbreviateSynopsis = true,
        versionProvider = GitVersionProvider.class,
        mixinStandardHelpOptions = true,
        subcommands = {DatConvertCommand.class})
public final class DatCommand implements Callable<Integer> {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec commandSpec;

    @Override
    public Integer call() {
        commandSpec.commandLine().usage(System.out);
        return CommandLine.ExitCode.USAGE;
    }
}
