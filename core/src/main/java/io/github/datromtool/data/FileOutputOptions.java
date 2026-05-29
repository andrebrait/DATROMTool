package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.datromtool.io.ArchiveType;

import java.nio.file.Path;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record FileOutputOptions(Path outputDir, boolean alphabetic, ArchiveType archiveType, boolean forceSubfolder) {
}
