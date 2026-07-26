package io.github.datromtool.cli.command;

import io.github.datromtool.ByteSize;
import io.github.datromtool.cli.DatRomCommand;
import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.converter.ArchiveTypeConverter;
import io.github.datromtool.cli.converter.ByteSizeConverter;
import io.github.datromtool.cli.converter.CopyBufferSizeConverter;
import io.github.datromtool.cli.converter.CopyThreadsConverter;
import io.github.datromtool.cli.converter.DatafileConverter;
import io.github.datromtool.cli.converter.GameCategoryConverter;
import io.github.datromtool.cli.converter.OrderPreferenceConverter;
import io.github.datromtool.cli.converter.OutputModeConverter;
import io.github.datromtool.cli.converter.PatternsFileConverter;
import io.github.datromtool.cli.converter.ScanBufferSizeConverter;
import io.github.datromtool.cli.converter.ScanMaxBufferSizeConverter;
import io.github.datromtool.cli.converter.ScanThreadsConverter;
import io.github.datromtool.config.CopyBufferSize;
import io.github.datromtool.config.CopyThreads;
import io.github.datromtool.config.ScanBufferSize;
import io.github.datromtool.config.ScanMaxBufferSize;
import io.github.datromtool.config.ScanThreads;
import io.github.datromtool.data.GameCategory;
import io.github.datromtool.data.OrderPreference;
import io.github.datromtool.data.OutputMode;
import io.github.datromtool.io.ArchiveType;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Red-first surface-change proof for issue #17: {@code scan} is a brand-new subcommand
 * ({@link ScanCommand}) registered on the real {@link DatRomCommand} tree, wired with the exact
 * converters {@link DatRomCommand#main} registers.
 *
 * <p>Executed RED before the migration ({@code scan} was an unknown subcommand, rejected with
 * {@link CommandLine.UnmatchedArgumentException}); frozen byte-identical and re-run GREEN,
 * unchanged, after the migration.
 */
class ScanMigrationTest {

    private static CommandLine newDatRomCommandLine() {
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
        return cmd;
    }

    @Test
    void scanIsAcceptedByTheDatromCommandTree() {
        assertDoesNotThrow(
                () -> newDatRomCommandLine().parseArgs("scan", "."),
                "'scan <dir>' must be a known subcommand on the datrom command tree");
    }
}
