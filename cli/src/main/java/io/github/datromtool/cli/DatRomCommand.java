package io.github.datromtool.cli;

import io.github.datromtool.ByteSize;
import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.command.OneGameOneRomCommand;
import io.github.datromtool.cli.converter.*;
import io.github.datromtool.data.OutputMode;
import io.github.datromtool.io.ArchiveType;
import picocli.CommandLine;

@CommandLine.Command(
        name = "datrom",
        description = "DATROMTool - *that* tool to work with DATs and ROMs!",
        sortOptions = false,
        abbreviateSynopsis = true,
        versionProvider = GitVersionProvider.class,
        mixinStandardHelpOptions = true,
        showEndOfOptionsDelimiterInUsageHelp = true,
        subcommands = {OneGameOneRomCommand.class})
public final class DatRomCommand {

    @SuppressWarnings("InstantiationOfUtilityClass")
    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new DatRomCommand());
        cmd.registerConverter(ArchiveType.class, new ArchiveTypeConverter());
        cmd.registerConverter(OutputMode.class, new OutputModeConverter());
        cmd.registerConverter(PatternsFileArgument.class, new PatternsFileConverter());
        cmd.registerConverter(DatafileArgument.class, new DatafileConverter());
        cmd.registerConverter(ByteSize.class, new ByteSizeConverter());
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

}
