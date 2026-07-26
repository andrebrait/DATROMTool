package io.github.datromtool.cli.command;

import io.github.datromtool.SerializationHelper;
import io.github.datromtool.cli.GitVersionProvider;
import io.github.datromtool.cli.converter.ExistingFileConverter;
import io.github.datromtool.cli.converter.OutputModeConverter;
import io.github.datromtool.data.OutputMode;
import io.github.datromtool.domain.datafile.logiqx.Datafile;
import picocli.CommandLine;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * Issue #16: CLI wiring for {@link SerializationHelper}'s existing Logiqx XML / DATROMTool JSON /
 * DATROMTool YAML {@link Datafile} round-trip. No transformation logic lives here: the input file
 * is dispatched to {@link SerializationHelper#loadXml} or {@link SerializationHelper#loadJsonOrYaml}
 * by extension (real Logiqx DATs are XML despite the {@code .dat} extension), and the parsed
 * {@link Datafile} is serialized with whichever {@code write*} method matches {@code --to}.
 */
@CommandLine.Command(
        name = "convert",
        description = "Convert a DAT file between Logiqx XML, DATROMTool JSON, and DATROMTool YAML",
        sortOptions = false,
        abbreviateSynopsis = true,
        versionProvider = GitVersionProvider.class,
        mixinStandardHelpOptions = true)
public final class DatConvertCommand implements Callable<Integer> {

    private static final Pattern XML_LIKE_PATTERN = Pattern.compile("^.+\\.(?:dat|xml)$", CASE_INSENSITIVE);

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec commandSpec;

    @CommandLine.Parameters(
            paramLabel = "FILE",
            converter = ExistingFileConverter.class,
            description = "DAT file to convert (Logiqx XML, DATROMTool JSON, or DATROMTool YAML)")
    private Path input;

    @CommandLine.Option(
            names = "--to",
            required = true,
            paramLabel = "FORMAT",
            completionCandidates = OutputModeConverter.class,
            description = "Output format. Options: ${COMPLETION-CANDIDATES}")
    private OutputMode to;

    @CommandLine.Option(
            names = "--out",
            paramLabel = "PATH",
            description = "Write output to this file (default: print to stdout)")
    private Path out;

    @Override
    public Integer call() {
        SerializationHelper helper = SerializationHelper.getInstance();
        Datafile datafile;
        try {
            datafile = XML_LIKE_PATTERN.matcher(input.getFileName().toString()).matches()
                    ? helper.loadXml(input, Datafile.class)
                    : helper.loadJsonOrYaml(input, Datafile.class);
        } catch (IOException | JacksonException e) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    format("Could not read DAT file '%s': %s", input, e.getMessage()));
        }

        List<String> lines;
        try {
            lines = switch (to) {
                case XML -> helper.writeAsXml(datafile);
                case JSON -> helper.writeAsJson(datafile);
                case YAML -> helper.writeAsYaml(datafile);
            };
        } catch (JacksonException e) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    format("Could not write DAT file as %s: %s", to, e.getMessage()));
        }

        if (out != null) {
            try {
                Files.write(out, lines);
            } catch (IOException e) {
                throw new CommandLine.ParameterException(
                        commandSpec.commandLine(),
                        format("Could not write output file '%s': %s", out, e.getMessage()));
            }
        } else {
            lines.forEach(System.out::println);
        }
        return 0;
    }
}
