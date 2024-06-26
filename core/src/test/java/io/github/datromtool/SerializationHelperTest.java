package io.github.datromtool;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.domain.datafile.logiqx.Datafile;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.util.ArchiveUtils;
import io.github.datromtool.util.XMLValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
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
    @MethodSource("validLogiqxDats")
    void testReadLogiqxDats(Path validFile) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(validFile))) {
            ZipEntry zi;
            while ((zi = zis.getNextEntry()) != null) {
                if (zi.isDirectory()) {
                    continue;
                }
                if (!zi.getName().matches("(?i)^.+.dat$")) {
                    continue;
                }
                log.info("Reading '{}'", validFile.resolve(zi.getName()));
                Datafile datafile = emptyHelper.loadXml(toNonCloseable(zis), Datafile.class);
                assertNotNull(datafile);
                // Blacklist of known bad DATs
                if (validFile.getFileName().toString().equals("No-Intro Love Pack (PC XML) (2023-03-08).zip")
                        && (datafile.getHeader().getName().startsWith("Non-Game - Miscellaneous - Instructional (Audio CD)")
                        || datafile.getHeader().getName().startsWith("Non-Redump - Microsoft - Xbox Series X")
                        || datafile.getHeader().getName().startsWith("Non-Redump - Super Audio CD")
                        || datafile.getHeader().getName().startsWith("VTech - Mobigo"))) {
                    assertTrue(datafile.getGames().isEmpty());
                    continue;
                }
                XMLValidator.validateLogiqxDat(emptyHelper.getXmlMapper().writeValueAsBytes(datafile));
            }
        }
    }

    // TODO: reenable later
    @Disabled
    @ParameterizedTest
    @MethodSource("validNoIntroDats")
    void testReadNoIntroDats(Path validFile) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(validFile))) {
            ZipEntry zi;
            while ((zi = zis.getNextEntry()) != null) {
                if (zi.isDirectory()) {
                    continue;
                }
                if (!zi.getName().matches("(?i)^.+.dat$")) {
                    continue;
                }
                log.info("Reading '{}'", validFile.resolve(zi.getName()));
                // TODO: replace with Nointro Datafile
                Datafile datafile = emptyHelper.loadXml(toNonCloseable(zis), Datafile.class);
                assertNotNull(datafile);
                XMLValidator.validateNoIntroDat(emptyHelper.getXmlMapper().writeValueAsBytes(datafile));
            }
        }
    }

    private static InputStream toNonCloseable(InputStream other) {
        return new FilterInputStream(other) {
            @Override
            public void close() {
            }
        };
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

    static Stream<Arguments> validLogiqxDats() throws Exception {
        URL folderUrl = ClassLoader.getSystemResource("valid-dats/logiqx");
        return Files.list(Paths.get(folderUrl.toURI())).map(Arguments::of);
    }

    static Stream<Arguments> validNoIntroDats() throws Exception {
        URL folderUrl = ClassLoader.getSystemResource("valid-dats/nointro");
        return Files.list(Paths.get(folderUrl.toURI())).map(Arguments::of);
    }

    static Stream<Arguments> validHeaders() throws Exception {
        URL folderUrl = ClassLoader.getSystemResource("detectors");
        return Files.list(Paths.get(folderUrl.toURI())).map(Arguments::of);
    }
}