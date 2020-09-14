package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.NONE;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_DEFAULT)
public class FilteringOptions {

    @CommandLine.Option(
            names = "--include-region",
            description = "Includes only entries with the given region",
            paramLabel = "REGION")
    private List<String> includeRegions = ImmutableList.of();

    @CommandLine.Option(
            names = "--include-language",
            description = "Includes only entries with the given language",
            paramLabel = "LANGUAGE")
    private List<String> includeLanguages = ImmutableList.of();

    @CommandLine.Option(
            names = "--exclude-region",
            description = "Excludes all entries with the given region",
            paramLabel = "REGION")
    private List<String> excludeRegions = ImmutableList.of();

    @CommandLine.Option(
            names = "--exclude-language",
            description = "Exclude all entries with the given language",
            paramLabel = "LANGUAGE")
    private List<String> excludeLanguages = ImmutableList.of();

    @CommandLine.Option(
            names = "--exclude",
            description = "Excludes entries that match this expression",
            paramLabel = "EXPRESSION")
    private List<Pattern> excludes = ImmutableList.of();

    @CommandLine.Option(
            names = "--excludes-file",
            paramLabel = "PATH",
            description = "Provides a file with a list of expressions for exclusion")
    private List<Path> excludesFiles = ImmutableList.of();

    @CommandLine.Option(
            names = "--proto",
            description = "Includes/excludes prototype entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowProto;

    @CommandLine.Option(
            names = "--beta",
            description = "Includes/excludes beta entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowBeta;

    @CommandLine.Option(
            names = "--demo",
            description = "Includes/excludes demo entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowDemo;

    @CommandLine.Option(
            names = "--sample",
            description = "Includes/excludes sample entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowSample;

    @CommandLine.Option(
            names = "--bios",
            description = "Includes/excludes BIOS entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowBios;

    @CommandLine.Option(
            names = "--program",
            description = "Includes/excludes program entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowProgram;

    @CommandLine.Option(
            names = "--chip",
            description = "Includes/excludes enhancement chip entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowChip;

    @CommandLine.Option(
            names = "--pirate",
            description = "Includes/excludes pirate entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowPirate;

    @CommandLine.Option(
            names = "--promo",
            description = "Includes/excludes promotion entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowPromo;

    @CommandLine.Option(
            names = "--no-all",
            description = "Excludes all the above")
    @Getter(NONE)
    @JsonIgnore
    private boolean excludeAll;

    @CommandLine.Option(
            names = "--unlicensed",
            description = "Includes/excludes unlicensed entries",
            negatable = true)
    @Getter(NONE)
    private Boolean allowUnlicensed;

    private boolean reallySet(Boolean condition) {
        return excludeAll
                ? condition != null && condition
                : condition == null || condition;
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
        return allowUnlicensed == null || allowUnlicensed;
    }
}