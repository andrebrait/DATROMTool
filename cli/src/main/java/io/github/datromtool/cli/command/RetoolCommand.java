package io.github.datromtool.cli.command;

import io.github.datromtool.cli.GitVersionProvider;
import picocli.CommandLine;

/**
 * Command group for Retool cache operations (issue #44 step 2's {@code update} subcommand).
 * Deliberately not {@code Callable}, mirroring {@link DatCommand} (#16): picocli's default
 * handling of a bare group prints "Missing required subcommand" plus usage on stderr and exits
 * with the usage code (verified for this exact shape in PR #33) - not this class's own logic to
 * reimplement.
 */
@CommandLine.Command(
        name = "retool",
        description = "Manage the local Retool clone list / metadata cache",
        sortOptions = false,
        abbreviateSynopsis = true,
        versionProvider = GitVersionProvider.class,
        mixinStandardHelpOptions = true,
        subcommands = {RetoolUpdateCommand.class})
public final class RetoolCommand {
}
