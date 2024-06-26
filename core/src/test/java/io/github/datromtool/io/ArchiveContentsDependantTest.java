package io.github.datromtool.io;

import io.github.datromtool.TestDirDependantTest;
import io.github.datromtool.io.copy.FileTimes;
import io.github.datromtool.io.copy.SourceSpec;
import io.github.datromtool.io.copy.archive.ArchiveSourceInternalSpec;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static java.util.Calendar.FEBRUARY;
import static java.util.Calendar.MARCH;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class ArchiveContentsDependantTest extends TestDirDependantTest {

    public enum DateField {
        MTIME, CTIME, ATIME
    }

    protected static final String LOREM_IPSUM_FILE = "files/test/lorem-ipsum.txt";
    protected static final String SHORT_TEXT_FILE = "files/test/short-text.txt";
    protected static final FileTimes SHORT_TEXT_TIMES = FileTimes.from(
            FileTime.from(Instant.parse("2022-02-23T09:24:19.191543300Z")),
            FileTime.from(Instant.parse("2022-03-02T17:45:18.694091100Z")),
            FileTime.from(Instant.parse("2022-02-23T09:34:59.759754700Z")));
    protected static final FileTimes LOREM_IPSUM_TIMES = FileTimes.from(
            FileTime.from(Instant.parse("2022-02-23T09:25:55.206205200Z")),
            FileTime.from(Instant.parse("2022-03-02T17:45:18.695090800Z")),
            FileTime.from(Instant.parse("2022-02-23T09:19:22.854708200Z")));

    /**
     * Corrected for the file's original time zone
     */
    @Nonnull
    private static FileTimes getShortTextDosTimes() {
        return FileTimes.from(
                toFileTime(2022, FEBRUARY, 23, 10, 24, 19, 191, 543300),
                toFileTime(2022, MARCH, 2, 18, 45, 18, 694, 91100),
                toFileTime(2022, FEBRUARY, 23, 10, 34, 59, 759, 754700));
    }

    /**
     * Corrected for the file's original time zone
     */
    @Nonnull
    private static FileTimes getLoremIpsumDosTimes() {
        return FileTimes.from(
                toFileTime(2022, FEBRUARY, 23, 10, 25, 55, 206, 205200),
                toFileTime(2022, MARCH, 2, 18, 45, 18, 695, 90800),
                toFileTime(2022, FEBRUARY, 23, 10, 19, 22, 854, 708200));
    }

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
        assertIsLoremIpsum(internalSpec, false);
    }

    protected static void assertIsLoremIpsum(ArchiveSourceInternalSpec internalSpec, boolean convertTimeZone, DateField... dateFields) throws IOException {
        assertNotNull(internalSpec);
        assertEquals(LOREM_IPSUM_FILE, internalSpec.getName());
        assertEquals(loremIpsumContents.length, internalSpec.getSize());
        if (convertTimeZone) {
            lenientAssertEquals(getLoremIpsumDosTimes(), internalSpec.getFileTimes(), dateFields);
        } else {
            lenientAssertEquals(LOREM_IPSUM_TIMES, internalSpec.getFileTimes(), dateFields);
        }
        assertIsLoremIpsumContents(internalSpec);
    }

    protected static void assertIsShortText(ArchiveSourceInternalSpec internalSpec) throws IOException {
        assertIsShortText(internalSpec, false);
    }

    private static FileTime toFileTime(int year, int month, int day, int hour, int minute, int second, int millis, int nanos) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, millis);
        return FileTime.from(Instant.ofEpochMilli(calendar.getTimeInMillis()).plus(nanos, ChronoUnit.NANOS));
    }

    protected static void assertIsShortText(ArchiveSourceInternalSpec internalSpec, boolean convertTimeZone, DateField... dateFields) throws IOException {
        assertNotNull(internalSpec);
        assertEquals(SHORT_TEXT_FILE, internalSpec.getName());
        assertEquals(shortTextContents.length, internalSpec.getSize());
        if (convertTimeZone) {
            lenientAssertEquals(getShortTextDosTimes(), internalSpec.getFileTimes(), dateFields);
        } else {
            lenientAssertEquals(SHORT_TEXT_TIMES, internalSpec.getFileTimes(), dateFields);
        }
        assertIsShortTextContents(internalSpec);
    }

    protected static void assertIsLocalShortText(ArchiveSourceInternalSpec spec, DateField... dateFields) throws IOException {
        assertNotNull(spec);
        assertEquals(SHORT_TEXT_FILE, spec.getName());
        assertEquals(shortTextContents.length, spec.getSize());
        lenientAssertEquals(shortTextLocalTimes, spec.getFileTimes(), dateFields);
        assertIsShortTextContents(spec);
    }

    protected static void assertIsLocalLoremIpsum(ArchiveSourceInternalSpec spec, DateField... dateFields) throws IOException {
        assertNotNull(spec);
        assertEquals(LOREM_IPSUM_FILE, spec.getName());
        assertEquals(loremIpsumContents.length, spec.getSize());
        lenientAssertEquals(loremIpsumLocalTimes, spec.getFileTimes(), dateFields);
        assertIsLoremIpsumContents(spec);
    }

    private static void lenientAssertEquals(FileTimes expected, FileTimes actual, DateField... dateFields) {
        if (isMinutesPrecision(actual)) {
            expected = expected.truncate(TimeUnit.MINUTES);
            actual = actual.truncate(TimeUnit.MINUTES);
        } else if (isSecondsPrecision(actual)) {
            expected = expected.toUnixTimes();
            actual = actual.toUnixTimes();
        } else if (isMillisPrecision(actual)) {
            expected = expected.toJavaTime();
            actual = actual.toJavaTime();
        }
        if (dateFields.length == 0) {
            dateFields = DateField.values();
        }
        for (DateField field : dateFields) {
            switch (field) {
                case MTIME -> {
                    // workaround for missing mtime PAX header in Tar
                    FileTime truncated = truncate(actual.getLastModifiedTime(), TimeUnit.SECONDS);
                    if (truncated != null && truncated.equals(actual.getLastModifiedTime())) {
                        assertEquals(truncate(expected.getLastModifiedTime(), TimeUnit.SECONDS), truncated);
                    } else {
                        assertEquals(expected.getLastModifiedTime(), actual.getLastModifiedTime());
                    }
                }
                case ATIME -> assertEquals(expected.getLastAccessTime(), actual.getLastAccessTime());
                case CTIME -> assertEquals(expected.getCreationTime(), actual.getCreationTime());
            }
        }
    }

    @Nullable
    private static FileTime truncate(@Nullable FileTime fileTime, TimeUnit timeUnit) {
        return fileTime != null ? FileTime.from(fileTime.to(timeUnit), timeUnit) : null;
    }

    private static boolean isSecondsPrecision(FileTimes actual) {
        return actual.equals(actual.toUnixTimes());
    }

    private static boolean isMinutesPrecision(FileTimes actual) {
        return actual.equals(actual.truncate(TimeUnit.MINUTES));
    }

    private static boolean isMillisPrecision(FileTimes actual) {
        return actual.equals(actual.toJavaTime());
    }
}
