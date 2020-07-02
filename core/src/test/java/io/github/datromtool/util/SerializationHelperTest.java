package io.github.datromtool.util;

import io.github.datromtool.SerializationHelper;
import io.github.datromtool.generated.datafile.Datafile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

class SerializationHelperTest {

    @ParameterizedTest
    @MethodSource("validFiles")
    void testReadFiles(Path validFile) throws Exception {
        Datafile datafile = SerializationHelper.getInstance()
                .loadXml(validFile, Datafile.class);
        Assertions.assertNotNull(datafile);
        Assertions.assertFalse(datafile.getGame().isEmpty());
    }

    private static Stream<Arguments> validFiles() throws Exception {
        URL folderUrl = ClassLoader.getSystemResource("valid/dats/no-intro");
        return Files.list(Paths.get(folderUrl.toURI()))
                .map(Arguments::of);
    }
}