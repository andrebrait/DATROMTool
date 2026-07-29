package io.github.datromtool.cli.profile;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.cli.option.FilteringOptions;
import io.github.datromtool.cli.option.InputOptions;
import io.github.datromtool.cli.option.OutputOptions;
import io.github.datromtool.cli.option.PerformanceOptions;
import io.github.datromtool.cli.option.PostFilteringOptions;
import io.github.datromtool.cli.option.SortingOptions;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.config.Profile;
import io.github.datromtool.data.FileOutputOptions;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.data.TextOutputOptions;
import io.github.datromtool.domain.datafile.logiqx.Datafile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static java.lang.String.format;

/**
 * Applies issue #15's precedence rule (built-in defaults &lt; {@code ~/.DATROMTool/config.yaml}
 * &lt; {@code --profile} file(s) &lt; explicit CLI flags) field by field, so an untouched field of
 * a section a flag partially touches still comes from the profile rather than falling back all
 * the way to the stage default.
 *
 * <p>This is the "Option B" fallback from the issue's design: an {@link picocli.CommandLine.IDefaultValueProvider}
 * was probed first and rejected — see {@code OneGameOneRomCommand}'s class Javadoc for the
 * evidence — so precedence is resolved here, after a single ordinary parse, by consulting
 * {@link CommandLine.ParseResult#hasMatchedOption(String)} for every option name that feeds a
 * given {@link Filter}/{@link PostFilter}/{@link SortingPreference}/output/performance field:
 * matched -&gt; the CLI-converted value wins for that field; unmatched -&gt; the profile's value
 * (which is already the stage default when the profile omitted that section) survives.
 *
 * <p>Deliberately stays in {@code cli}: it is the only place that knows about picocli's
 * {@link CommandLine.ParseResult} and the {@code cli.option} classes; {@code core} stays
 * picocli-free.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProfileBinder {

    public static Filter effectiveFilter(
            CommandLine.ParseResult pr,
            @Nullable FilteringOptions cli,
            Filter profileFilter) {
        Filter.FilterBuilder builder = profileFilter.toBuilder();
        if (cli == null) {
            return validated(pr, builder.build());
        }
        Filter cliFilter = cli.toFilter();
        if (pr.hasMatchedOption("--include-regions")) {
            builder.includeRegions(cliFilter.getIncludeRegions());
        }
        if (pr.hasMatchedOption("--exclude-regions")) {
            builder.excludeRegions(cliFilter.getExcludeRegions());
        }
        if (pr.hasMatchedOption("--include-languages")) {
            builder.includeLanguages(cliFilter.getIncludeLanguages());
        }
        if (pr.hasMatchedOption("--exclude-languages")) {
            builder.excludeLanguages(cliFilter.getExcludeLanguages());
        }
        if (pr.hasMatchedOption("--exclude-categories")) {
            builder.excludeCategories(cliFilter.getExcludeCategories());
        }
        if (pr.hasMatchedOption("--exclude")
                || pr.hasMatchedOption("--exclude-regex")
                || pr.hasMatchedOption("--excludes-file")) {
            builder.excludes(cliFilter.getExcludes());
        }
        if (pr.hasMatchedOption("--include")
                || pr.hasMatchedOption("--include-regex")
                || pr.hasMatchedOption("--includes-file")) {
            builder.includes(cliFilter.getIncludes());
        }
        return validated(pr, builder.build());
    }

    private static Filter validated(CommandLine.ParseResult pr, Filter filter) {
        rejectBlankEntries(pr, "includeRegions", filter.getIncludeRegions());
        rejectBlankEntries(pr, "excludeRegions", filter.getExcludeRegions());
        rejectBlankEntries(pr, "includeLanguages", filter.getIncludeLanguages());
        rejectBlankEntries(pr, "excludeLanguages", filter.getExcludeLanguages());
        return filter;
    }


    public static PostFilter effectivePostFilter(
            CommandLine.ParseResult pr,
            @Nullable PostFilteringOptions cli,
            PostFilter profilePostFilter) {
        PostFilter.PostFilterBuilder builder = profilePostFilter.toBuilder();
        if (cli == null) {
            return builder.build();
        }
        if (pr.hasMatchedOption("--post-exclude")
                || pr.hasMatchedOption("--post-exclude-regex")
                || pr.hasMatchedOption("--post-excludes-file")) {
            builder.excludes(cli.toPostFilter().getExcludes());
        }
        return builder.build();
    }

    public static SortingPreference effectiveSortingPreference(
            CommandLine.ParseResult pr,
            @Nullable SortingOptions cli,
            SortingPreference profileSort) {
        SortingPreference.SortingPreferenceBuilder builder = profileSort.toBuilder();
        if (cli == null) {
            return validated(pr, builder.build());
        }
        SortingPreference cliSort = cli.toSortingPreference();
        if (pr.hasMatchedOption("--sort-regions")) {
            builder.regions(cliSort.getRegions());
        }
        if (pr.hasMatchedOption("--sort-languages")) {
            builder.languages(cliSort.getLanguages());
        }
        if (pr.hasMatchedOption("--prefer")
                || pr.hasMatchedOption("--prefer-regex")
                || pr.hasMatchedOption("--prefers-file")) {
            builder.prefers(cliSort.getPrefers());
        }
        if (pr.hasMatchedOption("--avoid")
                || pr.hasMatchedOption("--avoid-regex")
                || pr.hasMatchedOption("--avoids-file")) {
            builder.avoids(cliSort.getAvoids());
        }
        if (pr.hasMatchedOption("--prioritize-languages")) {
            builder.prioritizeLanguages(cliSort.isPrioritizeLanguages());
        }
        if (pr.hasMatchedOption("--versions")) {
            builder.versions(cliSort.getVersions());
        }
        if (pr.hasMatchedOption("--revisions")) {
            builder.revisions(cliSort.getRevisions());
        }
        if (pr.hasMatchedOption("--prereleases")) {
            builder.prereleases(cliSort.getPrereleases());
        }
        if (pr.hasMatchedOption("--prefer-prereleases")) {
            builder.preferPrereleases(cliSort.isPreferPrereleases());
        }
        if (pr.hasMatchedOption("--prefer-parents")) {
            builder.preferParents(cliSort.isPreferParents());
        }
        return validated(pr, builder.build());
    }

    private static SortingPreference validated(
            CommandLine.ParseResult pr,
            SortingPreference sortingPreference) {
        rejectBlankEntries(pr, "regions", sortingPreference.getRegions());
        rejectBlankEntries(pr, "languages", sortingPreference.getLanguages());
        return sortingPreference;
    }

    /**
     * A profile file binds straight off its YAML/JSON tree, never through the CLI's trimming
     * converters, so it is the other door onto issue #26's defect: a blank region or language
     * entry reads as "no restriction" but is a real restriction that no game can satisfy.
     */
    private static void rejectBlankEntries(
            CommandLine.ParseResult pr,
            String field,
            Collection<String> values) {
        if (values.stream().anyMatch(String::isBlank)) {
            throw new CommandLine.ParameterException(
                    pr.commandSpec().commandLine(),
                    format(
                            "profile field '%s' contains a blank entry: remove it to apply no "
                                    + "restriction",
                            field));
        }
    }


    public static List<Path> effectiveInputDirs(
            CommandLine.ParseResult pr,
            @Nullable InputOptions cli,
            Profile.InputSection profileInput) {
        if (cli != null && pr.hasMatchedOption(InputOptions.IN_DIR_OPTION)) {
            return cli.getInputDirs();
        }
        return profileInput.getDirs();
    }

    /**
     * Input DAT files for the {@code --dump-profile} snapshot only: real execution always reads
     * {@code DAT_FILE} positional arguments (never sourced from a profile), so this is purely
     * for round-tripping a profile that was built (in full or in part) from a prior
     * {@code --dump-profile} run.
     */
    public static List<Path> effectiveDats(
            List<DatafileArgument> cliDatafiles,
            Profile.InputSection profileInput) {
        if (!cliDatafiles.isEmpty()) {
            return cliDatafiles.stream()
                    .map(DatafileArgument::getPath)
                    .collect(ImmutableList.toImmutableList());
        }
        return profileInput.getDats();
    }

    /**
     * Real DAT input execution wiring (issue #19 step 3 - the PR #31 deferred obligation):
     * positional {@code DAT_FILE} arguments win when present (their {@link Datafile}s are
     * already parsed, held by their {@link DatafileArgument}); otherwise the effective profile's
     * {@code input.dats} paths are loaded fresh here - they were never converted to a {@link
     * DatafileArgument} at parse time, since a profile file is itself just a path parsed well
     * after positional {@code DAT_FILE} parameters are resolved. An {@link IOException} loading
     * one of those paths propagates to the caller, which wraps it into a {@link
     * CommandLine.ParameterException} exactly like every other profile-loading failure in
     * {@code OneGameOneRomCommand}.
     */
    public static List<Datafile> effectiveDatafiles(
            List<DatafileArgument> cliDatafiles,
            Profile.InputSection profileInput) throws IOException {
        if (!cliDatafiles.isEmpty()) {
            return cliDatafiles.stream()
                    .map(DatafileArgument::getDatafile)
                    .collect(ImmutableList.toImmutableList());
        }
        ImmutableList.Builder<Datafile> builder = ImmutableList.builder();
        for (Path path : profileInput.getDats()) {
            builder.add(SerializationHelper.getInstance().loadXml(path, Datafile.class));
        }
        return builder.build();
    }

    /**
     * Same flag-beats-profile precedence as {@link #effectiveInputDirs}, for {@code --clonelist}
     * (issue #19 step 3): the flag's path wins when {@code --clonelist} was explicitly matched,
     * otherwise the profile's {@code input.clonelists} value (possibly {@code null}, meaning no
     * clone list source) survives.
     */
    @Nullable
    public static Path effectiveClonelist(
            CommandLine.ParseResult pr,
            @Nullable InputOptions cli,
            Profile.InputSection profileInput) {
        if (cli != null && pr.hasMatchedOption(InputOptions.CLONELIST_OPTION)) {
            return cli.getClonelist();
        }
        return profileInput.getClonelists();
    }

    /**
     * Same flag-beats-profile precedence as {@link #effectiveInputDirs}, for
     * {@code --retool-metadata} (issue #19 step 3): the flag's path wins when
     * {@code --retool-metadata} was explicitly matched, otherwise the profile's
     * {@code input.metadata} value (possibly {@code null}, meaning no metadata source) survives.
     */
    @Nullable
    public static Path effectiveRetoolMetadata(
            CommandLine.ParseResult pr,
            @Nullable InputOptions cli,
            Profile.InputSection profileInput) {
        if (cli != null && pr.hasMatchedOption(InputOptions.RETOOL_METADATA_OPTION)) {
            return cli.getRetoolMetadata();
        }
        return profileInput.getMetadata();
    }

    /**
     * {@code output.file} and {@code output.text} are alternatives, never independent fields, so
     * once any flag under one of the two groups is matched the profile's output section is
     * discarded wholesale in favor of the CLI-chosen alternative (itself overlaid field by field
     * on the profile's same-kind section, if any); with no output flags matched at all, the
     * profile's section — file, text, or neither — passes through unchanged.
     */
    public static Profile.OutputSection effectiveOutput(
            CommandLine.ParseResult pr,
            @Nullable OutputOptions cli,
            Profile.OutputSection profileOutput) {
        boolean fileMatched = pr.hasMatchedOption("--out-dir")
                || pr.hasMatchedOption("--alphabetic")
                || pr.hasMatchedOption("--archive")
                || pr.hasMatchedOption("--force-subfolder");
        boolean textMatched = pr.hasMatchedOption("--out-file")
                || pr.hasMatchedOption("--out-mode");
        if (fileMatched && cli != null) {
            FileOutputOptions cliFile = cli.getFileOptions().toFileOutputOptions();
            FileOutputOptions base = profileOutput.getFile();
            Path outputDir = pr.hasMatchedOption("--out-dir") || base == null
                    ? cliFile.outputDir()
                    : base.outputDir();
            boolean alphabetic = pr.hasMatchedOption("--alphabetic") || base == null
                    ? cliFile.alphabetic()
                    : base.alphabetic();
            var archiveType = pr.hasMatchedOption("--archive") || base == null
                    ? cliFile.archiveType()
                    : base.archiveType();
            boolean forceSubfolder = pr.hasMatchedOption("--force-subfolder") || base == null
                    ? cliFile.forceSubfolder()
                    : base.forceSubfolder();
            return Profile.OutputSection.builder()
                    .file(new FileOutputOptions(outputDir, alphabetic, archiveType, forceSubfolder))
                    .build();
        }
        if (textMatched && cli != null) {
            TextOutputOptions cliText = cli.getTextOptions().toTextOutputOptions();
            TextOutputOptions base = profileOutput.getText();
            Path outputFile = pr.hasMatchedOption("--out-file") || base == null
                    ? cliText.outputFile()
                    : base.outputFile();
            var outputMode = pr.hasMatchedOption("--out-mode") || base == null
                    ? cliText.outputMode()
                    : base.outputMode();
            return Profile.OutputSection.builder()
                    .text(new TextOutputOptions(outputFile, outputMode))
                    .build();
        }
        return profileOutput;
    }

    /**
     * Layers the performance section: {@code config.yaml} (or built-in defaults, already loaded
     * into {@code base}) is the floor, a profile's non-default {@code performance} fields sit
     * above it, and explicit {@code --scan-*}/{@code --copy-*} flags (via
     * {@link PerformanceOptions#merge}, unchanged) win over both. "Non-default" is the same
     * proxy the rest of this codebase uses for "the profile set this field" (see
     * {@code @JsonInclude(NON_DEFAULT)} throughout {@code core.config}/{@code core.data}): a
     * profile field bound to exactly its class's hardcoded default is indistinguishable from an
     * omitted one, which only matters if a profile deliberately pins a field to a value that
     * happens to equal the hardcoded default while trying to override a different config.yaml
     * value — a narrow, accepted edge case, not a core change.
     */
    public static AppConfig effectivePerformance(
            AppConfig base,
            AppConfig profilePerformance,
            @Nullable PerformanceOptions cli) {
        AppConfig.FileScannerConfig scannerDefault = AppConfig.FileScannerConfig.builder().build();
        AppConfig.FileScannerConfig profileScanner = profilePerformance.getScanner();
        AppConfig.FileScannerConfig.FileScannerConfigBuilder scannerBuilder = base.getScanner().toBuilder();
        if (!profileScanner.getDefaultBufferSize().equals(scannerDefault.getDefaultBufferSize())) {
            scannerBuilder.defaultBufferSize(profileScanner.getDefaultBufferSize());
        }
        if (!profileScanner.getMaxBufferSize().equals(scannerDefault.getMaxBufferSize())) {
            scannerBuilder.maxBufferSize(profileScanner.getMaxBufferSize());
        }
        if (!profileScanner.getThreads().equals(scannerDefault.getThreads())) {
            scannerBuilder.threads(profileScanner.getThreads());
        }

        AppConfig.FileCopierConfig copierDefault = AppConfig.FileCopierConfig.builder().build();
        AppConfig.FileCopierConfig profileCopier = profilePerformance.getCopier();
        AppConfig.FileCopierConfig.FileCopierConfigBuilder copierBuilder = base.getCopier().toBuilder();
        if (!profileCopier.getBufferSize().equals(copierDefault.getBufferSize())) {
            copierBuilder.bufferSize(profileCopier.getBufferSize());
        }
        if (!profileCopier.getThreads().equals(copierDefault.getThreads())) {
            copierBuilder.threads(profileCopier.getThreads());
        }
        if (profileCopier.isAllowRawZipCopy() != copierDefault.isAllowRawZipCopy()) {
            copierBuilder.allowRawZipCopy(profileCopier.isAllowRawZipCopy());
        }

        AppConfig withProfile = base
                .withScanner(scannerBuilder.build())
                .withCopier(copierBuilder.build());
        if (cli != null) {
            withProfile = withProfile
                    .withScanner(cli.merge(withProfile.getScanner()))
                    .withCopier(cli.merge(withProfile.getCopier()));
        }
        return withProfile;
    }
}
