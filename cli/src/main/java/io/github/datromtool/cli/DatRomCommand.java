package io.github.datromtool.cli;

import io.github.datromtool.cli.command.OneGameOneRomCommand;
import picocli.CommandLine;

@CommandLine.Command(
        name = "datrom",
        description = "Base command for " + GitVersionProvider.TITLE,
        versionProvider = GitVersionProvider.class,
        mixinStandardHelpOptions = true,
        subcommands = {OneGameOneRomCommand.class})
public final class DatRomCommand {

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new DatRomCommand());
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

}
