package io.github.datromtool;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.util.XMLValidator;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static java.nio.file.Files.newInputStream;

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
//        ImmutableList<ParsedGame> parsedGameList = new GameParser(
//                SerializationHelper.getInstance().loadRegionData(),
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
//        Detector detector = SerializationHelper.getInstance()
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
                .loadAppConfig(ClassLoader.getSystemResource("config-test.yaml"));
        assertNotNull(config);
        assertNotNull(config.getScanner());
        assertEquals(4, (int) config.getScanner().getThreads());
        assertEquals(4 * 1024 * 1024, (int) config.getScanner().getMaxBufferSize());
    }

    @Test
    void testLoadDefaultAppConfig() {
        AppConfig config = SerializationHelper.getInstance().loadAppConfig();
        assertNotNull(config);
        assertEquals(AppConfig.builder().build(), config);
    }

    @Test
    void testLoadRegionData() throws Exception {
        RegionData regionData = SerializationHelper.getInstance()
                .loadRegionData(ClassLoader.getSystemResource("region-data-test.yaml"));
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