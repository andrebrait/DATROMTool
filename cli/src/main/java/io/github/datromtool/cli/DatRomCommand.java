package io.github.datromtool.cli;

import io.github.datromtool.ByteSize;
import io.github.datromtool.GameParser;
import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.command.DatCommand;
import io.github.datromtool.cli.command.OneGameOneRomCommand;
import io.github.datromtool.cli.command.ScanCommand;
import io.github.datromtool.cli.converter.*;
import io.github.datromtool.config.CopyBufferSize;
import io.github.datromtool.config.CopyThreads;
import io.github.datromtool.config.ScanBufferSize;
import io.github.datromtool.config.ScanMaxBufferSize;
import io.github.datromtool.config.ScanThreads;
import io.github.datromtool.data.GameCategory;
import io.github.datromtool.data.OrderPreference;
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
        subcommands = {OneGameOneRomCommand.class, DatCommand.class, ScanCommand.class})
public final class DatRomCommand {

    @SuppressWarnings("InstantiationOfUtilityClass")
    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new DatRomCommand());
        cmd.registerConverter(ArchiveType.class, new ArchiveTypeConverter());
        cmd.registerConverter(GameCategory.class, new GameCategoryConverter());
        cmd.registerConverter(OrderPreference.class, new OrderPreferenceConverter());
        cmd.registerConverter(OutputMode.class, new OutputModeConverter());
        cmd.registerConverter(PatternsFileArgument.class, new PatternsFileConverter());
        cmd.registerConverter(DatafileArgument.class, new DatafileConverter());
        cmd.registerConverter(ByteSize.class, new ByteSizeConverter());
        cmd.registerConverter(ScanThreads.class, new ScanThreadsConverter());
        cmd.registerConverter(CopyThreads.class, new CopyThreadsConverter());
        cmd.registerConverter(ScanBufferSize.class, new ScanBufferSizeConverter());
        cmd.registerConverter(ScanMaxBufferSize.class, new ScanMaxBufferSizeConverter());
        cmd.registerConverter(CopyBufferSize.class, new CopyBufferSizeConverter());
        cmd.registerConverter(GameParser.DivergenceDetection.class, new DivergenceDetectionConverter());
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

}
