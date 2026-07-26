package io.github.datromtool.cli.command;

import io.github.datromtool.SerializationHelper;
import io.github.datromtool.cli.converter.OutputModeConverter;
import io.github.datromtool.data.OutputMode;
import io.github.datromtool.domain.datafile.logiqx.Datafile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage matrix rows 1-5 for issue #16's {@code dat convert} subcommand: the round trip through
 * every format preserves the {@link Datafile} model, output routes to stdout/file correctly, and
 * every input-side failure surfaces as a clean {@link CommandLine.ParameterException} naming the
 * offending file/option rather than a raw stack trace.
 */
class DatConvertCommandTest {

    private static Path fixture(String name) {
        try {
            return Paths.get(DatConvertCommandTest.class
                    .getClassLoader()
                    .getResource("datafiles/" + name)
                    .toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CommandLine newCommandLine(DatConvertCommand command) {
        CommandLine commandLine = new CommandLine(command);
        commandLine.registerConverter(OutputMode.class, new OutputModeConverter());
        return commandLine;
    }

    private static int run(String... args) {
        DatConvertCommand command = new DatConvertCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(args);
        return command.call();
    }

    // Row 1: XML -> JSON -> YAML -> XML round trip preserves the Datafile model.
    @Test
    void xmlToJsonToYamlToXmlRoundTripPreservesTheDatafileModel(@TempDir Path tempDir) throws IOException {
        SerializationHelper helper = SerializationHelper.getInstance();
        Datafile original = helper.loadXml(fixture("minimal.dat"), Datafile.class);

        Path json = tempDir.resolve("out.json");
        Path yaml = tempDir.resolve("out.yaml");
        Path xml = tempDir.resolve("out.xml");

        assertEquals(0, run(fixture("minimal.dat").toString(), "--to", "json", "--out", json.toString()),
                "xml -> json conversion must exit 0");
        assertEquals(0, run(json.toString(), "--to", "yaml", "--out", yaml.toString()),
                "json -> yaml conversion must exit 0");
        assertEquals(0, run(yaml.toString(), "--to", "xml", "--out", xml.toString()),
                "yaml -> xml conversion must exit 0");

        Datafile roundTripped = helper.loadXml(xml, Datafile.class);
        assertEquals(original, roundTripped,
                "round trip through JSON and YAML must preserve the Datafile model");
    }

    // Row 2(a): output goes to stdout when --out is absent.
    @Test
    void outputGoesToStdoutWhenOutAbsent() {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(captured));
        int exitCode;
        try {
            exitCode = run(fixture("minimal.dat").toString(), "--to", "json");
        } finally {
            System.setOut(original);
        }
        assertEquals(0, exitCode, "conversion to stdout must exit 0");
        assertTrue(captured.toString().contains("\"header\""),
                "stdout must contain the converted JSON, got:\n" + captured);
    }

    // Row 2(b): output goes to a file when --out is present, and that file parses back.
    @Test
    void outputGoesToFileWhenOutPresent(@TempDir Path tempDir) throws IOException {
        Path out = tempDir.resolve("converted.json");
        assertEquals(0, run(fixture("minimal.dat").toString(), "--to", "json", "--out", out.toString()),
                "conversion to file must exit 0");
        Datafile reloaded = SerializationHelper.getInstance().loadJson(out, Datafile.class);
        assertEquals("Test DAT", reloaded.getHeader().getName(),
                "file content must parse back as the converted Datafile");
    }

    // Row 3(a): --to is required; a missing value fails parsing naming the option.
    @Test
    void missingToOptionFailsNamingTheOption() {
        DatConvertCommand command = new DatConvertCommand();
        CommandLine commandLine = newCommandLine(command);
        CommandLine.ParameterException ex = assertThrows(
                CommandLine.ParameterException.class,
                () -> commandLine.parseArgs(fixture("minimal.dat").toString()),
                "missing required --to must fail parsing");
        assertTrue(ex.getMessage().contains("--to"),
                "error must name the missing option, got: " + ex.getMessage());
    }

    // Row 3(b): a bogus --to value fails parsing listing the valid candidates.
    @Test
    void bogusToValueFailsListingCandidates() {
        DatConvertCommand command = new DatConvertCommand();
        CommandLine commandLine = newCommandLine(command);
        CommandLine.ParameterException ex = assertThrows(
                CommandLine.ParameterException.class,
                () -> commandLine.parseArgs(fixture("minimal.dat").toString(), "--to", "bogus"),
                "an unknown --to value must fail parsing");
        assertTrue(
                ex.getMessage().contains("xml")
                        && ex.getMessage().contains("json")
                        && ex.getMessage().contains("yaml"),
                "error must list the valid format candidates, got: " + ex.getMessage());
    }

    // Row 4: a nonexistent input path fails at parse time naming the path.
    @Test
    void nonexistentInputFailsAtParseTimeNamingThePath() {
        DatConvertCommand command = new DatConvertCommand();
        CommandLine commandLine = newCommandLine(command);
        CommandLine.ParameterException ex = assertThrows(
                CommandLine.ParameterException.class,
                () -> commandLine.parseArgs("/no/such/file.dat", "--to", "xml"),
                "a nonexistent input path must fail parsing");
        assertTrue(ex.getMessage().contains("/no/such/file.dat"),
                "error must name the offending path, got: " + ex.getMessage());
    }

    // Row 5: unparseable input (garbage bytes in a .dat) fails cleanly naming the file.
    @Test
    void unparseableInputFailsNamingTheFile(@TempDir Path tempDir) throws IOException {
        Path garbage = tempDir.resolve("garbage.dat");
        Files.writeString(garbage, "this is not xml at all {{{");
        DatConvertCommand command = new DatConvertCommand();
        CommandLine commandLine = newCommandLine(command);
        commandLine.parseArgs(garbage.toString(), "--to", "xml");
        CommandLine.ParameterException ex = assertThrows(
                CommandLine.ParameterException.class,
                command::call,
                "unparseable input must fail cleanly rather than throw a raw parser exception");
        assertTrue(ex.getMessage().contains(garbage.toString()),
                "error must name the offending file, got: " + ex.getMessage());
    }
}
