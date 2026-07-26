package io.github.datromtool;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.config.CopyBufferSize;
import io.github.datromtool.config.CopyThreads;
import io.github.datromtool.config.ScanBufferSize;
import io.github.datromtool.config.ScanMaxBufferSize;
import io.github.datromtool.config.ScanThreads;
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
        assertEquals(4, config.getScanner().getThreads().value());
        assertEquals(4 * 1024 * 1024, config.getScanner().getMaxBufferSize().bytes());
    }

    @Test
    void testLoadAppConfigFromFile() {
        AppConfig config = testHelper.loadAppConfig();
        assertNotNull(config);
        assertNotNull(config.getScanner());
        assertEquals(24, config.getScanner().getThreads().value());
        assertEquals(32768, config.getScanner().getMaxBufferSize().bytes());
        assertEquals(12, config.getCopier().getThreads().value());
    }

    // Row: performance-primitive wrapper records serialize as plain numbers, not nested
    // objects, and round-trip through both JSON and YAML unchanged (issue #14 step 3).
    @Test
    void testAppConfigWrapperRecordsSerializeAsPlainNumbersAndRoundTrip() throws Exception {
        AppConfig config = AppConfig.builder()
                .scanner(AppConfig.FileScannerConfig.builder()
                        .threads(new ScanThreads(7))
                        .defaultBufferSize(new ScanBufferSize(65536))
                        .maxBufferSize(new ScanMaxBufferSize(1048576))
                        .build())
                .copier(AppConfig.FileCopierConfig.builder()
                        .threads(new CopyThreads(9))
                        .bufferSize(new CopyBufferSize(4096))
                        .build())
                .build();

        String json = emptyHelper.getJsonMapper().writeValueAsString(config);
        String normalizedJson = json.replaceAll("\\s", "");
        assertTrue(
                normalizedJson.contains("\"threads\":7"),
                "scanner threads must serialize as a plain number, got: " + json);
        assertTrue(
                normalizedJson.contains("\"threads\":9"),
                "copier threads must serialize as a plain number, got: " + json);
        assertFalse(
                json.contains("\"value\""),
                "wrapper records must not serialize as a nested {\"value\": ...} object, got: " + json);
        assertFalse(
                json.contains("\"bytes\""),
                "wrapper records must not serialize as a nested {\"bytes\": ...} object, got: " + json);
        assertEquals(config, emptyHelper.getJsonMapper().readValue(json, AppConfig.class));

        String yaml = emptyHelper.getYamlMapper().writeValueAsString(config);
        assertFalse(
                yaml.contains("value:"),
                "wrapper records must not serialize as a nested 'value:' object, got: " + yaml);
        assertFalse(
                yaml.contains("bytes:"),
                "wrapper records must not serialize as a nested 'bytes:' object, got: " + yaml);
        assertEquals(config, emptyHelper.getYamlMapper().readValue(yaml, AppConfig.class));
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
        assertNotNull(regionData.regions());
        assertEquals(2, regionData.regions().size());
        Iterator<RegionData.RegionDataEntry> iterator = regionData.regions().iterator();
        RegionData.RegionDataEntry r1 = iterator.next();
        assertEquals("TST", r1.code());
        assertTrue(r1.pattern().matcher("Test").matches());
        assertTrue(r1.pattern().matcher("test").matches());
        assertFalse(r1.pattern().matcher("test2").matches());
        assertEquals(ImmutableSet.of("tt"), r1.languages());
        RegionData.RegionDataEntry r2 = iterator.next();
        assertEquals("TS2", r2.code());
        assertTrue(r2.pattern().matcher("Test2").matches());
        assertTrue(r2.pattern().matcher("test2").matches());
        assertFalse(r2.pattern().matcher("test").matches());
        assertEquals(ImmutableSet.of("tt", "ts"), r2.languages());
    }

    @Test
    void testLoadRegionDataFromFile() throws Exception {
        RegionData regionData = testHelper.loadRegionData();
        assertNotNull(regionData);
        assertNotNull(regionData.regions());
        assertEquals(2, regionData.regions().size());
        Iterator<RegionData.RegionDataEntry> iterator = regionData.regions().iterator();
        RegionData.RegionDataEntry r1 = iterator.next();
        assertEquals("BRA", r1.code());
        assertTrue(r1.pattern().matcher("Brazil").matches());
        assertTrue(r1.pattern().matcher("brazil").matches());
        assertFalse(r1.pattern().matcher("brazil2").matches());
        assertEquals(ImmutableSet.of("pt"), r1.languages());
        RegionData.RegionDataEntry r2 = iterator.next();
        assertEquals("EUR", r2.code());
        assertTrue(r2.pattern().matcher("Europe").matches());
        assertTrue(r2.pattern().matcher("europe").matches());
        assertTrue(r2.pattern().matcher("World").matches());
        assertTrue(r2.pattern().matcher("world").matches());
        assertFalse(r2.pattern().matcher("europe2").matches());
        assertFalse(r2.pattern().matcher("world2").matches());
        assertEquals(ImmutableSet.of("en"), r2.languages());
    }

    @Test
    void testLoadDefaultRegionData() throws Exception {
        RegionData regionData = emptyHelper.loadRegionData();
        assertNotNull(regionData);
        assertNotNull(regionData.regions());
        assertFalse(regionData.regions().isEmpty());
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