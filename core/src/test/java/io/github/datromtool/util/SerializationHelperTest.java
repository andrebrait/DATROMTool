package io.github.datromtool.util;

import io.github.datromtool.SerializationHelper;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.generated.datafile.Datafile;
import io.github.datromtool.generated.headers.Detector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

class SerializationHelperTest {

    @ParameterizedTest
    @MethodSource("validNoIntroDats")
    void testReadDats(Path validFile) throws Exception {
        Datafile datafile = SerializationHelper.getInstance().loadXml(validFile, Datafile.class);
        Assertions.assertNotNull(datafile);
        Assertions.assertFalse(datafile.getGame().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("validHeaders")
    void testReadDetectors(Path validFile) throws Exception {
        Detector detector = SerializationHelper.getInstance().loadXml(validFile, Detector.class);
        Assertions.assertNotNull(detector);
        Assertions.assertFalse(detector.getRule().isEmpty());
    }

    @Test
    void testLoadRegionData() throws Exception {
        List<RegionData> regionDataList = SerializationHelper.getInstance().loadRegionData();
        Assertions.assertNotNull(regionDataList);
        Assertions.assertFalse(regionDataList.isEmpty());
    }

    static Stream<Arguments> validNoIntroDats() throws Exception {
        URL folderUrl = ClassLoader.getSystemResource("valid/dats/no-intro");
        return Files.list(Paths.get(folderUrl.toURI())).map(Arguments::of);
    }

    static Stream<Arguments> validHeaders() throws Exception {
        URL folderUrl = ClassLoader.getSystemResource("headers");
        return Files.list(Paths.get(folderUrl.toURI())).map(Arguments::of);
    }
}