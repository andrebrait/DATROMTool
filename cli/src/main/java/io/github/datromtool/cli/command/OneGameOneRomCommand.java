package io.github.datromtool.cli.command;

import io.github.datromtool.cli.GitVersionProvider;
import io.github.datromtool.cli.option.FilteringOptions;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "1g1r",
        description = "Operate in 1G1R mode",
        sortOptions = false,
        versionProvider = GitVersionProvider.class,
        mixinStandardHelpOptions = true)
public final class OneGameOneRomCommand implements Callable<Integer> {

    @CommandLine.ArgGroup(heading = "Filtering options\n", exclusive = false)
    private FilteringOptions filteringOptions;

    @Override
    public Integer call() throws Exception {
        return 0;
    }
}
