package io.github.datromtool;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.util.ArchiveUtils;
import io.github.datromtool.util.XMLValidator;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;

import static java.nio.file.Files.newInputStream;
import static org.junit.jupiter.api.Assertions.*;

class SerializationHelperTest extends TestDirDependantTest {

    static SerializationHelper testHelper;

    @BeforeAll
    static void setupHelpers() {
        testHelper = SerializationHelper.getInstance(testDir.resolve("config"));
    }

    private Path tempDir;
    private SerializationHelper emptyHelper;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("datromtool_serialization_test_");
        emptyHelper = SerializationHelper.getInstance(tempDir);
    }

    @AfterEach
    void tearDown() throws Exception {
        ArchiveUtils.deleteFolder(tempDir);
    }

    @ParameterizedTest
    @MethodSource("validNoIntroDats")
    void testReadDats(Path validFile) throws Exception {
        Datafile datafile;
        try (InputStream inputStream = new BZip2CompressorInputStream(newInputStream(validFile))) {
            datafile = emptyHelper.loadXml(inputStream, Datafile.class);
        }
        assertNotNull(datafile);
        assertFalse(datafile.getGames().isEmpty());
        XMLValidator.validateDat(emptyHelper.getXmlMapper().writeValueAsBytes(datafile));
    }

    @ParameterizedTest
    @MethodSource("validHeaders")
    void testLoadDetectors(Path validFile) throws Exception {
        Detector detector = emptyHelper.loadDetector(validFile);
        assertNotNull(detector);
        assertFalse(detector.getRules().isEmpty());
        XMLValidator.validateDetector(emptyHelper
                .getXmlMapper()
                .writeValueAsBytes(detector));
    }

    @Test
    void testLoadAppConfig() throws Exception {
        AppConfig config = emptyHelper.loadAppConfig(ClassLoader.getSystemResource("config-test.yaml"));
        assertNotNull(config);
        assertNotNull(config.getScanner());
        assertEquals(4, (int) config.getScanner().getThreads());
        assertEquals(4 * 1024 * 1024, (int) config.getScanner().getMaxBufferSize());
    }

    @Test
    void testLoadAppConfigFromFile() {
        AppConfig config = testHelper.loadAppConfig();
        assertNotNull(config);
        assertNotNull(config.getScanner());
        assertEquals(24, (int) config.getScanner().getThreads());
        assertEquals(32768, (int) config.getScanner().getMaxBufferSize());
        assertEquals(12, (int) config.getCopier().getThreads());
    }

    @Test
    void testLoadDefaultAppConfig() {
        AppConfig config = emptyHelper.loadAppConfig();
        assertNotNull(config);
        assertEquals(AppConfig.builder().build(), config);
    }

    @Test
    void testLoadRegionData() throws Exception {
        RegionData regionData = emptyHelper.loadRegionData(ClassLoader.getSystemResource("region-data-test.yaml"));
        assertNotNull(regionData);
        assertNotNull(regionData.getRegions());
        assertEquals(2, regionData.getRegions().size());
        Iterator<RegionData.RegionDataEntry> iterator = regionData.getRegions().iterator();
        RegionData.RegionDataEntry r1 = iterator.next();
        assertEquals("TST", r1.getCode());
        assertTrue(r1.getPattern().matcher("Test").matches());
        assertTrue(r1.getPattern().matcher("test").matches());
        assertFalse(r1.getPattern().matcher("test2").matches());
        assertEquals(ImmutableSet.of("tt"), r1.getLanguages());
        RegionData.RegionDataEntry r2 = iterator.next();
        assertEquals("TS2", r2.getCode());
        assertTrue(r2.getPattern().matcher("Test2").matches());
        assertTrue(r2.getPattern().matcher("test2").matches());
        assertFalse(r2.getPattern().matcher("test").matches());
        assertEquals(ImmutableSet.of("tt", "ts"), r2.getLanguages());
    }

    @Test
    void testLoadRegionDataFromFile() throws Exception {
        RegionData regionData = testHelper.loadRegionData();
        assertNotNull(regionData);
        assertNotNull(regionData.getRegions());
        assertEquals(2, regionData.getRegions().size());
        Iterator<RegionData.RegionDataEntry> iterator = regionData.getRegions().iterator();
        RegionData.RegionDataEntry r1 = iterator.next();
        assertEquals("BRA", r1.getCode());
        assertTrue(r1.getPattern().matcher("Brazil").matches());
        assertTrue(r1.getPattern().matcher("brazil").matches());
        assertFalse(r1.getPattern().matcher("brazil2").matches());
        assertEquals(ImmutableSet.of("pt"), r1.getLanguages());
        RegionData.RegionDataEntry r2 = iterator.next();
        assertEquals("EUR", r2.getCode());
        assertTrue(r2.getPattern().matcher("Europe").matches());
        assertTrue(r2.getPattern().matcher("europe").matches());
        assertTrue(r2.getPattern().matcher("World").matches());
        assertTrue(r2.getPattern().matcher("world").matches());
        assertFalse(r2.getPattern().matcher("europe2").matches());
        assertFalse(r2.getPattern().matcher("world2").matches());
        assertEquals(ImmutableSet.of("en"), r2.getLanguages());
    }

    @Test
    void testLoadDefaultRegionData() throws Exception {
        RegionData regionData = emptyHelper.loadRegionData();
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