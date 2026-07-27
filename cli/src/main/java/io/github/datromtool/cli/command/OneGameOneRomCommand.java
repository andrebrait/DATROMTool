package io.github.datromtool.cli.command;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.GameParser;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.cli.GitVersionProvider;
import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.converter.DivergenceDetectionConverter;
import io.github.datromtool.cli.converter.DumpProfileFormatConverter;
import io.github.datromtool.cli.converter.ExistingFileConverter;
import io.github.datromtool.cli.option.DiagnosticOptions;
import io.github.datromtool.cli.option.FilteringOptions;
import io.github.datromtool.cli.option.InputOptions;
import io.github.datromtool.cli.option.OutputOptions;
import io.github.datromtool.cli.option.PerformanceOptions;
import io.github.datromtool.cli.option.PostFilteringOptions;
import io.github.datromtool.cli.option.SortingOptions;
import io.github.datromtool.cli.profile.ProfileBinder;
import io.github.datromtool.cli.progressbar.CommandLineProgressBar;
import io.github.datromtool.command.OneGameOneRom;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.config.Profile;
import io.github.datromtool.data.FileOutputOptions;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.data.TextOutputOptions;
import io.github.datromtool.domain.datafile.logiqx.Datafile;
import io.github.datromtool.domain.datafile.logiqx.Header;
import io.github.datromtool.domain.retool.CloneList;
import io.github.datromtool.domain.retool.RetoolMetadata;
import io.github.datromtool.exception.ExecutionException;
import io.github.datromtool.exception.InvalidDatafileException;
import io.github.datromtool.io.FileCopier;
import io.github.datromtool.io.FileScanner;
import io.github.datromtool.io.logging.FileCopierLoggingListener;
import io.github.datromtool.io.logging.FileScannerLoggingListener;
import io.github.datromtool.retool.RetoolFileResolver;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.Ansi;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import tools.jackson.core.JacksonException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static java.lang.String.format;
import static lombok.AccessLevel.NONE;

/**
 * Issue #15 step 3 wires the {@link Profile} contract into this command via {@code --profile}
 * (repeatable, layered by {@link SerializationHelper#loadProfiles}) and {@code --dump-profile}.
 * Precedence (built-in defaults &lt; profile file(s) &lt; explicit flags) is resolved by
 * {@link ProfileBinder}, field by field, after a single ordinary parse — <b>not</b> by a picocli
 * {@link CommandLine.IDefaultValueProvider}. That was probed first and rejected: an
 * {@code IDefaultValueProvider} only supplies a single default-value string per option, which
 * {@code picocli} splits into multiple elements only when the option itself declares a
 * {@code split} regex. None of the free-form expression options that feed
 * {@link Filter#getExcludes()}/{@link Filter#getIncludes()}/{@link PostFilter#getExcludes()}/
 * {@code SortingPreference}'s {@code prefers}/{@code avoids} (e.g. {@code --exclude},
 * {@code --exclude-regex}, {@code --prefer-regex}, ...) declare one, and joining arbitrary
 * literal/regex values on a delimiter is inherently unsafe (a regex can itself contain the
 * delimiter). Verified empirically: a provider returning {@code "foo,bar"} for an undeclared-split
 * {@code List<String>} option binds as the single element {@code "foo,bar"}, not two elements.
 * Since most of this command's sections are built from exactly these expression options, a
 * uniform field-overlay mechanism (this one) was chosen over mixing two precedence mechanisms
 * for different option shapes. A useful side effect: resolving precedence after parsing (rather
 * than needing the profile file's content before picocli resolves defaults) avoids the two-pass
 * parse an {@code IDefaultValueProvider} would have required to read {@code --profile} ahead of
 * everything else.
 */
@Slf4j
@Data
@NoArgsConstructor
@JsonInclude(NON_DEFAULT)
@CommandLine.Command(
        name = "1g1r",
        description = "Operate in 1G1R mode",
        sortOptions = false,
        abbreviateSynopsis = true,
        versionProvider = GitVersionProvider.class,
        mixinStandardHelpOptions = true)
public final class OneGameOneRomCommand implements Callable<Integer> {

    @CommandLine.Spec
    @ToString.Exclude
    @JsonIgnore
    @Getter(NONE)
    @Setter(NONE)
    private CommandLine.Model.CommandSpec commandSpec;

    // Package-private (not private), no lombok-generated accessor: exists purely so
    // OneGameOneRomCommandDivergenceTest (same package) can pin that the parsed --divergence
    // value is the one that actually reached OneGameOneRom's constructor, mirroring
    // ScanCommand.PROGRESS_OUTPUT's test-only visibility pattern. Re-reading this field's own
    // getDivergenceDetection() getter would not catch a regression where call() hardcodes a mode
    // at the OneGameOneRom construction call site instead of using the parsed field.
    @ToString.Exclude
    @JsonIgnore
    @Getter(NONE)
    @Setter(NONE)
    OneGameOneRom oneGameOneRom;

    @CommandLine.Parameters(
            description = "DAT file to use when generating the 1G1R set",
            arity = "0..*",
            paramLabel = "DAT_FILE")
    private List<DatafileArgument> datafiles = ImmutableList.of();

