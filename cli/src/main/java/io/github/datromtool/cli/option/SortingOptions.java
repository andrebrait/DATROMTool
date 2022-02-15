package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.converter.TrimmingLowerCaseConverter;
import io.github.datromtool.cli.converter.TrimmingUpperCaseConverter;
import io.github.datromtool.data.SortingPreference;
import lombok.Data;
import lombok.NoArgsConstructor;
import picocli.CommandLine;

import java.util.List;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static io.github.datromtool.cli.util.ArgumentUtils.merge;

@Data
@NoArgsConstructor
@JsonInclude(NON_DEFAULT)
public final class SortingOptions {

    @CommandLine.Option(
            names = "--sort-regions",
            split = "\\s*,\\s*",
            splitSynopsisLabel = ",",
            converter = TrimmingUpperCaseConverter.class,
            description = "Set the sorting preference based on region codes",
            paramLabel = "REGION")
    private List<String> regions = ImmutableList.of();

    @CommandLine.Option(
            names = "--sort-languages",
            split = "\\s*,\\s*",
            splitSynopsisLabel = ",",
            converter = TrimmingLowerCaseConverter.class,
            description = "Set the sorting preference based on language codes",
            paramLabel = "LANGUAGE")
    private List<String> languages = ImmutableList.of();

    @CommandLine.Option(
            names = "--prefer",
            description = "Prefer entries that match this expression",
            paramLabel = "EXPRESSION")
    private List<String> prefers = ImmutableList.of();

    @CommandLine.Option(
            names = "--prefer-regex",
            description = "Prefer entries that match this regular expression",
            paramLabel = "EXPRESSION")
    private List<Pattern> preferRegexes = ImmutableList.of();

    @CommandLine.Option(
            names = "--prefers-file",
            paramLabel = "PATH",
            description = "Read preference expressions from a file")
    private List<PatternsFileArgument> prefersFiles = ImmutableList.of();

    @CommandLine.Option(
            names = "--avoid",
            description = "Avoid entries that match this expression",
            paramLabel = "EXPRESSION")
    private List<String> avoids = ImmutableList.of();

    @CommandLine.Option(
            names = "--avoid-regex",
            description = "Avoid entries that match this regular expression",
            paramLabel = "EXPRESSION")
    private List<Pattern> avoidRegexes = ImmutableList.of();

    @CommandLine.Option(
            names = "--avoids-file",
            paramLabel = "PATH",
            description = "Read avoidance expressions from a file")
    private List<PatternsFileArgument> avoidsFiles = ImmutableList.of();

    @CommandLine.Option(
            names = "--prioritize-languages",
            description = "Sorting by language will precede sorting by region")
    private boolean prioritizeLanguages;

    @CommandLine.Option(
            names = "--early-versions",
            description = "Prefer release entries with earlier versions")
    boolean earlyVersions = false;

    @CommandLine.Option(
            names = "--early-revisions",
            description = "Prefer release entries with earlier revisions")
    boolean earlyRevisions = false;

    @CommandLine.Option(
            names = "--early-prereleases",
            description = "Prefer entries of earlier prerelease versions")
    boolean earlyPrereleases = false;

    @CommandLine.Option(
            names = "--prefer-prereleases",
            description = "Prefer prerelease entries over release ones")
    boolean preferPrereleases = false;

    @CommandLine.Option(
            names = "--prefer-parents",
            description = "Prefer parents regardless of versioning")
    boolean preferParents = false;

    public SortingPreference toSortingPreference() {
        return SortingPreference.builder()
                .regions(ImmutableSet.copyOf(regions))
                .languages(ImmutableSet.copyOf(languages))
                .prefers(merge(prefers, preferRegexes, prefersFiles))
                .avoids(merge(avoids, preferRegexes, avoidsFiles))
                .prioritizeLanguages(prioritizeLanguages)
                .earlyVersions(earlyVersions)
                .earlyRevisions(earlyRevisions)
                .earlyPrereleases(earlyPrereleases)
                .preferPrereleases(preferPrereleases)
                .preferParents(preferParents)
                .build();
    }
}
