package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.cli.converter.LowerCaseConverter;
import io.github.datromtool.cli.converter.UpperCaseConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@Data
@Jacksonized
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
@JsonInclude(NON_DEFAULT)
public final class SortingOptions {

    @CommandLine.Option(
            names = {"--sr", "--sort-regions"},
            split = "\\s*,\\s*",
            splitSynopsisLabel = ",",
            converter = UpperCaseConverter.class,
            description = "Set the sorting preference based on region codes",
            paramLabel = "REGION")
    private List<String> regions = ImmutableList.of();

    @CommandLine.Option(
            names = {"--sl", "--sort-languages"},
            split = "\\s*,\\s*",
            splitSynopsisLabel = ",",
            converter = LowerCaseConverter.class,
            description = "Set the sorting preference based on language codes",
            paramLabel = "LANGUAGE")
    private List<String> languages = ImmutableList.of();

    @CommandLine.Option(
            names = "--prefer",
            description = "Prefer entries that match this expression",
            paramLabel = "EXPRESSION")
    private List<Pattern> prefers = ImmutableList.of();

    @CommandLine.Option(
            names = "--prefers-file",
            paramLabel = "PATH",
            description = "Read preference expressions from a file")
    private List<Path> prefersFiles = ImmutableList.of();

    @CommandLine.Option(
            names = "--avoid",
            description = "Avoid entries that match this expression",
            paramLabel = "EXPRESSION")
    private List<Pattern> avoids = ImmutableList.of();

    @CommandLine.Option(
            names = "--avoids-file",
            paramLabel = "PATH",
            description = "Read avoidance expressions from a file")
    private List<Path> avoidsFiles = ImmutableList.of();

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

}
