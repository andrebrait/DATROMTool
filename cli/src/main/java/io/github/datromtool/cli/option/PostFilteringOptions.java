package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.data.PostFilter;
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
    private List<PatternsFileArgument> postExcludesFiles = ImmutableList.of();

    public PostFilter toPostFilter() {
        return PostFilter.builder()
                .excludes(merge(postExcludes, postExcludesFiles))
                .build();
    }
}