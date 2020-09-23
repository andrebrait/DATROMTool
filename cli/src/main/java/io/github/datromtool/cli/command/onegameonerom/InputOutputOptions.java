package io.github.datromtool.cli.command.onegameonerom;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.cli.ArchiveCompletionCandidates;
import io.github.datromtool.cli.converter.ArchiveTypeConverter;
import io.github.datromtool.io.ArchiveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@Data
@Jacksonized
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
@JsonInclude(NON_DEFAULT)
public final class InputOutputOptions {

    public static final String INPUT_DIR_OPTION = "--input-dir";
    public static final String OUTPUT_DIR_OPTION = "--output-dir";
    public static final String ARCHIVE_FORMAT_OPTION = "--archive";
    public static final String FORCE_SUBFOLDER_OPTION = "--force-subfolder";

    @CommandLine.Option(
            names = {"-i", INPUT_DIR_OPTION},
            paramLabel = "PATH",
            description = "Base directory for scanning ROM files")
    private List<Path> inputDirs = ImmutableList.of();

    @CommandLine.Option(
            names = {"-o", OUTPUT_DIR_OPTION},
            paramLabel = "PATH",
            description = "Output directory for the resulting files")
    private Path outputDir;

    @NonNull
    @CommandLine.Option(
            names = {"-a", ARCHIVE_FORMAT_OPTION},
            paramLabel = "FORMAT",
            description = "Archive format for output. Options: ${COMPLETION-CANDIDATES}",
            converter = ArchiveTypeConverter.class,
            completionCandidates = ArchiveCompletionCandidates.class,
            showDefaultValue = CommandLine.Help.Visibility.NEVER)
    private ArchiveType archiveType = ArchiveType.NONE;

    @CommandLine.Option(
            names = FORCE_SUBFOLDER_OPTION,
            description = "Create subfolders even for entries with one file")
    private boolean forceSubfolder;

}
