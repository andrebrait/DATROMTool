package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.TestDirDependantTest;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.CrcKey;
import io.github.datromtool.domain.datafile.logiqx.Datafile;
import io.github.datromtool.domain.datafile.logiqx.Game;
import io.github.datromtool.domain.datafile.logiqx.Rom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.datromtool.util.TestUtils.getFilename;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

class FileScannerTest extends TestDirDependantTest {

    private static Map<String, CrcKey> crc32sums;
    private static Map<String, String> md5sums;
    private static Map<String, String> sha1sums;

    @BeforeAll
    static void setup() throws IOException {
        crc32sums = Files.readAllLines(scanTestDataSource.getParent().resolve("CRC32SUMS")).stream()
                .map(s -> s.split("\\s+"))
                .peek(s -> s[2] = Paths.get(s[2]).getFileName().toString())
                .collect(Collectors.toMap(s -> s[2], s -> CrcKey.of(Long.parseLong(s[1]), s[0])));
        md5sums = Files.readAllLines(scanTestDataSource.getParent().resolve("MD5SUMS")).stream()
                .map(s -> s.split("\\s+"))
                .peek(s -> s[1] = Paths.get(s[1]).getFileName().toString())
                .collect(Collectors.toMap(s -> s[1], s -> s[0]));
        sha1sums = Files.readAllLines(scanTestDataSource.getParent().resolve("SHA1SUMS")).stream()
                .map(s -> s.split("\\s+"))
                .peek(s -> s[1] = Paths.get(s[1]).getFileName().toString())
                .collect(Collectors.toMap(s -> s[1], s -> s[0]));
    }

    private static Stream<String> sizeDirStream() {
        return Stream.of("0016384", "0032768", "0065536", "0131072", "0262144", "0524288", "1048576", "1048577");
    }

    static Stream<Arguments> allSizeDirs() {
        return sizeDirStream().map(name -> argumentSet(name, name, true));
    }

    static Stream<Arguments> sizeDirsForMinSizeLimit() {
        Set<String> excluded = Set.of("0016384", "0032768");
        return sizeDirStream().map(name -> argumentSet(name, name, !excluded.contains(name)));
    }

    static Stream<Arguments> sizeDirsForMaxSizeLimit() {
        Set<String> excluded = Set.of("1048576", "1048577");
        return sizeDirStream().map(name -> argumentSet(name, name, !excluded.contains(name)));
    }

    static Stream<Arguments> sizeDirsForMinAndMaxSizeLimit() {
        Set<String> excluded = Set.of("0016384", "0032768", "1048576", "1048577");
        return sizeDirStream().map(name -> argumentSet(name, name, !excluded.contains(name)));
    }

    @ParameterizedTest
    @MethodSource("allSizeDirs")
    void testScan_defaultSettings(String sizeDirName, boolean expectResults) {
        FileScanner fileScanner = new FileScanner(
                AppConfig.FileScannerConfig.builder().build(),
                ImmutableList.of(),
                ImmutableList.of(),
                ImmutableList.of());
        ImmutableList<FileScanner.Result> results = fileScanner.scan(ImmutableList.of(
                scanTestDataSource.resolve(sizeDirName),
                scanTestDataSource.resolve("rar5").resolve(sizeDirName)));
        verifyScanResults(results, expectResults);
    }

    @ParameterizedTest
    @MethodSource("sizeDirsForMinSizeLimit")
    void testScan_minSizeLimit(String sizeDirName, boolean expectResults) {
        FileScanner fileScanner = new FileScanner(
                AppConfig.FileScannerConfig.builder().build(),
                ImmutableList.of(buildDatafile(64 * 1024L, 64 * 1024L * 1024L)),
                ImmutableList.of(),
                ImmutableList.of());
        ImmutableList<FileScanner.Result> results = fileScanner.scan(ImmutableList.of(
                scanTestDataSource.resolve(sizeDirName),
                scanTestDataSource.resolve("rar5").resolve(sizeDirName)));
        verifyScanResults(results, expectResults);
    }

    @ParameterizedTest
    @MethodSource("sizeDirsForMaxSizeLimit")
    void testScan_maxSizeLimit(String sizeDirName, boolean expectResults) {
        FileScanner fileScanner = new FileScanner(
                AppConfig.FileScannerConfig.builder().build(),
                ImmutableList.of(buildDatafile(16 * 1024L, 768 * 1024L)),
                ImmutableList.of(),
                ImmutableList.of());
        ImmutableList<FileScanner.Result> results = fileScanner.scan(ImmutableList.of(
                scanTestDataSource.resolve(sizeDirName),
                scanTestDataSource.resolve("rar5").resolve(sizeDirName)));
        verifyScanResults(results, expectResults);
    }

    @ParameterizedTest
    @MethodSource("sizeDirsForMinAndMaxSizeLimit")
    void testScan_minAndMaxSizeLimit(String sizeDirName, boolean expectResults) {
        FileScanner fileScanner = new FileScanner(
                AppConfig.FileScannerConfig.builder().build(),
                ImmutableList.of(buildDatafile(64 * 1024L, 768 * 1024L)),
                ImmutableList.of(),
                ImmutableList.of());
        ImmutableList<FileScanner.Result> results = fileScanner.scan(ImmutableList.of(
                scanTestDataSource.resolve(sizeDirName),
                scanTestDataSource.resolve("rar5").resolve(sizeDirName)));
        verifyScanResults(results, expectResults);
    }

    private void verifyScanResults(
            ImmutableList<FileScanner.Result> results,
            boolean expectResults) {
        if (expectResults) {
            assertFalse(results.isEmpty());
            assertEquals(2 * 18, results.size());
            for (FileScanner.Result i : results) {
                assertEquals(i.getUnheaderedSize(), i.getSize());
                String filename = getFilename(i);
                CrcKey crc32 = crc32sums.get(filename);
                assertNotNull(crc32);
                assertEquals((long) crc32.size(), i.getSize());
                assertEquals(crc32.crc(), i.getDigest().getCrc());
                assertEquals(md5sums.get(filename), i.getDigest().getMd5());
                assertEquals(sha1sums.get(filename), i.getDigest().getSha1());
            }
        } else {
            assertTrue(results.isEmpty());
        }
    }

    private Datafile buildDatafile(long minSize, long maxSize) {
        return Datafile.builder().games(ImmutableList.of(
                Game.builder()
                        .name("Test game 1")
                        .description("Test game 1")
                        .roms(ImmutableList.of(new Rom("Test rom 1.ext", minSize)))
                        .build(),
                Game.builder()
                        .name("Test game 2")
                        .description("Test game 2")
                        .roms(ImmutableList.of(new Rom("Test rom 2.ext", maxSize)))
                        .build()))
                .build();
    }

}
