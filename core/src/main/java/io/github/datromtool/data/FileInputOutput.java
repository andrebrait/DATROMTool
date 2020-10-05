package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.io.ArchiveType;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@Jacksonized
@Value
@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@JsonInclude(NON_DEFAULT)
public class FileInputOutput {

    @Builder.Default
    List<Path> inputDirs = ImmutableList.of();
    Path outputDir;
    boolean alphabetic;
    ArchiveType archiveType;
    boolean forceSubfolder;

}