    @CommandLine.Option(
            names = "--profile",
            paramLabel = "PATH",
            converter = ExistingFileConverter.class,
            description = "Load run settings from a profile file (JSON or YAML). Repeatable; "
                    + "later files are layered over earlier ones. Explicit flags always win over "
                    + "a profile value.")
    private List<Path> profiles = ImmutableList.of();

    @CommandLine.Option(
            names = "--dump-profile",
            arity = "0..1",
            fallbackValue = "yaml",
            paramLabel = "FORMAT",
            converter = DumpProfileFormatConverter.class,
            completionCandidates = DumpProfileFormatConverter.class,
            description = "Print the effective merged configuration (defaults + profile + "
                    + "flags) as a profile and exit, without running the pipeline. "
                    + "Options: ${COMPLETION-CANDIDATES} (default: yaml).")
    private DumpProfileFormat dumpProfile;

    @CommandLine.Option(
            names = "--divergence",
            paramLabel = "MODE",
            converter = DivergenceDetectionConverter.class,
            completionCandidates = DivergenceDetectionConverter.class,
            description = "How strictly to flag divergences between No-Intro parsed names and "
                    + "DAT-declared region/language metadata (logged as warnings; does not affect "
                    + "filtering/output). Options: ${COMPLETION-CANDIDATES} (default: one_way).")
    private GameParser.DivergenceDetection divergenceDetection = GameParser.DivergenceDetection.ONE_WAY;

    @CommandLine.ArgGroup(heading = "Input options\n", exclusive = false)
    private InputOptions inputOptions;

    @CommandLine.ArgGroup
    private OutputOptions outputOptions;

    @CommandLine.ArgGroup(heading = "Filtering options\n", exclusive = false)
    private FilteringOptions filteringOptions;

    @CommandLine.ArgGroup(heading = "Post-filtering options\n", exclusive = false)
    private PostFilteringOptions postFilteringOptions;

    @CommandLine.ArgGroup(heading = "Sorting options\n", exclusive = false)
    private SortingOptions sortingOptions;

    @CommandLine.ArgGroup(heading = "Performance options\n", exclusive = false)
    private PerformanceOptions performanceOptions;

    @CommandLine.ArgGroup(heading = "Diagnostic options\n", exclusive = false)
    private DiagnosticOptions diagnosticOptions;

    @Override
    public Integer call() {
        log.info("Starting {}", this);
        if (diagnosticOptions != null && diagnosticOptions.isDebug()) {
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.DEBUG);
        }

