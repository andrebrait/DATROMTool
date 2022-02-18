package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.ConfigDependantTest;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.data.CrcKey;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.datafile.Game;
import io.github.datromtool.domain.datafile.Rom;
import io.github.datromtool.util.ArchiveUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.datromtool.util.TestUtils.getFilename;
import static io.github.datromtool.util.TestUtils.isRar5;
import static org.junit.jupiter.api.Assertions.*;

class FileScannerTest extends ConfigDependantTest {

    private static Map<String, CrcKey> crc32sums;
    private static Map<String, String> md5sums;
    private static Map<String, String> sha1sums;

    @BeforeAll
    static void setup() throws IOException {
        crc32sums = Files.readAllLines(testDataSource.getParent().resolve("CRC32SUMS")).stream()
                .map(s -> s.split("\\s+"))
                .peek(s -> s[2] = Paths.get(s[2]).getFileName().toString())
                .collect(Collectors.toMap(s -> s[2], s -> CrcKey.from(Long.parseLong(s[1]), s[0])));
        md5sums = Files.readAllLines(testDataSource.getParent().resolve("MD5SUMS")).stream()
                .map(s -> s.split("\\s+"))
                .peek(s -> s[1] = Paths.get(s[1]).getFileName().toString())
                .collect(Collectors.toMap(s -> s[1], s -> s[0]));
        sha1sums = Files.readAllLines(testDataSource.getParent().resolve("SHA1SUMS")).stream()
                .map(s -> s.split("\\s+"))
                .peek(s -> s[1] = Paths.get(s[1]).getFileName().toString())
                .collect(Collectors.toMap(s -> s[1], s -> s[0]));
    }

    @ParameterizedTest
    @MethodSource("rar5Toggles")
    void testScan_defaultSettings(boolean rar5Available, boolean forceUnrar, boolean forceSevenZip) {
        boolean rar5Enabled = rar5Available && !(forceUnrar && forceSevenZip);
        FileScanner fileScanner = new FileScanner(
                AppConfig.FileScannerConfig.builder().forceUnrar(forceUnrar).forceSevenZip(forceSevenZip).build(),
                ImmutableList.of(),
                ImmutableList.of(),
                ImmutableList.of());
        ImmutableList<FileScanner.Result> results =
                fileScanner.scan(ImmutableList.of(testDataSource));
        assertFalse(results.isEmpty());
        assertEquals(crc32sums.size() * (rar5Enabled ? 18 : 17), results.size());
        for (FileScanner.Result i : results) {
            assertEquals(i.getUnheaderedSize(), i.getSize());
            if (!rar5Enabled && isRar5(i)) {
                continue;
            }
            String filename = getFilename(i);
            CrcKey crc32 = crc32sums.get(filename);
            assertNotNull(crc32);
            assertEquals((long) crc32.getSize(), i.getSize());
            assertEquals(crc32.getCrc(), i.getDigest().getCrc());
            assertEquals(md5sums.get(filename), i.getDigest().getMd5());
            assertEquals(sha1sums.get(filename), i.getDigest().getSha1());
        }
    }

    @ParameterizedTest
    @MethodSource("rar5Toggles")
    void testScan_minSizeLimit(boolean rar5Available, boolean forceUnrar, boolean forceSevenZip) {
        boolean rar5Enabled = rar5Available && !(forceUnrar && forceSevenZip);
        FileScanner fileScanner = new FileScanner(
                AppConfig.FileScannerConfig.builder().forceUnrar(forceUnrar).forceSevenZip(forceSevenZip).build(),
                ImmutableList.of(buildDatafile(64 * 1024L, 64 * 1024L * 1024L)),
                ImmutableList.of(),
                ImmutableList.of());
        ImmutableList<FileScanner.Result> results =
                fileScanner.scan(ImmutableList.of(testDataSource));
        assertFalse(results.isEmpty());
        assertEquals((crc32sums.size() - 4) * (rar5Enabled ? 18 : 17), results.size());
        for (FileScanner.Result i : results) {
            assertEquals(i.getUnheaderedSize(), i.getSize());
            if (!rar5Enabled && isRar5(i)) {
                continue;
            }
            String filename = getFilename(i);
            CrcKey crc32 = crc32sums.get(filename);
            assertNotNull(crc32);
            assertEquals((long) crc32.getSize(), i.getSize());
            assertEquals(crc32.getCrc(), i.getDigest().getCrc());
            assertEquals(md5sums.get(filename), i.getDigest().getMd5());
            assertEquals(sha1sums.get(filename), i.getDigest().getSha1());
        }
    }

