package io.github.datromtool.util;

import io.github.datromtool.generated.datafile.Datafile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

class XMLLoaderTest {

    @ParameterizedTest
    @MethodSource("validFiles")
    void testUnmarshalFiles(Path validFile) throws Exception {
        XMLLoader<Datafile> xmlLoader = XMLLoader.forClass(Datafile.class);
        Datafile datafile = xmlLoader.unmarshal(validFile);
        Assertions.assertNotNull(datafile);
        Assertions.assertFalse(datafile.getGame().isEmpty());
    }

    private static Stream<Arguments> validFiles() throws URISyntaxException, IOException {
        URL folderUrl = ClassLoader.getSystemResource("valid");
        return Files.list(Paths.get(folderUrl.toURI())).map(Arguments::of);
    }
}