package io.github.datromtool.cli.converter;

import io.github.datromtool.cli.argument.DatafileArgument;
import io.github.datromtool.domain.datafile.logiqx.Datafile;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Row 31: direct unit tests for {@link DatafileConverter}'s eager DAT loading at parse time. */
class DatafileConverterTest {

    private static final DatafileConverter CONVERTER = new DatafileConverter();

    private static Path fixture(String name) {
        try {
            return Paths.get(DatafileConverterTest.class
                    .getClassLoader()
                    .getResource("datafiles/" + name)
                    .toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void loadsMinimalValidDatafileEagerly() {
        DatafileArgument argument = CONVERTER.convert(fixture("minimal.dat").toString());
        Datafile datafile = argument.getDatafile();
        assertEquals("Test DAT", datafile.getHeader().getName(), "header name must load verbatim");
        assertEquals(1, datafile.getGames().size(), "the fixture declares exactly one game");
        assertEquals(
                "Test Game (USA)",
                datafile.getGames().get(0).getName(),
                "game name must load verbatim");
        assertEquals(
                1,
                datafile.getGames().get(0).getRoms().size(),
                "the fixture game declares exactly one rom");
    }

    @Test
    void nonexistentPathFailsAtConversionTime() {
        assertThrows(
                CommandLine.TypeConversionException.class,
                () -> CONVERTER.convert("/no/such/datafile-" + System.nanoTime() + ".dat"),
                "a nonexistent DAT path must fail as a picocli TypeConversionException");
    }
}
