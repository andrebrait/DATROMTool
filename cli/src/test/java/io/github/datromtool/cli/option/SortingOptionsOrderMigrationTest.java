package io.github.datromtool.cli.option;

import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.converter.PatternsFileConverter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Red-first surface-change proof for issue #14 step 2: the direction-ambiguous
 * {@code --early-versions}/{@code --early-revisions}/{@code --early-prereleases} boolean flags
 * are replaced by {@code --versions}/{@code --revisions}/{@code --prereleases} enum options.
 *
 * <p>Parse-level only: intentionally does not touch {@link SortingPreference} getters, since
 * the new {@code getVersions()}/{@code getRevisions()}/{@code getPrereleases()} accessors do not
 * exist before the migration and would break compilation at red time. Getter-level mapping
 * assertions live in the ordinary {@link SortingOptionsTest} instead.
 *
 * <p>Executed RED before the migration (old flags still parsed fine, new options were unknown);
 * frozen byte-identical and re-run GREEN, unchanged, after the migration.
 */
class SortingOptionsOrderMigrationTest {

    @CommandLine.Command
    private static final class Holder {

        @CommandLine.ArgGroup(exclusive = false)
        SortingOptions sortingOptions = new SortingOptions();
    }

    private static void parse(String... args) {
        Holder holder = new Holder();
        CommandLine commandLine = new CommandLine(holder);
        commandLine.registerConverter(PatternsFileArgument.class, new PatternsFileConverter());
        commandLine.parseArgs(args);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--early-versions", "--early-revisions", "--early-prereleases"})
    void oldEarlyFlagsAreRejected(String flag) {
        assertThrows(
                CommandLine.UnmatchedArgumentException.class,
                () -> parse(flag),
                flag + " must no longer parse; it is replaced by an order enum option");
    }

    @ParameterizedTest
    @ValueSource(strings = {"--versions", "--revisions", "--prereleases"})
    void newOrderOptionsAreAcceptedWithEarliestValue(String option) {
        assertDoesNotThrow(
                () -> parse(option, "earliest"),
                option + " earliest must parse as the new enum option");
    }
}
