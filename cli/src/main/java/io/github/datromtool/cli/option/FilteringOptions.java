package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.cli.converter.LowerCaseConverter;
import io.github.datromtool.cli.converter.UpperCaseConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.NONE;
import static lombok.AccessLevel.PRIVATE;

@Data
@Jacksonized
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
@JsonInclude(NON_DEFAULT)
public final class FilteringOptions {

    @CommandLine.Option(
            names = {"--ir", "--include-regions"},
            split = "\\s*,\\s*",
            splitSynopsisLabel = ",",
            description = "Include only entries with the given region codes",
            converter = UpperCaseConverter.class,
            paramLabel = "REGION")
    private List<String> includeRegions = ImmutableList.of();

    @CommandLine.Option(
            names = {"--il", "--include-languages"},
            split = "\\s*,\\s*",
            splitSynopsisLabel = ",",
            converter = LowerCaseConverter.class,
            description = "Include only entries with the given language codes",
            paramLabel = "LANGUAGE")
    private List<String> includeLanguages = ImmutableList.of();

    @CommandLine.Option(
            names = {"--er", "--exclude-regions"},
            split = "\\s*,\\s*",
            splitSynopsisLabel = ",",
            converter = UpperCaseConverter.class,
            description = "Exclude all entries with the given region codes",
            paramLabel = "REGION")
    private List<String> excludeRegions = ImmutableList.of();

    @CommandLine.Option(
            names = {"--el", "--exclude-languages"},
            split = "\\s*,\\s*",
            splitSynopsisLabel = ",",
            converter = LowerCaseConverter.class,
            description = "Exclude all entries with the given language codes",
            paramLabel = "LANGUAGE")
    private List<String> excludeLanguages = ImmutableList.of();

    @CommandLine.Option(
            names = "--exclude",
            description = "Exclude entries that match this expression",
            paramLabel = "EXPRESSION")
    private List<Pattern> excludes = ImmutableList.of();

    @CommandLine.Option(
            names = "--excludes-file",
            paramLabel = "PATH",
            description = "Read exclusion expressions from a file")
    private List<Path> excludesFiles = ImmutableList.of();

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
}