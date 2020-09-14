package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.Pair;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.datafile.Game;
import io.github.datromtool.domain.datafile.Rom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.datromtool.util.TestUtils.getFilename;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static java.util.Objects.requireNonNull;

@EnabledIf(
        value = "io.github.datromtool.io.ArchiveUtils#isUnrarAvailable",
        disabledReason = "'unrar' is not available")
class FileScannerTestWithUnrar {

    private static final String TEST_DATA_FOLDER = "../test-data";
    private Path testDataSource;

    private Map<String, Pair<Long, String>> crc32sums;
    private Map<String, String> md5sums;
    private Map<String, String> sha1sums;

    @BeforeEach
    void setup() throws IOException {
        String testDir = System.getenv("DATROMTOOL_TEST_DIR");
        if (testDir == null && Files.isDirectory(Paths.get(TEST_DATA_FOLDER))) {
            testDir = TEST_DATA_FOLDER;
        }
        testDataSource = Paths.get(requireNonNull(testDir), "data", "files");
        crc32sums = Files.readAllLines(testDataSource.getParent().resolve("CRC32SUMS")).stream()
                .map(s -> s.split("\\s+"))
                .peek(s -> s[2] = Paths.get(s[2]).getFileName().toString())
                .collect(Collectors.toMap(s -> s[2], s -> Pair.of(Long.parseLong(s[1]), s[0])));
        md5sums = Files.readAllLines(testDataSource.getParent().resolve("MD5SUMS")).stream()
                .map(s -> s.split("\\s+"))
                .peek(s -> s[1] = Paths.get(s[1]).getFileName().toString())
                .collect(Collectors.toMap(s -> s[1], s -> s[0]));
        sha1sums = Files.readAllLines(testDataSource.getParent().resolve("SHA1SUMS")).stream()
                .map(s -> s.split("\\s+"))
                .peek(s -> s[1] = Paths.get(s[1]).getFileName().toString())
                .collect(Collectors.toMap(s -> s[1], s -> s[0]));
    }

    @Test
    void testScan_defaultSettings() throws Exception {
        FileScanner fileScanner = new FileScanner(AppConfig.builder().build(), null, null, null);
        ImmutableList<FileScanner.Result> results = fileScanner.scan(testDataSource);
        assertFalse(results.isEmpty());
        assertEquals(crc32sums.size() * 18, results.size());
        for (FileScanner.Result i : results) {
            assertEquals(i.getUnheaderedSize(), i.getSize());
            String filename = getFilename(i);
            Pair<Long, String> crc32 = crc32sums.get(filename);
            assertNotNull(crc32);
            assertEquals((long) crc32.getLeft(), i.getSize());
            assertEquals(crc32.getRight(), i.getDigest().getCrc());
            assertEquals(md5sums.get(filename), i.getDigest().getMd5());
            assertEquals(sha1sums.get(filename), i.getDigest().getSha1());
        }
    }

    @Test
    void testScan_minSizeLimit() throws Exception {
        FileScanner fileScanner = new FileScanner(
                AppConfig.builder().build(),
                buildDatafile(64 * 1024L, 64 * 1024L * 1024L),
                null,
                null);
        ImmutableList<FileScanner.Result> results = fileScanner.scan(testDataSource);
        assertFalse(results.isEmpty());
        assertEquals((crc32sums.size() - 4) * 18, results.size());
        for (FileScanner.Result i : results) {
            assertEquals(i.getUnheaderedSize(), i.getSize());
            String filename = getFilename(i);
            Pair<Long, String> crc32 = crc32sums.get(filename);
            assertNotNull(crc32);
            assertEquals((long) crc32.getLeft(), i.getSize());
            assertEquals(crc32.getRight(), i.getDigest().getCrc());
            assertEquals(md5sums.get(filename), i.getDigest().getMd5());
            assertEquals(sha1sums.get(filename), i.getDigest().getSha1());
        }
    }

    @Test
    void testScan_maxSizeLimit() throws Exception {
        FileScanner fileScanner = new FileScanner(
                AppConfig.builder().build(),
                buildDatafile(16 * 1024L, 768 * 1024L),
                null,
                null);
        ImmutableList<FileScanner.Result> results = fileScanner.scan(testDataSource);
        assertFalse(results.isEmpty());
        assertEquals((crc32sums.size() - 4) * 18, results.size());
        for (FileScanner.Result i : results) {
            assertEquals(i.getUnheaderedSize(), i.getSize());
            String filename = getFilename(i);
            Pair<Long, String> crc32 = crc32sums.get(filename);
            assertNotNull(crc32);
            assertEquals((long) crc32.getLeft(), i.getSize());
            assertEquals(crc32.getRight(), i.getDigest().getCrc());
            assertEquals(md5sums.get(filename), i.getDigest().getMd5());
            assertEquals(sha1sums.get(filename), i.getDigest().getSha1());
        }
    }

    @Test
    void testScan_minAndmaxSizeLimit() throws Exception {
        FileScanner fileScanner = new FileScanner(
                AppConfig.builder().build(),
                buildDatafile(64 * 1024L, 768 * 1024L),
                null,
                null);
        ImmutableList<FileScanner.Result> results = fileScanner.scan(testDataSource);
        assertFalse(results.isEmpty());
        assertEquals((crc32sums.size() - 8) * 18, results.size());
        for (FileScanner.Result i : results) {
            assertEquals(i.getUnheaderedSize(), i.getSize());
            String filename = getFilename(i);
            Pair<Long, String> crc32 = crc32sums.get(filename);
            assertNotNull(crc32);
            assertEquals((long) crc32.getLeft(), i.getSize());
            assertEquals(crc32.getRight(), i.getDigest().getCrc());
            assertEquals(md5sums.get(filename), i.getDigest().getMd5());
            assertEquals(sha1sums.get(filename), i.getDigest().getSha1());
        }
    }

    private Datafile buildDatafile(long minSize, long maxSize) {
        return Datafile.builder().games(ImmutableList.of(
                Game.builder()
                        .name("Test game 1")
                        .description("Test game 1")
                        .roms(ImmutableList.of(Rom.builder()
                                .name("Test rom 1.ext")
                                .size(minSize)
                                .build()))
                        .build(),
                Game.builder()
                        .name("Test game 2")
                        .description("Test game 2")
                        .roms(ImmutableList.of(Rom.builder()
                                .name("Test rom 2.ext")
                                .size(maxSize)
                                .build()))
                        .build()))
                .build();
    }

}