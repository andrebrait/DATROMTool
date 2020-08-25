package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.Pair;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

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
        assertEquals(crc32sums.size(), md5sums.size());
        assertEquals(sha1sums.size(), md5sums.size());
    }

    @Test
    public void testScan_defaultSettings() throws Exception {
        FileScanner fileScanner = new FileScanner(AppConfig.builder().build(), null, null, null);
        ImmutableList<FileScanner.Result> results = fileScanner.scan(testDataSource);
        assertFalse(results.isEmpty());
        assertEquals(results.size(), crc32sums.size() * 18);
        for (FileScanner.Result i : results) {
            assertEquals(i.getSize(), i.getUnheaderedSize());
            if (i.getArchivePath() == null
                    && i.getPath().getFileName().toString().endsWith(".txt.rar")) {
                continue;
            }
            Pair<Long, String> crc32 = crc32sums.get(
                    i.getArchivePath() != null
                            ? Paths.get(i.getArchivePath()).getFileName().toString()
                            : i.getPath().getFileName().toString());
            assertNotNull(crc32);
            assertEquals(i.getSize(), (long) crc32.getLeft());
            assertEquals(i.getDigest().getCrc(), crc32.getRight());
        }
    }
}