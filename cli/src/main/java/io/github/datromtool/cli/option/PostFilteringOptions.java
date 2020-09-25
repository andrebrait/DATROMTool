package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.util.ArgumentException;
import lombok.Data;
import lombok.NoArgsConstructor;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static io.github.datromtool.util.ArgumentUtils.combine;

@Data
@NoArgsConstructor
@JsonInclude(NON_DEFAULT)
public final class PostFilteringOptions {

    @CommandLine.Option(
            names = "--post-exclude",
            description = "Exclude entries that match this expression",
            paramLabel = "EXPRESSION")
    private List<Pattern> postExcludes = ImmutableList.of();

    @CommandLine.Option(
            names = "--post-excludes-file",
            paramLabel = "PATH",
            description = "Read exclusion expressions from a file")
    private List<Path> postExcludesFiles = ImmutableList.of();

    public PostFilter toPostFilter() throws ArgumentException, IOException {
        return PostFilter.builder()
                .excludes(ImmutableSet.copyOf(combine(postExcludes, postExcludesFiles)))
                .build();
    }

}