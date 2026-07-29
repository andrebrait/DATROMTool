package io.github.datromtool.cli.command;

import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.converter.DatafileConverter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Review round fix: clone list group keys sharing a namespace with raw DAT parent names, and
 * partial clone list matches splitting a DAT parent/clone family (see
 * {@link io.github.datromtool.GameSorter#sortAndGroupByParent} and
 * {@link io.github.datromtool.command.OneGameOneRom}'s clone list group union pass).
 *
 * <p>Both scenarios below are executed end-to-end through {@code 1g1r} exactly like {@link
 * OneGameOneRomCommandRetoolTest}, since the bug only manifests once matching, grouping, and
 * 1G1R selection all run together.
 */
class OneGameOneRomCommandCloneListFamilyTest {

    private static Path fixture(String name) {
        try {
            return Paths.get(OneGameOneRomCommandCloneListFamilyTest.class
                    .getClassLoader()
                    .getResource(name)
                    .toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CommandLine newCommandLine(OneGameOneRomCommand command) {
        CommandLine commandLine = new CommandLine(command);
        commandLine.registerConverter(DatafileArgument.class, new DatafileConverter());
        return commandLine;
    }

    private static String runAndCaptureStdout(OneGameOneRomCommand command, CommandLine commandLine) {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(captured));
        try {
            int exitCode = command.call();
            assertEquals(0, exitCode, "run must exit 0, captured output so far:\n" + captured);
        } finally {
            System.setOut(original);
        }
        return captured.toString();
    }

    // (a): a clone list title matching only the DAT-declared *parent* of a family must not
    // orphan the rest of that family - the whole family stays one 1G1R entry, exactly as it
    // was without any clone list at all (parent "Foo (USA)" / clone "Bar (Europe)"; clone list
    // matches "Foo (USA)" only).
    @Test
    void partialClonelistMatchOnParentDoesNotSplitDatFamily() {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--clonelist", fixture("retool/clonelists/foo-only.json").toString(),
                fixture("datafiles/foo-bar-family.dat").toString());

        String output = runAndCaptureStdout(command, commandLine);

        int matches = 0;
        for (String name : new String[]{"Foo (USA)", "Bar (Europe)"}) {
            if (output.contains(name)) {
                matches++;
            }
        }
        assertEquals(
                1,
                matches,
                "a clone list match on only the family's parent must still unify the whole "
                        + "DAT parent/clone family into exactly one 1G1R entry, got:\n" + output);
    }

    // (c): upstream's ignore flag "force removes the title from Retool's consideration" - an
    // ignored title must be gone from the 1G1R output, not merely left on its DAT grouping
    // (issue #43). "Mystery Quest (USA)" is the only member of its ignored group, so it would
    // otherwise win outright and be listed; the unrelated "Adventure" must stay.
    @Test
    void ignoredGroupRemovesItsTitleFromTheOutput() {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--clonelist", fixture("retool/clonelists/mystery-quest-ignored.json").toString(),
                fixture("datafiles/adventure-collision.dat").toString());

        String output = runAndCaptureStdout(command, commandLine);

        assertTrue(
                output.contains("Adventure"),
                "a game untouched by the clone list must still be listed, got:\n" + output);
        assertFalse(
                output.contains("Mystery Quest"),
                "a title in a group flagged ignore must be removed from consideration "
                        + "entirely, got:\n" + output);
    }

    // (b): a clone list group literally named the same as an unrelated, untagged DAT game must
    // not collide with that DAT game's own (unprefixed) parent-name grouping key - both stay
    // separate 1G1R entries ("Adventure", an untagged DAT game with no clone list match of its
    // own, vs. "Mystery Quest (USA)", matched into a clone list group literally named
    // "Adventure").
    @Test
    void clonelistGroupNameDoesNotCollideWithUnrelatedDatGameName() {
        OneGameOneRomCommand command = new OneGameOneRomCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(
                "--clonelist", fixture("retool/clonelists/adventure-collision.json").toString(),
                fixture("datafiles/adventure-collision.dat").toString());

        String output = runAndCaptureStdout(command, commandLine);

        int matches = 0;
        for (String name : new String[]{"Adventure", "Mystery Quest (USA)"}) {
            if (output.contains(name)) {
                matches++;
            }
        }
        assertEquals(
                2,
                matches,
                "a clone list group name colliding with an unrelated untagged DAT game's own "
                        + "name must not merge them into one 1G1R entry, got:\n" + output);
    }
}