        CommandLine.ParseResult parseResult = commandSpec.commandLine().getParseResult();
        Profile profile;
        try {
            profile = SerializationHelper.getInstance().loadProfiles(profiles);
        } catch (IOException e) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(), e.getMessage());
        }

        Filter filter = ProfileBinder.effectiveFilter(parseResult, filteringOptions, profile.getFilter());
        PostFilter postFilter = ProfileBinder.effectivePostFilter(parseResult, postFilteringOptions, profile.getPostFilter());
        SortingPreference sortingPreference = ProfileBinder.effectiveSortingPreference(parseResult, sortingOptions, profile.getSort());
        List<Path> effectiveInputDirs = ProfileBinder.effectiveInputDirs(parseResult, inputOptions, profile.getInput());
        Profile.OutputSection effectiveOutput = ProfileBinder.effectiveOutput(parseResult, outputOptions, profile.getOutput());
        Path effectiveClonelistPath = ProfileBinder.effectiveClonelist(parseResult, inputOptions, profile.getInput());
        Path effectiveMetadataPath = ProfileBinder.effectiveRetoolMetadata(parseResult, inputOptions, profile.getInput());
        AppConfig baseAppConfig = SerializationHelper.getInstance().loadAppConfig();
        AppConfig appConfig = ProfileBinder.effectivePerformance(baseAppConfig, profile.getPerformance(), performanceOptions);

        if (dumpProfile != null) {
            Profile effectiveProfile = Profile.builder()
                    .input(Profile.InputSection.builder()
                            .dats(ImmutableList.copyOf(ProfileBinder.effectiveDats(datafiles, profile.getInput())))
                            .dirs(ImmutableList.copyOf(effectiveInputDirs))
                            .clonelists(effectiveClonelistPath)
                            .metadata(effectiveMetadataPath)
                            .build())
                    .filter(filter)
                    .sort(sortingPreference)
                    .postFilter(postFilter)
                    .output(effectiveOutput)
                    .performance(appConfig)
                    .build();
            try {
                List<String> lines = dumpProfile == DumpProfileFormat.JSON
                        ? SerializationHelper.getInstance().writeAsJson(effectiveProfile)
                        : SerializationHelper.getInstance().writeAsYaml(effectiveProfile);
                lines.forEach(System.out::println);
            } catch (JacksonException e) {
                throw new CommandLine.ParameterException(
                        commandSpec.commandLine(),
                        format("Could not dump profile: %s", e.getMessage()));
            }
            return 0;
        }

        // Issue #19 step 3 (the PR #31 deferred obligation): positional DAT_FILE arguments win
        // when present; otherwise fall back to the effective profile's input.dats. The
        // requiredness check below only fires when *both* are absent.
        List<Path> effectiveDatPaths = ProfileBinder.effectiveDats(datafiles, profile.getInput());
        if (effectiveDatPaths.isEmpty()) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    "Missing required parameter: 'DAT_FILE' (or set input.dats in a --profile file)");
        }
        if (effectiveOutput.getFile() != null
                && effectiveOutput.getFile().outputDir() != null
                && effectiveInputDirs.isEmpty()) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    format(
                            "%s requires %s",
                            OutputOptions.FileOptions.OUT_DIR_OPTION,
                            InputOptions.IN_DIR_OPTION));
        }
        List<Datafile> realDataFiles;
        try {
            realDataFiles = ProfileBinder.effectiveDatafiles(datafiles, profile.getInput());
        } catch (IOException e) {
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    format("Could not load DAT file from profile input.dats: %s", e.getMessage()));
        }
        CloneList cloneList = null;
        RetoolMetadata retoolMetadata = null;
        if (effectiveClonelistPath != null || effectiveMetadataPath != null) {
            String headerName = realDataFiles.stream()
                    .findFirst()
                    .map(Datafile::getHeader)
                    .map(Header::getName)
                    .orElse(null);
            if (headerName == null) {
                throw new CommandLine.ParameterException(
                        commandSpec.commandLine(),
                        "Cannot resolve --clonelist/--retool-metadata: the DAT file has no header name");
            }
            if (effectiveClonelistPath != null) {
                try {
                    cloneList = RetoolFileResolver.loadCloneList(
                            effectiveClonelistPath,
                            headerName,
                            SerializationHelper.getInstance().getVersionString());
                } catch (IOException e) {
                    throw new CommandLine.ParameterException(
                            commandSpec.commandLine(),
                            format("Could not load clone list: %s", e.getMessage()));
                }
            }
            if (effectiveMetadataPath != null) {
                try {
                    retoolMetadata = RetoolFileResolver.loadRetoolMetadata(effectiveMetadataPath, headerName);
                } catch (IOException e) {
                    throw new CommandLine.ParameterException(
                            commandSpec.commandLine(),
                            format("Could not load Retool metadata: %s", e.getMessage()));
                }
            }
        }
        oneGameOneRom = new OneGameOneRom(
                filter, postFilter, sortingPreference, divergenceDetection, cloneList, retoolMetadata);
        boolean hasErrors = false;
        try (Terminal terminal = createTerminal()) {
            FileScannerLoggingListener scannerLoggingListener = new FileScannerLoggingListener();
            List<FileScanner.Listener> scannerListeners = ImmutableList.of(
                    scannerLoggingListener,
                    new CommandLineProgressBar(terminal, "Scanning", "Scanning input directories..."));
            FileOutputOptions fileOutputOptions = effectiveOutput.getFile();
            if (fileOutputOptions != null) {
                FileCopierLoggingListener copierLoggingListener = new FileCopierLoggingListener();
                List<FileCopier.Listener> copierListeners = ImmutableList.of(
                        copierLoggingListener,
                        new CommandLineProgressBar(terminal, "Copying", "Copying selected files..."));
                oneGameOneRom.generate(
                        appConfig,
                        realDataFiles,
                        effectiveInputDirs,
                        fileOutputOptions,
                        scannerListeners,
                        copierListeners);
                hasErrors = scannerLoggingListener.isErrors() || copierLoggingListener.isErrors();
            } else {
                TextOutputOptions textOutputOptions = effectiveOutput.getText();
                oneGameOneRom.generate(
                        appConfig,
                        realDataFiles,
                        effectiveInputDirs,
                        textOutputOptions,
                        scannerListeners,
                        list -> list.forEach(System.out::println));
                hasErrors = scannerLoggingListener.isErrors();
            }
        } catch (InvalidDatafileException e) {
            hasErrors = true;
            log.error("Got invalid DAT exception", e);
            throw new CommandLine.ParameterException(
                    commandSpec.commandLine(),
                    format("Invalid DAT file: %s", e.getMessage()));
        } catch (ExecutionException e) {
            hasErrors = true;
            System.err.print(Ansi.ansi().eraseScreen());
            log.error("Got execution exception", e);
            return 1;
        } catch (IOException e) {
            hasErrors = true;
            log.error("Error while closing the terminal", e);
        } finally {
            Path logFilePath = Paths.get("").toAbsolutePath().normalize().resolve("datromtool.log");
            System.err.println();
            System.err.println("Finished");
            if (hasErrors) {
                System.err.println("!!! Errors during execution detected !!!");
            }
            System.err.printf("Check the generated log file for details: '%s'%n", logFilePath);
        }
        return 0;
    }

    @Nullable
    private Terminal createTerminal() {
        Terminal terminal;
        try {
            terminal = TerminalBuilder.terminal();
        } catch (IOException e) {
            log.error("Error while creating terminal", e);
            terminal = null;
        }
        return terminal;
    }

}
