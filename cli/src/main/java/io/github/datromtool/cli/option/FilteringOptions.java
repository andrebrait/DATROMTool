package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.converter.GameCategoryConverter;
import io.github.datromtool.cli.converter.TrimmingLowerCaseConverter;
import io.github.datromtool.cli.converter.TrimmingUpperCaseConverter;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.GameCategory;
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
public final class FilteringOptions {

    @CommandLine.Option(
            names = "--include-regions",
            split = "\\s*,\\s*",
            splitSynopsisLabel = ",",
            description = "Include only entries with the given region codes",
            converter = TrimmingUpperCaseConverter.class,
            paramLabel = "REGION")
    private List<String> includeRegions = ImmutableList.of();

    @CommandLine.Option(
            names = "--include-languages",
            split = "\\s*,\\s*",
            splitSynopsisLabel = ",",
            converter = TrimmingLowerCaseConverter.class,
            description = "Include only entries with the given language codes",
            paramLabel = "LANGUAGE")
    private List<String> includeLanguages = ImmutableList.of();

    @CommandLine.Option(
            names = "--exclude-regions",
            split = "\\s*,\\s*",
            splitSynopsisLabel = ",",
            converter = TrimmingUpperCaseConverter.class,
            description = "Exclude all entries with the given region codes",
            paramLabel = "REGION")
    private List<String> excludeRegions = ImmutableList.of();

    @CommandLine.Option(
            names = "--exclude-languages",
            split = "\\s*,\\s*",
            splitSynopsisLabel = ",",
            converter = TrimmingLowerCaseConverter.class,
            description = "Exclude all entries with the given language codes",
            paramLabel = "LANGUAGE")
    private List<String> excludeLanguages = ImmutableList.of();

    @CommandLine.Option(
            names = "--exclude",
            description = "Exclude entries that match this expression",
            paramLabel = "EXPRESSION")
    private List<String> excludes = ImmutableList.of();

    @CommandLine.Option(
            names = "--exclude-regex",
            description = "Exclude entries that match this regular expression",
            paramLabel = "EXPRESSION")
    private List<Pattern> excludeRegexes = ImmutableList.of();

    @CommandLine.Option(
            names = "--excludes-file",
            paramLabel = "PATH",
            description = "Read exclusion expressions from a file")
    private List<PatternsFileArgument> excludesFiles = ImmutableList.of();

    @CommandLine.Option(
            names = "--include",
            description = "Include entries that match this expression",
            paramLabel = "EXPRESSION")
    private List<String> includes = ImmutableList.of();

    @CommandLine.Option(
            names = "--include-regex",
            description = "Include entries that match this regular expression",
            paramLabel = "EXPRESSION")
    private List<Pattern> includeRegexes = ImmutableList.of();

    @CommandLine.Option(
            names = "--includes-file",
            paramLabel = "PATH",
            description = "Read inclusion expressions from a file")
    private List<PatternsFileArgument> includesFiles = ImmutableList.of();

    @CommandLine.Option(
            names = "--exclude-categories",
            split = "\\s*,\\s*",
            splitSynopsisLabel = ",",
            paramLabel = "CATEGORY",
            description = "Exclude entries belonging to the given categories. "
                    + "Options: ${COMPLETION-CANDIDATES}",
            completionCandidates = GameCategoryConverter.class)
    private List<GameCategory> excludeCategories = ImmutableList.of();

    public Filter toFilter() {
        Filter.FilterBuilder builder = Filter.builder();
        builder.includeRegions(ImmutableSet.copyOf(includeRegions));
        builder.excludeRegions(ImmutableSet.copyOf(excludeRegions));
        builder.includeLanguages(ImmutableSet.copyOf(includeLanguages));
        builder.excludeLanguages(ImmutableSet.copyOf(excludeLanguages));
        builder.excludeCategories(ImmutableSet.copyOf(excludeCategories));
        builder.excludes(merge(excludes, excludeRegexes, excludesFiles));
        builder.includes(merge(includes, includeRegexes, includesFiles));
        return builder.build();
    }
}