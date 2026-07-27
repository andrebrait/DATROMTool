package io.github.datromtool.cli;

import io.github.datromtool.cli.command.RetoolUpdateCommand;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Mandatory red-first proof (issue #44 step 2): {@code retool update} must be accepted by the
 * top-level {@code datrom} command tree. Executed RED before {@code RetoolCommand}/{@code
 * RetoolUpdateCommand} were wired into {@link DatRomCommand#subcommands} - picocli rejected
 * {@code "retool"} as an unmatched positional/subcommand with a {@code
 * CommandLine.UnmatchedArgumentException} (a {@link CommandLine.ParameterException} subtype) at
 * parse time, exactly like {@code OneGameOneRomCommandRetoolOptionsTest} pinned for {@code
 * --clonelist}/{@code --retool-metadata} in issue #19 step 3. Frozen here byte-identical; green
 * once the subcommand is wired.
 */
class DatRomCommandRetoolTest {

    @Test
    void retoolUpdateSubcommandIsAcceptedByTheCommandTree() {
        CommandLine commandLine = new CommandLine(new DatRomCommand());
        CommandLine.ParseResult parseResult = assertDoesNotThrow(
                () -> commandLine.parseArgs("retool", "update"),
                "'retool update' must be a recognized subcommand of the top-level command tree");
        CommandLine.ParseResult retoolResult = parseResult.subcommand();
        CommandLine.ParseResult updateResult = retoolResult.subcommand();
        assertInstanceOf(
                RetoolUpdateCommand.class,
                updateResult.commandSpec().userObject(),
                "the parsed subcommand chain must resolve to RetoolUpdateCommand");
    }
}
