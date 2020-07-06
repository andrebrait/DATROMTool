package io.github.datromtool;

import io.github.datromtool.data.RegionData;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.util.XMLValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SerializationHelperTest {

    @ParameterizedTest
    @MethodSource("validNoIntroDats")
    void testReadDats(Path validFile) throws Exception {
        Datafile datafile = SerializationHelper.getInstance().loadXml(validFile, Datafile.class);
        assertNotNull(datafile);
        assertFalse(datafile.getGames().isEmpty());
        XMLValidator.validateDat(SerializationHelper.getInstance()
                .getXmlMapper()
                .writeValueAsBytes(datafile));
    }

    @ParameterizedTest
    @MethodSource("validHeaders")
    void testReadDetectors(Path validFile) throws Exception {
        Detector detector = SerializationHelper.getInstance().loadXml(validFile, Detector.class);
        assertNotNull(detector);
        assertFalse(detector.getRules().isEmpty());
        XMLValidator.validateDetector(SerializationHelper.getInstance()
                .getXmlMapper()
                .writeValueAsBytes(detector));
    }

    @Test
    void testLoadRegionData() throws Exception {
        RegionData regionData = SerializationHelper.getInstance().loadRegionData();
        assertNotNull(regionData);
        assertNotNull(regionData.getRegions());
        assertFalse(regionData.getRegions().isEmpty());
    }

    static Stream<Arguments> validNoIntroDats() throws Exception {
        URL folderUrl = ClassLoader.getSystemResource("valid/dats/no-intro");
        return Files.list(Paths.get(folderUrl.toURI())).map(Arguments::of);
    }

    static Stream<Arguments> validHeaders() throws Exception {
        URL folderUrl = ClassLoader.getSystemResource("detectors");
        return Files.list(Paths.get(folderUrl.toURI())).map(Arguments::of);
    }
}