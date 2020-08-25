package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.Pair;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.datafile.Game;
import io.github.datromtool.domain.datafile.Rom;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.datromtool.util.TestUtils.getFilename;
import static io.github.datromtool.util.TestUtils.isRar5;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

import static java.util.Objects.requireNonNull;

public class FileScannerTest {

    private static final String TEST_DATA_FOLDER = "../test-data";
    private Path testDataSource;

    private Map<String, Pair<Long, String>> crc32sums;
    private Map<String, String> md5sums;
    private Map<String, String> sha1sums;

    @BeforeMethod
    public void setup() throws IOException {
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
    public void testScan_defaultSettings() throws Exception {
        FileScanner fileScanner = new FileScanner(AppConfig.builder().build(), null, null, null);
        ImmutableList<FileScanner.Result> results = fileScanner.scan(testDataSource);
        assertFalse(results.isEmpty());
        assertEquals(results.size(), crc32sums.size() * 17);
        for (FileScanner.Result i : results) {
            assertEquals(i.getSize(), i.getUnheaderedSize());
            if (isRar5(i)) {
                continue;
            }
            String filename = getFilename(i);
            Pair<Long, String> crc32 = crc32sums.get(filename);
            assertNotNull(crc32);
            assertEquals(i.getSize(), (long) crc32.getLeft());
            assertEquals(i.getDigest().getCrc(), crc32.getRight());
            assertEquals(i.getDigest().getMd5(), md5sums.get(filename));
            assertEquals(i.getDigest().getSha1(), sha1sums.get(filename));
        }
    }

    @Test
    public void testScan_minSizeLimit() throws Exception {
        FileScanner fileScanner = new FileScanner(
                AppConfig.builder().build(),
                buildDatafile(64 * 1024L, 64 * 1024L * 1024L),
                null,
                null);
        ImmutableList<FileScanner.Result> results = fileScanner.scan(testDataSource);
        assertFalse(results.isEmpty());
        assertEquals(results.size(), (crc32sums.size() - 4) * 17);
        for (FileScanner.Result i : results) {
            assertEquals(i.getSize(), i.getUnheaderedSize());
            if (isRar5(i)) {
                continue;
            }
            String filename = getFilename(i);
            Pair<Long, String> crc32 = crc32sums.get(filename);
            assertNotNull(crc32);
            assertEquals(i.getSize(), (long) crc32.getLeft());
            assertEquals(i.getDigest().getCrc(), crc32.getRight());
            assertEquals(i.getDigest().getMd5(), md5sums.get(filename));
            assertEquals(i.getDigest().getSha1(), sha1sums.get(filename));
        }
    }

    @Test
    public void testScan_maxSizeLimit() throws Exception {
        FileScanner fileScanner = new FileScanner(
                AppConfig.builder().build(),
                buildDatafile(16 * 1024L, 768 * 1024L),
                null,
                null);
        ImmutableList<FileScanner.Result> results = fileScanner.scan(testDataSource);
        assertFalse(results.isEmpty());
        assertEquals(results.size(), (crc32sums.size() - 4) * 17);
        for (FileScanner.Result i : results) {
            assertEquals(i.getSize(), i.getUnheaderedSize());
            if (isRar5(i)) {
                continue;
            }
            String filename = getFilename(i);
            Pair<Long, String> crc32 = crc32sums.get(filename);
            assertNotNull(crc32);
            assertEquals(i.getSize(), (long) crc32.getLeft());
            assertEquals(i.getDigest().getCrc(), crc32.getRight());
            assertEquals(i.getDigest().getMd5(), md5sums.get(filename));
            assertEquals(i.getDigest().getSha1(), sha1sums.get(filename));
        }
    }

    @Test
    public void testScan_minAndmaxSizeLimit() throws Exception {
        FileScanner fileScanner = new FileScanner(
                AppConfig.builder().build(),
                buildDatafile(64 * 1024L, 768 * 1024L),
                null,
                null);
        ImmutableList<FileScanner.Result> results = fileScanner.scan(testDataSource);
        assertFalse(results.isEmpty());
        assertEquals(results.size(), (crc32sums.size() - 8) * 17);
        for (FileScanner.Result i : results) {
            assertEquals(i.getSize(), i.getUnheaderedSize());
            if (isRar5(i)) {
                continue;
            }
            String filename = getFilename(i);
            Pair<Long, String> crc32 = crc32sums.get(filename);
            assertNotNull(crc32);
            assertEquals(i.getSize(), (long) crc32.getLeft());
            assertEquals(i.getDigest().getCrc(), crc32.getRight());
            assertEquals(i.getDigest().getMd5(), md5sums.get(filename));
            assertEquals(i.getDigest().getSha1(), sha1sums.get(filename));
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