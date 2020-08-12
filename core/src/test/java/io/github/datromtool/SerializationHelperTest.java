package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.util.XMLValidator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static java.nio.file.Files.newInputStream;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

class SerializationHelperTest {

    @ParameterizedTest
    @MethodSource("validNoIntroDats")
    void testReadDats(Path validFile) throws Exception {
        Datafile datafile;
        try (InputStream inputStream = new BZip2CompressorInputStream(newInputStream(validFile))) {
            datafile = SerializationHelper.getInstance().loadXml(inputStream, Datafile.class);
        }
        assertNotNull(datafile);
        assertFalse(datafile.getGames().isEmpty());
        XMLValidator.validateDat(SerializationHelper.getInstance()
                .getXmlMapper()
                .writeValueAsBytes(datafile));
    }

    @ParameterizedTest
    @MethodSource("validHeaders")
    void testLoadDetectors(Path validFile) throws Exception {
        Detector detector = SerializationHelper.getInstance().loadDetector(validFile);
        assertNotNull(detector);
        assertFalse(detector.getRules().isEmpty());
        XMLValidator.validateDetector(SerializationHelper.getInstance()
                .getXmlMapper()
                .writeValueAsBytes(detector));
    }

    @Test
    void testLoadAppConfig() throws Exception {
        AppConfig config = SerializationHelper.getInstance()
                .loadAppConfig(Paths.get(ClassLoader.getSystemResource("config-test.yaml")
                        .toURI()));
        assertNotNull(config);
        assertNotNull(config.getScanner());
        assertEquals(config.getScanner().getThreads(), 4);
        assertEquals(config.getScanner().getMaxBufferSize(), 4 * 1024 * 1024);
    }

    @Test
    void testLoadDefaultAppConfig() {
        AppConfig config = SerializationHelper.getInstance().loadAppConfig();
        assertNotNull(config);
        assertEquals(config, AppConfig.builder().build());
    }

    @Test
    void testLoadRegionData() throws Exception {
        RegionData regionData = SerializationHelper.getInstance()
                .loadRegionData(Paths.get(ClassLoader.getSystemResource("region-data-test.yaml")
                        .toURI()));
        assertNotNull(regionData);
        assertNotNull(regionData.getRegions());
        assertEquals(regionData.getRegions().size(), 2);
        Iterator<RegionData.RegionDataEntry> iterator = regionData.getRegions().iterator();
        RegionData.RegionDataEntry r1 = iterator.next();
        assertEquals(r1.getCode(), "TST");
        assertTrue(r1.getPattern().matcher("Test").matches());
        assertTrue(r1.getPattern().matcher("test").matches());
        assertFalse(r1.getPattern().matcher("test2").matches());
        assertEquals(r1.getLanguages(), ImmutableSet.of("tt"));
        RegionData.RegionDataEntry r2 = iterator.next();
        assertEquals(r2.getCode(), "TS2");
        assertTrue(r2.getPattern().matcher("Test2").matches());
        assertTrue(r2.getPattern().matcher("test2").matches());
        assertFalse(r2.getPattern().matcher("test").matches());
        assertEquals(r2.getLanguages(), ImmutableSet.of("tt", "ts"));
    }

    @Test
    void testLoadDefaultRegionData() throws Exception {
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