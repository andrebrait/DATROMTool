package io.github.datromtool.io;

import io.github.datromtool.TestDirDependantTest;
import io.github.datromtool.io.spec.ArchiveSourceInternalSpec;
import io.github.datromtool.io.spec.FileTimes;
import io.github.datromtool.io.spec.SourceSpec;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.BeforeAll;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public abstract class ArchiveContentsDependantTest extends TestDirDependantTest {

    protected static final String LOREM_IPSUM_FILE = "files/test/lorem-ipsum.txt";
    protected static final String SHORT_TEXT_FILE = "files/test/short-text.txt";
    protected static final FileTimes SHORT_TEXT_TIMES = FileTimes.from(
            FileTime.from(Instant.parse("2022-02-23T09:24:19.191543Z")),
            FileTime.from(Instant.parse("2022-02-23T09:28:27.781976Z")),
            FileTime.from(Instant.parse("2022-02-23T09:17:37.409043Z")));
    protected static final FileTimes LOREM_IPSUM_TIMES = FileTimes.from(
            FileTime.from(Instant.parse("2022-02-23T09:25:55.206205Z")),
            FileTime.from(Instant.parse("2022-02-23T09:28:27.781976Z")),
            FileTime.from(Instant.parse("2022-02-23T09:19:22.854708Z")));

    protected static byte[] shortTextContents;
    protected static byte[] loremIpsumContents;
    protected static FileTimes shortTextLocalTimes;
    protected static FileTimes loremIpsumLocalTimes;

    @BeforeAll
    static void readTextFiles() throws IOException {
        Path baseDir = archiveTestDataSource.resolve("files").resolve("test");
        Path shortTextPath = baseDir.resolve("short-text.txt");
        Path loremIpsumPath = baseDir.resolve("lorem-ipsum.txt");
        shortTextContents = Files.readAllBytes(shortTextPath);
        loremIpsumContents = Files.readAllBytes(loremIpsumPath);
        assertEquals(22, shortTextContents.length);
        assertEquals(64420, loremIpsumContents.length);
        shortTextLocalTimes = FileTimes.from(shortTextPath);
        loremIpsumLocalTimes = FileTimes.from(loremIpsumPath);
    }
    
    protected static void assertIsLoremIpsumContents(@Nullable SourceSpec sourceSpec) throws IOException {
        assertNotNull(sourceSpec);
        try (InputStream is = sourceSpec.getInputStream()) {
            assertIsLoremIpsumContents(is);
        }
    }

    protected static void assertIsShortTextContents(@Nullable SourceSpec sourceSpec) throws IOException {
        assertNotNull(sourceSpec);
        try (InputStream is = sourceSpec.getInputStream()) {
            assertIsShortTextContents(is);
        }
    }

    protected static void assertIsLoremIpsumContents(InputStream is) throws IOException {
        assertArrayEquals(loremIpsumContents, IOUtils.toByteArray(is));
    }

    protected static void assertIsShortTextContents(InputStream is) throws IOException {
        assertArrayEquals(shortTextContents, IOUtils.toByteArray(is));
    }

    protected static void assertIsLoremIpsum(ArchiveSourceInternalSpec internalSpec) throws IOException {
        assertIsLoremIpsum(internalSpec, false, false);
    }

    protected static void assertIsLoremIpsum(ArchiveSourceInternalSpec internalSpec, boolean onlyModificationTime, boolean ignoreDifferencesSmallerThanOneUnit) throws IOException {
        assertNotNull(internalSpec);
        assertEquals(LOREM_IPSUM_FILE, internalSpec.getName());
        assertEquals(loremIpsumContents.length, internalSpec.getSize());
        lenientAssertEquals(LOREM_IPSUM_TIMES, internalSpec.getFileTimes(), onlyModificationTime, ignoreDifferencesSmallerThanOneUnit);
        assertIsLoremIpsumContents(internalSpec);
    }

    protected static void assertIsShortText(ArchiveSourceInternalSpec internalSpec) throws IOException {
        assertIsShortText(internalSpec, false, false);
    }

    protected static void assertIsShortText(ArchiveSourceInternalSpec internalSpec, boolean onlyModificationTime, boolean ignoreDifferencesSmallerThanOneUnit) throws IOException {
        assertNotNull(internalSpec);
        assertEquals(SHORT_TEXT_FILE, internalSpec.getName());
        assertEquals(shortTextContents.length, internalSpec.getSize());
        lenientAssertEquals(SHORT_TEXT_TIMES, internalSpec.getFileTimes(), onlyModificationTime, ignoreDifferencesSmallerThanOneUnit);
        assertIsShortTextContents(internalSpec);
    }

    protected static void assertIsLocalShortText(ArchiveSourceInternalSpec internalSpec) throws IOException {
        assertIsLocalShortText(internalSpec, false, false);
    }

    protected static void assertIsLocalShortText(ArchiveSourceInternalSpec spec, boolean onlyModificationTime, boolean ignoreDifferencesSmallerThanOneUnit) throws IOException {
        assertNotNull(spec);
        assertEquals(SHORT_TEXT_FILE, spec.getName());
        assertEquals(shortTextContents.length, spec.getSize());
        lenientAssertEquals(shortTextLocalTimes, spec.getFileTimes(), onlyModificationTime, ignoreDifferencesSmallerThanOneUnit);
        assertIsShortTextContents(spec);
    }

    protected static void assertIsLocalLoremIpsum(ArchiveSourceInternalSpec internalSpec) throws IOException {
        assertIsLocalLoremIpsum(internalSpec, false, false);
    }

    protected static void assertIsLocalLoremIpsum(ArchiveSourceInternalSpec spec, boolean onlyModificationTime, boolean ignoreDifferencesSmallerThanOneUnit) throws IOException {
        assertNotNull(spec);
        assertEquals(LOREM_IPSUM_FILE, spec.getName());
        assertEquals(loremIpsumContents.length, spec.getSize());
        lenientAssertEquals(loremIpsumLocalTimes, spec.getFileTimes(), onlyModificationTime, ignoreDifferencesSmallerThanOneUnit);
        assertIsLoremIpsumContents(spec);
    }

    private static void lenientAssertEquals(FileTimes expected, FileTimes actual, boolean onlyModificationTime, boolean ignoreDifferencesSmallerThanOneUnit) {
        TimeUnit unit = TimeUnit.MICROSECONDS;
        if (isSecondsPrecision(actual)) {
            unit = TimeUnit.SECONDS;
            expected = expected.toUnixTimes();
            actual = actual.toUnixTimes();
        } else if (isMillisPrecision(actual)) {
            unit = TimeUnit.MILLISECONDS;
            expected = expected.toJavaTime();
            actual = actual.toJavaTime();
        }
        if (onlyModificationTime && ignoreDifferencesSmallerThanOneUnit) {
            assertDifferenceSmallerThanOneUnit(unit, expected.getLastModifiedTime(), actual.getLastModifiedTime());
        } else if (onlyModificationTime) {
            assertEquals(expected.getLastModifiedTime(), actual.getLastModifiedTime());
        } else if (ignoreDifferencesSmallerThanOneUnit) {
            assertDifferenceSmallerThanOneUnit(unit, expected.getLastModifiedTime(), actual.getLastModifiedTime());
            assertDifferenceSmallerThanOneUnit(unit, expected.getLastAccessTime(), actual.getLastAccessTime());
            assertDifferenceSmallerThanOneUnit(unit, expected.getCreationTime(), actual.getCreationTime());
        } else {
            assertEquals(expected, actual);
        }
    }

    private static void assertDifferenceSmallerThanOneUnit(TimeUnit unit, FileTime expectedMod, FileTime actualMod) {
        if (expectedMod != null && actualMod != null) {
            assertTrue(Math.abs(expectedMod.to(unit) - actualMod.to(unit)) <= 1);
        } else {
            assertEquals(expectedMod, actualMod);
        }
    }

    private static boolean isSecondsPrecision(FileTimes actual) {
        return actual.equals(actual.toUnixTimes());
    }

    private static boolean isMillisPrecision(FileTimes actual) {
        return actual.equals(actual.toJavaTime());
    }
}
