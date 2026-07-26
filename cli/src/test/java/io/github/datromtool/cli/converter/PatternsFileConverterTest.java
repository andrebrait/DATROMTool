package io.github.datromtool.cli.converter;

import io.github.datromtool.cli.argument.PatternsFileArgument;
import io.github.datromtool.cli.argument.StringFilterArgument;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Row 30 + hostile rows H5/H6: direct unit tests for {@link PatternsFileConverter} loading both
 * JSON and YAML fixtures into a {@link StringFilterArgument}, and its failure modes.
 */
class PatternsFileConverterTest {

    private static final PatternsFileConverter CONVERTER = new PatternsFileConverter();

    private static Path fixture(String name) {
        try {
            return Paths.get(PatternsFileConverterTest.class
                    .getClassLoader()
                    .getResource("patterns/" + name)
                    .toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    // Row 30
    @Test
    void loadsStringsAndPatternsFromJsonFixture() {
        PatternsFileArgument argument = CONVERTER.convert(fixture("fixture.json").toString());
        StringFilterArgument stringFilter = argument.getStringFilter();
        assertEquals(java.util.List.of("FileString"), stringFilter.strings(), "JSON strings must load verbatim");
        assertEquals(1, stringFilter.patterns().size(), "JSON patterns must load exactly one entry");
        assertEquals(
                "FilePattern.*",
                stringFilter.patterns().get(0).pattern(),
                "JSON pattern text must load verbatim");
    }

    @Test
    void loadsStringsAndPatternsFromYamlFixture() {
        PatternsFileArgument argument = CONVERTER.convert(fixture("fixture.yaml").toString());
        StringFilterArgument stringFilter = argument.getStringFilter();
        assertEquals(java.util.List.of("YamlString"), stringFilter.strings(), "YAML strings must load verbatim");
        assertEquals(1, stringFilter.patterns().size(), "YAML patterns must load exactly one entry");
        assertEquals(
                "YamlPattern.*",
                stringFilter.patterns().get(0).pattern(),
                "YAML pattern text must load verbatim");
    }

    @Test
    void missingFileFailsAtConversionTime() {
        assertThrows(
                CommandLine.TypeConversionException.class,
                () -> CONVERTER.convert("/no/such/patterns-file-" + System.nanoTime() + ".json"),
                "a nonexistent patterns file must fail as a picocli TypeConversionException");
    }

    // Hostile row H5 (pinning actual behavior)
    @Test
    void invalidRegexInPatternsFileFailsAtConversionTime() {
        assertThrows(
                CommandLine.TypeConversionException.class,
                () -> CONVERTER.convert(fixture("invalid-regex.json").toString()),
                "an invalid regex inside the patterns file must fail as a picocli TypeConversionException");
    }

    // Hostile row H6 (pinning actual behavior)
    @Test
    void wrongTopLevelShapeFailsAtConversionTime() {
        assertThrows(
                CommandLine.TypeConversionException.class,
                () -> CONVERTER.convert(fixture("wrong-shape.yaml").toString()),
                "a patterns file whose top level is a list instead of an object must fail as a "
                        + "picocli TypeConversionException");
    }
}
