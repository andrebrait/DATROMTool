package io.github.datromtool.cli.profile;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.cli.converter.ArchiveTypeConverter;
import io.github.datromtool.cli.converter.CopyThreadsConverter;
import io.github.datromtool.cli.converter.OutputModeConverter;
import io.github.datromtool.cli.converter.ScanThreadsConverter;
import io.github.datromtool.cli.option.FilteringOptions;
import io.github.datromtool.cli.option.InputOptions;
import io.github.datromtool.cli.option.OutputOptions;
import io.github.datromtool.cli.option.PerformanceOptions;
import io.github.datromtool.cli.option.SortingOptions;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.config.CopyThreads;
import io.github.datromtool.config.Profile;
import io.github.datromtool.config.ScanThreads;
import io.github.datromtool.data.FileOutputOptions;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.OrderPreference;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.data.TextOutputOptions;
import io.github.datromtool.io.ArchiveType;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Coverage matrix for issue #15 step 3's {@link ProfileBinder} (the "Option B" field-overlay
 * precedence mechanism; see {@code OneGameOneRomCommand}'s class Javadoc for why the
 * {@code IDefaultValueProvider} alternative was rejected).
 */
class ProfileBinderTest {

    @CommandLine.Command
    private static final class Holder {

        // Mirrors OneGameOneRomCommand: uninitialized ArgGroup fields, so an unmatched group
        // stays null exactly like the real command.
        @CommandLine.ArgGroup(exclusive = false)
        FilteringOptions filteringOptions;

        @CommandLine.ArgGroup(exclusive = false)
        SortingOptions sortingOptions;

        @CommandLine.ArgGroup(exclusive = false)
        InputOptions inputOptions;

        @CommandLine.ArgGroup(exclusive = false)
        PerformanceOptions performanceOptions;
    }

    private static CommandLine parsed(Holder holder, String... args) {
        CommandLine commandLine = new CommandLine(holder);
        commandLine.registerConverter(ScanThreads.class, new ScanThreadsConverter());
        commandLine.registerConverter(CopyThreads.class, new CopyThreadsConverter());
        commandLine.parseArgs(args);
        return commandLine;
    }

    // Row 1: a profile-only run (no CLI flags at all) reproduces the same Filter as the
    // equivalent flags would, field for field.
    @Test
    void profileOnlyFilterMatchesEquivalentFlags() {
        Holder empty = new Holder();
        CommandLine.ParseResult pr = parsed(empty).getParseResult();
        Filter profileFilter = Filter.builder()
                .includeRegions(ImmutableSet.of("USA"))
                .build();
        Filter effective = ProfileBinder.effectiveFilter(pr, empty.filteringOptions, profileFilter);

        Holder viaFlags = new Holder();
        parsed(viaFlags, "--include-regions", "USA");
        Filter fromFlags = viaFlags.filteringOptions.toFilter();

        assertEquals(fromFlags, effective, "profile-only includeRegions must equal the equivalent-flag Filter");
    }

    @Test
    void profileOnlySortMatchesEquivalentFlags() {
        Holder empty = new Holder();
        CommandLine.ParseResult pr = parsed(empty).getParseResult();
        SortingPreference profileSort = SortingPreference.builder()
                .versions(OrderPreference.EARLIEST)
                .build();
        SortingPreference effective = ProfileBinder.effectiveSortingPreference(pr, empty.sortingOptions, profileSort);

        Holder viaFlags = new Holder();
        parsed(viaFlags, "--versions", "earliest");
        SortingPreference fromFlags = viaFlags.sortingOptions.toSortingPreference();

        assertEquals(fromFlags, effective, "profile-only versions=earliest must equal the equivalent-flag SortingPreference");
    }

    // Row 2: flag beats file for the touched field; profile value survives for an untouched
    // field of the same section.
    @Test
    void explicitVersionsFlagBeatsProfileButRegionsSurviveFromProfile() {
        Holder holder = new Holder();
        CommandLine commandLine = parsed(holder, "--versions", "latest");

        SortingPreference profileSort = SortingPreference.builder()
                .versions(OrderPreference.EARLIEST)
                .regions(ImmutableSet.of("USA"))
                .build();
        SortingPreference effective = ProfileBinder.effectiveSortingPreference(
                commandLine.getParseResult(), holder.sortingOptions, profileSort);

        assertEquals(OrderPreference.LATEST, effective.getVersions(), "--versions latest must win over the profile's EARLIEST");
        assertEquals(ImmutableSet.of("USA"), effective.getRegions(), "profile's untouched regions must survive");
    }