    @ParameterizedTest
    @MethodSource("rar5Toggles")
    void testScan_maxSizeLimit(boolean rar5Available, boolean forceUnrar, boolean forceSevenZip) {
        boolean rar5Enabled = rar5Available && !(forceUnrar && forceSevenZip);
        FileScanner fileScanner = new FileScanner(
                AppConfig.FileScannerConfig.builder().forceUnrar(forceUnrar).forceSevenZip(forceSevenZip).build(),
                ImmutableList.of(buildDatafile(16 * 1024L, 768 * 1024L)),
                ImmutableList.of(),
                ImmutableList.of());
        ImmutableList<FileScanner.Result> results =
                fileScanner.scan(ImmutableList.of(testDataSource));
        assertFalse(results.isEmpty());
        assertEquals((crc32sums.size() - 4) * (rar5Enabled ? 18 : 17), results.size());
        for (FileScanner.Result i : results) {
            assertEquals(i.getUnheaderedSize(), i.getSize());
            if (!rar5Enabled && isRar5(i)) {
                continue;
            }
            String filename = getFilename(i);
            CrcKey crc32 = crc32sums.get(filename);
            assertNotNull(crc32);
            assertEquals((long) crc32.getSize(), i.getSize());
            assertEquals(crc32.getCrc(), i.getDigest().getCrc());
            assertEquals(md5sums.get(filename), i.getDigest().getMd5());
            assertEquals(sha1sums.get(filename), i.getDigest().getSha1());
        }
    }

    @ParameterizedTest
    @MethodSource("rar5Toggles")
    void testScan_minAndMaxSizeLimit(boolean rar5Available, boolean forceUnrar, boolean forceSevenZip) {
        boolean rar5Enabled = rar5Available && !(forceUnrar && forceSevenZip);
        FileScanner fileScanner = new FileScanner(
                AppConfig.FileScannerConfig.builder().forceUnrar(forceUnrar).forceSevenZip(forceSevenZip).build(),
                ImmutableList.of(buildDatafile(64 * 1024L, 768 * 1024L)),
                ImmutableList.of(),
                ImmutableList.of());
        ImmutableList<FileScanner.Result> results =
                fileScanner.scan(ImmutableList.of(testDataSource));
        assertFalse(results.isEmpty());
        assertEquals((crc32sums.size() - 8) * (rar5Enabled ? 18 : 17), results.size());
        for (FileScanner.Result i : results) {
            assertEquals(i.getUnheaderedSize(), i.getSize());
            if (!rar5Enabled && isRar5(i)) {
                continue;
            }
            String filename = getFilename(i);
            CrcKey crc32 = crc32sums.get(filename);
            assertNotNull(crc32);
            assertEquals((long) crc32.getSize(), i.getSize());
            assertEquals(crc32.getCrc(), i.getDigest().getCrc());
            assertEquals(md5sums.get(filename), i.getDigest().getMd5());
            assertEquals(sha1sums.get(filename), i.getDigest().getSha1());
        }
    }

    static Stream<Arguments> rar5Toggles() {
        boolean unrarAvailable = ArchiveUtils.isUnrarAvailable(null);
        boolean sevenZipAvailable = ArchiveUtils.isSevenZipAvailable(null);
        boolean rar5Available = unrarAvailable || sevenZipAvailable;
        Boolean[][] toggles = {
                {rar5Available, false, false},  // unrar takes precedence over 7z, whichever works first
                {rar5Available, false, true},   // forcing 7z
                {rar5Available, true, true}     // forcing both = disabling RAR5 completely
        };
        if (unrarAvailable && sevenZipAvailable) {
            return Stream.of(toggles[0], toggles[1], toggles[2]).map(Arguments::of);
        } else if (unrarAvailable || sevenZipAvailable) {
            return Stream.of(toggles[0], toggles[2]).map(Arguments::of);
        } else {
            return Stream.of(toggles[2]).map(Arguments::of);
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