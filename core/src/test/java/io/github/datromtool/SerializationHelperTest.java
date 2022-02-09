package io.github.datromtool;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.util.XMLValidator;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.stream.Stream;

import static java.nio.file.Files.newInputStream;
import static org.junit.jupiter.api.Assertions.*;

class SerializationHelperTest extends ConfigDependantTest {

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
        Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return super.postVisitDirectory(dir, exc);
            }
        });
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
//        ImmutableList<ParsedGame> parsedGameList = new GameParser(
//                defaultHelper.loadRegionData(),
//                GameParser.DivergenceDetection.ONE_WAY)
//                .parse(datafile);
//        GameSorter gameSorter = new GameSorter(
//                SortingPreference.builder()
//                        .regions(ImmutableSet.of("USA", "UK", "EUR"))
//                        .languages(ImmutableSet.of("en", "es"))
//                        .prioritizeLanguages(true)
//                        .avoids(ImmutableSet.of(
//                                Pattern.compile("Virtual Console", CASE_INSENSITIVE)))
//                        .build());
//        ImmutableMap<String, ImmutableList<ParsedGame>> gamesByParent = gameSorter
//                .sortAndGroupByParent(parsedGameList);
//        GameFilterer gameFilterer = new GameFilterer(
//                Filter.builder()
//                        .regions(ImmutableSet.of("USA", "UK", "EUR"))
//                        .languages(ImmutableSet.of("en", "es"))
//                        .noBeta(true)
//                        .noDemo(true)
//                        .noProto(true)
//                        .noSample(true)
//                        .noBios(true)
//                        .excludes(ImmutableSet.of(
//                                Pattern.compile("GameCube", CASE_INSENSITIVE)))
//                        .build(),
//                PostFilter.builder().build());
//        ImmutableMap<String, ImmutableList<ParsedGame>> filteredGames = gamesByParent.entrySet()
//                .stream()
//                .map(e -> ImmutablePair.of(
//                        e.getKey(),
//                        gameFilterer.postFilter(gameFilterer.filter(e.getValue()))))
//                .filter(p -> CollectionUtils.isNotEmpty(p.getRight()))
//                .collect(ImmutableMap.toImmutableMap(Pair::getLeft, Pair::getRight));
//        Detector detector = defaultHelper
//                .loadDetector("No-Intro_NES.xml");
//        ImmutableList<FileScanner.Result> results =
//                new FileScanner(
//                        Runtime.getRuntime().availableProcessors(),
//                        datafile,
//                        detector,
//                        new FileScanner.Listener() {
//
//                            @Override
//                            void reportTotalItems(int totalItems) {
//                                logger.info("Reporting {} total items", totalItems);
//                            }
//
//                            @Override
//                            void reportStart(Path label, int thread) {
//                                logger.info(
//                                        "Reporting starting to read '{}'",
//                                        label);
//                            }
//
//                            @Override
//                            void reportProgress(
//                                    Path label,
//                                    int thread,
//                                    int percentage,
//                                    long speed) {
//                                ByteUnit unit = ByteUnit.getUnit(speed);
//                                logger.info(
//                                        "Reporting {}% for '{}' ({} {}/s)",
//                                        percentage,
//                                        label,
//                                        String.format("%.02f", unit.convert(speed)),
//                                        unit.getSymbol());
//                            }
//
//                            @Override
//                            void reportSkip(Path label, int thread, String message) {
//                                // Do nothing
//                            }
//
//                            @Override
//                            void reportFailure(
//                                    Path label,
//                                    int thread,
//                                    String message,
//                                    Throwable cause) {
//                                String causeMessage = cause.getMessage();
//                                logger.error(
//                                        "Reporting failure for '{}': {}. Cause: {}",
//                                        label,
//                                        message,
//                                        StringUtils.isNotEmpty(causeMessage)
//                                                ? causeMessage
//                                                : cause.getClass().getSimpleName());
//                            }
//
//                            @Override
//                            void reportFinish(Path label, int thread) {
//                                logger.info("Reporting finish for '{}'", label);
//                            }
//                        })
//                        .scan(Paths.get("/home/andre/Downloads/test-1g1r"));
//
//        Path out = SystemUtils.getUserHome().toPath().resolve("Downloads").resolve("result-1g1r");
//        Files.createDirectories(out);
//        Map<Path, List<FileScanner.Result>> allWithSameOrigin = results.stream()
//                .collect(Collectors.groupingBy(FileScanner.Result::getPath, Collectors.toList()));
//        Set<FileCopier.CopyDefinition> copyDefinitions = allWithSameOrigin.entrySet().stream()
//                .filter(e -> CollectionUtils.isNotEmpty(e.getValue()))
//                .map(e -> {
//                    ArchiveType type = e.getValue().get(0).getArchiveType();
//                    Path from = e.getKey();
//                    Path to = type == ArchiveType.RAR
//                            ? out.resolve(from.getFileName() + ".zip")
//                            : out.resolve(from.getFileName());
//                    ImmutableSet<FileCopier.ArchiveCopyDefinition> archiveCopyDefinitions;
//                    if (type == ArchiveType.NONE) {
//                        archiveCopyDefinitions = ImmutableSet.of();
//                    } else {
//                        archiveCopyDefinitions = e.getValue()
//                                .stream()
//                                .map(FileScanner.Result::getArchivePath)
//                                .filter(StringUtils::isNotBlank)
//                                .map(s -> FileCopier.ArchiveCopyDefinition.builder()
//                                        .source(s)
//                                        .destination(s)
//                                        .build())
//                                .collect(ImmutableSet.toImmutableSet());
//                    }
//                    return FileCopier.CopyDefinition.builder()
//                            .fromType(type)
//                            .from(from)
//                            .to(to)
//                            .archiveCopyDefinitions(archiveCopyDefinitions)
//                            .build();
//                }).collect(ImmutableSet.toImmutableSet());
//        new FileCopier(true, 1, new FileCopier.Listener() {
//
//            @Override
//            void reportStart(Path from, Path to, int thread) {
//                logger.info(
//                        "Reporting starting to copy '{}' to '{}'",
//                        from,
//                        to);
//            }
//
//            @Override
//            void reportProgress(
//                    Path from,
//                    Path to,
//                    int thread,
//                    int percentage,
//                    long speed) {
//                ByteUnit unit = ByteUnit.getUnit(speed);
//                logger.info(
//                        "Reporting {}% for '{}' to '{}' ({} {}/s)",
//                        percentage,
//                        from,
//                        to,
//                        String.format("%.02f", unit.convert(speed)),
//                        unit.getSymbol());
//            }
//
//            @Override
//            void reportSkip(Path from, Path to, int thread, String message) {
//                // Do nothing
//            }
//
//            @Override
//            void reportFailure(
//                    Path from,
//                    Path to,
//                    int thread,
//                    String message,
//                    Throwable cause) {
//                String causeMessage = cause.getMessage();
//                logger.error(
//                        "Reporting failure for '{}' to '{}': {}. Cause: {}",
//                        from,
//                        to,
//                        message,
//                        StringUtils.isNotEmpty(causeMessage)
//                                ? causeMessage
//                                : cause.getClass().getSimpleName());
//            }
//
//            @Override
//            void reportFinish(Path from, Path to, int thread) {
//                logger.info("Reporting finish for '{}' to '{}'", from, to);
//            }
//        }).copy(copyDefinitions);
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