    // Row 7: profile performance.scanner.threads participates below explicit --scan-threads
    // (flag wins) and above the config.yaml-loaded base; base's untouched fields (bufferSize)
    // are unaffected.
    @Test
    void profilePerformanceLayersAboveBaseAndBelowExplicitFlag() {
        // Neither threads value may equal the class's runtime-computed default (half the CPU
        // count): that value is indistinguishable from "unset" (see ProfileBinder's Javadoc), so
        // picking anything other than the machine-dependent default keeps this test deterministic.
        ScanThreads runtimeDefault = AppConfig.FileScannerConfig.builder().build().getThreads();
        ScanThreads baseThreads = new ScanThreads(runtimeDefault.value() + 100);
        ScanThreads profileThreads = new ScanThreads(runtimeDefault.value() + 200);
        AppConfig base = AppConfig.builder()
                .scanner(AppConfig.FileScannerConfig.builder()
                        .threads(baseThreads)
                        .build())
                .build();
        AppConfig profilePerformance = AppConfig.builder()
                .scanner(AppConfig.FileScannerConfig.builder()
                        .threads(profileThreads)
                        .build())
                .build();

        // No CLI flags: profile's non-default threads win over base's config.yaml value.
        AppConfig noCli = ProfileBinder.effectivePerformance(base, profilePerformance, null);
        assertEquals(profileThreads, noCli.getScanner().getThreads(), "profile threads must win over config.yaml base when no flag given");
        assertEquals(
                base.getScanner().getDefaultBufferSize(),
                noCli.getScanner().getDefaultBufferSize(),
                "untouched bufferSize must keep the config.yaml base value");

        // Explicit --scan-threads wins over both the profile and the base.
        Holder holder = new Holder();
        parsed(holder, "--scan-threads", "7");
        AppConfig withCli = ProfileBinder.effectivePerformance(base, profilePerformance, holder.performanceOptions);
        assertEquals(new ScanThreads(7), withCli.getScanner().getThreads(), "--scan-threads must win over both profile and config.yaml base");
    }

    @Test
    void defaultProfilePerformanceLeavesBaseUntouched() {
        AppConfig base = AppConfig.builder()
                .scanner(AppConfig.FileScannerConfig.builder()
                        .threads(new ScanThreads(3))
                        .build())
                .build();
        AppConfig defaultProfilePerformance = AppConfig.builder().build();

        AppConfig effective = ProfileBinder.effectivePerformance(base, defaultProfilePerformance, null);
        assertEquals(
                base.getScanner().getThreads(),
                effective.getScanner().getThreads(),
                "a default (unset) profile performance section must not disturb the config.yaml base");
    }

    @Test
    void effectiveInputDirsFallsBackToProfileWhenNoInDirFlag() {
        Holder holder = new Holder();
        CommandLine commandLine = parsed(holder);
        List<Path> dirs = ProfileBinder.effectiveInputDirs(
                commandLine.getParseResult(),
                holder.inputOptions,
                Profile.InputSection.builder()
                        .dirs(ImmutableList.of(Paths.get("roms")))
                        .build());
        assertEquals(List.of(Paths.get("roms")), dirs, "--in-dir absent must fall back to the profile's dirs");
    }

    @Test
    void explicitInDirFlagBeatsProfileDirs() {
        Holder holder = new Holder();
        CommandLine commandLine = parsed(holder, "--in-dir", ".");
        List<Path> dirs = ProfileBinder.effectiveInputDirs(
                commandLine.getParseResult(),
                holder.inputOptions,
                Profile.InputSection.builder()
                        .dirs(ImmutableList.of(Paths.get("roms")))
                        .build());
        assertEquals(List.of(Paths.get(".")), dirs, "--in-dir must win over the profile's dirs");
        assertFalse(dirs.contains(Paths.get("roms")), "profile dirs must not leak in when --in-dir is explicit");
    }

    @CommandLine.Command
    private static final class OutputHolder {

        @CommandLine.ArgGroup
        private OutputOptions outputOptions;
    }

    private static CommandLine parsedOutput(OutputHolder holder, String... args) {
        CommandLine commandLine = new CommandLine(holder);
        commandLine.registerConverter(ArchiveType.class, new ArchiveTypeConverter());
        commandLine.registerConverter(io.github.datromtool.data.OutputMode.class, new OutputModeConverter());
        commandLine.parseArgs(args);
        return commandLine;
    }

    // Output section: file/text are alternatives, so a CLI flag under one group replaces the
    // profile's section wholesale, but still overlays field by field within the matched kind.
    @Test
    void effectiveOutputOverlaysMatchedFileFieldsOnlyKeepingProfileFieldsUntouched() {
        OutputHolder holder = new OutputHolder();
        CommandLine commandLine = parsedOutput(holder, "--out-dir", "new");
        Profile.OutputSection profileOutput = Profile.OutputSection.builder()
                .file(new FileOutputOptions(Paths.get("old"), true, ArchiveType.ZIP, true))
                .build();

        Profile.OutputSection effective = ProfileBinder.effectiveOutput(
                commandLine.getParseResult(), holder.outputOptions, profileOutput);

        FileOutputOptions file = effective.getFile();
        assertEquals(Paths.get("new"), file.outputDir(), "--out-dir must win over the profile's outputDir");
        assertEquals(true, file.alphabetic(), "untouched alphabetic must survive from the profile");
        assertEquals(ArchiveType.ZIP, file.archiveType(), "untouched archiveType must survive from the profile");
        assertEquals(true, file.forceSubfolder(), "untouched forceSubfolder must survive from the profile");
        assertNull(effective.getText(), "file-kind output must not carry a text section");
    }

    @Test
    void effectiveOutputPassesThroughProfileSectionWhenNoOutputFlagsMatched() {
        OutputHolder holder = new OutputHolder();
        CommandLine commandLine = parsedOutput(holder);
        Profile.OutputSection profileOutput = Profile.OutputSection.builder()
                .text(new TextOutputOptions(Paths.get("out.txt"), null))
                .build();

        Profile.OutputSection effective = ProfileBinder.effectiveOutput(
                commandLine.getParseResult(), holder.outputOptions, profileOutput);

        assertEquals(profileOutput, effective, "with no output flags matched, the profile's section passes through unchanged");
    }
}
