package io.github.datromtool.cli.option;

import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.converter.PatternsFileConverter;
import io.github.datromtool.data.PostFilter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pins {@link PostFilteringOptions#toPostFilter()}: all three exclude sources must merge. */
class PostFilteringOptionsTest {

    @CommandLine.Command
    private static final class Holder {

        @CommandLine.ArgGroup(exclusive = false)
        PostFilteringOptions postFilteringOptions = new PostFilteringOptions();
    }

    private static PostFilter parse(String... args) {
        Holder holder = new Holder();
        CommandLine commandLine = new CommandLine(holder);
        commandLine.registerConverter(PatternsFileArgument.class, new PatternsFileConverter());
        commandLine.parseArgs(args);
        return holder.postFilteringOptions.toPostFilter();
    }

    private static Path fixture(String name) {
        try {
            return Paths.get(PostFilteringOptionsTest.class
                    .getClassLoader()
                    .getResource("patterns/" + name)
                    .toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean anyMatches(Iterable<Pattern> patterns, String candidate) {
        for (Pattern p : patterns) {
            if (p.matcher(candidate).matches()) {
                return true;
            }
        }
        return false;
    }

    // Row 15
    @Test
    void allThreeExcludeSourcesMergeIntoPostFilterExcludes() {
        PostFilter postFilter = parse(
                "--post-exclude", "a(b",
                "--post-exclude-regex", "c.d",
                "--post-excludes-file", fixture("fixture.json").toString());
        assertTrue(
                anyMatches(postFilter.getExcludes(), "a(b"),
                "--post-exclude literal must merge into PostFilter.excludes, got: " + postFilter.getExcludes());
        assertTrue(
                anyMatches(postFilter.getExcludes(), "cXd"),
                "--post-exclude-regex must merge into PostFilter.excludes, got: " + postFilter.getExcludes());
        assertTrue(
                anyMatches(postFilter.getExcludes(), "FileString"),
                "--post-excludes-file strings must merge into PostFilter.excludes, got: " + postFilter.getExcludes());
        assertTrue(
                anyMatches(postFilter.getExcludes(), "FilePatternXYZ"),
                "--post-excludes-file patterns must merge into PostFilter.excludes, got: " + postFilter.getExcludes());
    }
}
