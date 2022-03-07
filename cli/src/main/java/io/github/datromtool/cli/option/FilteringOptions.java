package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.Patterns;
import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.converter.TrimmingLowerCaseConverter;
import io.github.datromtool.cli.converter.TrimmingUpperCaseConverter;
import io.github.datromtool.data.Filter;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import picocli.CommandLine;

import java.util.List;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static io.github.datromtool.cli.util.ArgumentUtils.merge;
import static lombok.AccessLevel.NONE;

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
            names = "--bad",
            description = "Include/exclude bad dumps",
            negatable = true)
    @Getter(NONE)
    private Boolean allowBad;

    @CommandLine.Option(
            names = "--proto",
            description = "Include/exclude prototype entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowProto;

    @CommandLine.Option(
            names = "--beta",
            description = "Include/exclude beta entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowBeta;

    @CommandLine.Option(
            names = "--demo",
            description = "Include/exclude demo entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowDemo;

    @CommandLine.Option(
            names = "--sample",
            description = "Include/exclude sample entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowSample;

    @CommandLine.Option(
            names = "--bios",
            description = "Include/exclude BIOS entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowBios;

    @CommandLine.Option(
            names = "--program",
            description = "Include/exclude program entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowProgram;

    @CommandLine.Option(
            names = "--chip",
            description = "Include/exclude enhancement chip entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowChip;

    @CommandLine.Option(
            names = "--pirate",
            description = "Include/exclude pirate entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowPirate;

    @CommandLine.Option(
            names = "--promo",
            description = "Include/exclude promotion entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowPromo;

    @CommandLine.Option(
            names = "--unlicensed",
            description = "Include/exclude unlicensed entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowUnlicensed;

    @CommandLine.Option(
            names = "--dlc",
            description = "Include/exclude DLCs",
            negatable = true)
    @Getter(NONE)
    private Boolean allowDlc;

    @CommandLine.Option(
            names = "--update",
            description = "Include/exclude software update entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowUpdate;

    @CommandLine.Option(
            names = "--no-all",
            description = "Exclude all the above")
    @Getter(NONE)
    @JsonIgnore
    private boolean excludeAll;

    private boolean reallySet(Boolean condition) {
        return excludeAll
                ? condition != null && condition
                : condition == null || condition;
    }

    public boolean isAllowBad() {
        return reallySet(allowBad);
    }

    public boolean isAllowProto() {
        return reallySet(allowProto);
    }

    public boolean isAllowBeta() {
        return reallySet(allowBeta);
    }

    public boolean isAllowDemo() {
        return reallySet(allowDemo);
    }

    public boolean isAllowSample() {
        return reallySet(allowSample);
    }

    public boolean isAllowBios() {
        return reallySet(allowBios);
    }

    public boolean isAllowProgram() {
        return reallySet(allowProgram);
    }

    public boolean isAllowChip() {
        return reallySet(allowChip);
    }

    public boolean isAllowPirate() {
        return reallySet(allowPirate);
    }

    public boolean isAllowPromo() {
        return reallySet(allowPromo);
    }

    public boolean isAllowUnlicensed() {
        return reallySet(allowUnlicensed);
    }

    public boolean isAllowDlc() {
        return reallySet(allowDlc);
    }

    public boolean isAllowUpdate() {
        return reallySet(allowUpdate);
    }

    public Filter toFilter() {
        Filter.FilterBuilder builder = Filter.builder();
        builder.includeRegions(ImmutableSet.copyOf(includeRegions));
        builder.excludeRegions(ImmutableSet.copyOf(excludeRegions));
        builder.includeLanguages(ImmutableSet.copyOf(includeLanguages));
        builder.excludeLanguages(ImmutableSet.copyOf(excludeLanguages));
        builder.allowProto(isAllowProto());
        builder.allowBeta(isAllowBeta());
        builder.allowDemo(isAllowDemo());
        builder.allowSample(isAllowSample());
        builder.allowBios(isAllowBios());
        ImmutableSet.Builder<Pattern> excludeRegexesBuilder = ImmutableSet.builder();
        if (!isAllowBad()) {
            excludeRegexesBuilder.add(Patterns.BAD);
        }
        if (!isAllowProgram()) {
            excludeRegexesBuilder.add(Patterns.PROGRAM);
        }
        if (!isAllowChip()) {
            excludeRegexesBuilder.add(Patterns.ENHANCEMENT_CHIP);
        }
        if (!isAllowPirate()) {
            excludeRegexesBuilder.add(Patterns.PIRATE);
        }
        if (!isAllowPromo()) {
            excludeRegexesBuilder.add(Patterns.PROMO);
        }
        if (!isAllowUnlicensed()) {
            excludeRegexesBuilder.add(Patterns.UNLICENSED);
        }
        if (!isAllowDlc()) {
            excludeRegexesBuilder.add(Patterns.DLC);
        }
        if (!isAllowUpdate()) {
            excludeRegexesBuilder.add(Patterns.UPDATE);
        }
        excludeRegexesBuilder.addAll(excludeRegexes);
        builder.excludes(merge(excludes, excludeRegexesBuilder.build(), excludesFiles));
        builder.includes(merge(includes, includeRegexes, includesFiles));
        return builder.build();
    }
}