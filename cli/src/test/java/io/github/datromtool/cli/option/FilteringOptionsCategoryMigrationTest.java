package io.github.datromtool.cli.option;

import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.converter.PatternsFileConverter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Red-first proof for issue #14 step 1: the 13 negatable category flags (e.g. {@code --proto})
 * and the {@code --no-all} tri-state are removed from {@link FilteringOptions} in favor of a
 * single {@code --exclude-categories} option. This pins that the OLD surface is gone: passing
 * an old flag must now fail parsing instead of silently working.
 *
 * <p>This file is authored and hashed BEFORE the migration (RED: flags still parse, assertions
 * fail) and stays byte-identical through the migration (GREEN: flags rejected).
 */
class FilteringOptionsCategoryMigrationTest {

    @CommandLine.Command
    private static final class Holder {

        @CommandLine.ArgGroup(exclusive = false)
        FilteringOptions filteringOptions = new FilteringOptions();
    }

    private static void parse(String... args) {
        Holder holder = new Holder();
        CommandLine commandLine = new CommandLine(holder);
        commandLine.registerConverter(PatternsFileArgument.class, new PatternsFileConverter());
        commandLine.parseArgs(args);
    }

    @Test
    void oldProtoFlagIsNoLongerRecognized() {
        assertThrows(
                CommandLine.UnmatchedArgumentException.class,
                () -> parse("--proto"),
                "--proto must no longer parse now that categories are exclude-list driven");
    }

    @Test
    void oldNoAllFlagIsNoLongerRecognized() {
        assertThrows(
                CommandLine.UnmatchedArgumentException.class,
                () -> parse("--no-all"),
                "--no-all must no longer parse now that categories are exclude-list driven");
    }
}
