package io.github.datromtool.cli.option;

import io.github.datromtool.cli.converter.ArchiveTypeConverter;
import io.github.datromtool.cli.converter.OutputModeConverter;
import io.github.datromtool.data.FileOutputOptions;
import io.github.datromtool.data.OutputMode;
import io.github.datromtool.data.TextOutputOptions;
import io.github.datromtool.io.ArchiveType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link OutputOptions}' picocli ArgGroup wiring (file/text exclusivity, out-dir
 * requiredness, nested archive/force-subfolder exclusivity) and its field-mapping methods.
 */
class OutputOptionsTest {

    @CommandLine.Command
    private static final class Holder {

        // Mirrors OneGameOneRomCommand's own declaration exactly: default exclusive group,
        // field left uninitialized so picocli instantiates it lazily.
        @CommandLine.ArgGroup
        private OutputOptions outputOptions;
    }

    private static CommandLine newCommandLine(Holder holder) {
        CommandLine commandLine = new CommandLine(holder);
        commandLine.registerConverter(ArchiveType.class, new ArchiveTypeConverter());
        commandLine.registerConverter(OutputMode.class, new OutputModeConverter());
        return commandLine;
    }

    private static OutputOptions parse(String... args) {
        Holder holder = new Holder();
        newCommandLine(holder).parseArgs(args);
        return Objects.requireNonNull(holder.outputOptions, "outputOptions must be populated");
    }

    // Row 19
    @Test
    void fileAndTextOutputAreMutuallyExclusive() {
        Holder holder = new Holder();
        CommandLine commandLine = newCommandLine(holder);
        assertThrows(
                CommandLine.MutuallyExclusiveArgsException.class,
                () -> commandLine.parseArgs("--out-dir", "out", "--out-file", "out.txt"),
                "--out-dir (file group) and --out-file (text group) must be mutually exclusive");
    }

    // Row 20
    @Test
    void alphabeticWithoutOutDirFailsRequiredValidation() {
        Holder holder = new Holder();
        CommandLine commandLine = newCommandLine(holder);
        assertThrows(
                CommandLine.ParameterException.class,
                () -> commandLine.parseArgs("--alphabetic"),
                "--alphabetic without --out-dir must fail file-group required validation");
    }

    // Row 21
    @Test
    void archiveOptionResolvesArchiveType() {
        OutputOptions options = parse("--out-dir", "out", "--archive", "zip");
        assertEquals(
                ArchiveType.ZIP,
                options.getFileOptions().getGroupingOptions().getArchiveType(),
                "--archive zip must resolve to ArchiveType.ZIP");
    }

    @Test
    void archiveOptionAcceptsLeadingDotAlias() {
        OutputOptions options = parse("--out-dir", "out", "--archive", ".zip");
        assertEquals(
                ArchiveType.ZIP,
                options.getFileOptions().getGroupingOptions().getArchiveType(),
                "--archive .zip must strip the leading dot and resolve to ArchiveType.ZIP");
    }

    @Test
    void bogusArchiveValueFailsListingCandidates() {
        Holder holder = new Holder();
        CommandLine commandLine = newCommandLine(holder);
        CommandLine.ParameterException thrown = assertThrows(
                CommandLine.ParameterException.class,
                () -> commandLine.parseArgs("--out-dir", "out", "--archive", "bogus"),
                "an unknown --archive value must fail parsing");
        assertTrue(
                thrown.getMessage().contains("zip"),
                "the error message must list valid archive candidates, got: " + thrown.getMessage());
    }

    // Row 22
    @Test
    void archiveAndForceSubfolderAreMutuallyExclusive() {
        Holder holder = new Holder();
        CommandLine commandLine = newCommandLine(holder);
        // The nested GroupingOptions group is exclusive, so picocli treats --archive and
        // --force-subfolder as two separate occurrences of the enclosing (0..1) file-options
        // group rather than reporting picocli's own MutuallyExclusiveArgsException; it surfaces
        // as a MaxValuesExceededException instead. Both are ParameterException subtypes and both
        // reject the combination, which is the exclusivity behavior this pins.
        assertThrows(
                CommandLine.MaxValuesExceededException.class,
                () -> commandLine.parseArgs(
                        "--out-dir", "out", "--archive", "zip", "--force-subfolder"),
                "--archive and --force-subfolder (nested GroupingOptions) must reject this combination");
    }

    // Row 23
    @ParameterizedTest
    @ValueSource(strings = {"json", "JSON", "yaml", "YAML", "xml", "XML"})
    void outModeResolvesCaseInsensitively(String value) {
        OutputOptions options = parse("--out-mode", value);
        assertEquals(
                OutputMode.valueOf(value.toUpperCase()),
                options.getTextOptions().getOutputMode(),
                "--out-mode " + value + " must resolve case-insensitively");
    }

    @Test
    void bogusOutModeValueFailsParsing() {
        Holder holder = new Holder();
        CommandLine commandLine = newCommandLine(holder);
        assertThrows(
                CommandLine.ParameterException.class,
                () -> commandLine.parseArgs("--out-mode", "bogus"),
                "an unknown --out-mode value must fail parsing");
    }

    // Row 24
    @Test
    void fileOptionsMapFieldsVerbatimWithoutGrouping() {
        OutputOptions options = parse("--out-dir", "out", "--alphabetic");
        FileOutputOptions mapped = options.getFileOptions().toFileOutputOptions();
        assertEquals(Paths.get("out"), mapped.outputDir(), "outputDir must map verbatim");
        assertTrue(mapped.alphabetic(), "alphabetic must map verbatim");
        assertNull(mapped.archiveType(), "archiveType must be null when no grouping options given");
        assertFalse(mapped.forceSubfolder(), "forceSubfolder must default to false");
    }

    @Test
    void fileOptionsMapFieldsVerbatimWithGrouping() {
        OutputOptions options = parse("--out-dir", "out", "--archive", "zip");
        FileOutputOptions mapped = options.getFileOptions().toFileOutputOptions();
        Path expectedDir = Paths.get("out");
        assertEquals(expectedDir, mapped.outputDir(), "outputDir must map verbatim");
        assertFalse(mapped.alphabetic(), "alphabetic must default to false");
        assertEquals(ArchiveType.ZIP, mapped.archiveType(), "archiveType must map from the grouping option");
        assertFalse(mapped.forceSubfolder(), "forceSubfolder must default to false");
    }

    @Test
    void forceSubfolderMapsVerbatimWhenSet() {
        OutputOptions options = parse("--out-dir", "out", "--force-subfolder");
        FileOutputOptions mapped = options.getFileOptions().toFileOutputOptions();
        assertTrue(mapped.forceSubfolder(), "--force-subfolder must map verbatim to true");
        assertNull(mapped.archiveType(), "archiveType must stay null when only --force-subfolder is given");
    }

    @Test
    void textOptionsMapFieldsVerbatim() {
        OutputOptions options = parse("--out-file", "out.txt", "--out-mode", "json");
        TextOutputOptions mapped = options.getTextOptions().toTextOutputOptions();
        assertEquals(Paths.get("out.txt"), mapped.outputFile(), "outputFile must map verbatim");
        assertEquals(OutputMode.JSON, mapped.outputMode(), "outputMode must map verbatim");
    }
}
