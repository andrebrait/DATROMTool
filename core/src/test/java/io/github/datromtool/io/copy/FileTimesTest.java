package io.github.datromtool.io.copy;

import org.junit.jupiter.api.Test;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileTimesTest {

    protected static final FileTimes TEST_TIMES = FileTimes.from(
            FileTime.from(Instant.parse("2022-02-23T09:24:19.191543Z")),
            FileTime.from(Instant.parse("2022-02-23T09:28:27.781976Z")),
            FileTime.from(Instant.parse("2022-02-23T09:17:37.409043Z")));

    @Test
    void testFrom_TruncateToMicroseconds() {
        FileTimes fromNanos = FileTimes.from(
                FileTime.from(Instant.parse("2022-02-23T09:24:19.191543123Z")),
                FileTime.from(Instant.parse("2022-02-23T09:28:27.781976123Z")),
                FileTime.from(Instant.parse("2022-02-23T09:17:37.409043123Z")));
        assertEquals(TEST_TIMES, fromNanos);
    }

    @Test
    void testToJavaTime() {
        FileTimes fromMillis = FileTimes.from(
                FileTime.from(Instant.parse("2022-02-23T09:24:19.191Z")),
                FileTime.from(Instant.parse("2022-02-23T09:28:27.781Z")),
                FileTime.from(Instant.parse("2022-02-23T09:17:37.409Z")));
        assertEquals(fromMillis, TEST_TIMES.toJavaTime());
    }

    @Test
    void testToUnixTimes() {
        FileTimes fromSeconds = FileTimes.from(
                FileTime.from(Instant.parse("2022-02-23T09:24:19Z")),
                FileTime.from(Instant.parse("2022-02-23T09:28:27Z")),
                FileTime.from(Instant.parse("2022-02-23T09:17:37Z")));
        assertEquals(fromSeconds, TEST_TIMES.toUnixTimes());
    }

    @Test
    void testGetLastModifiedTimeAsDate() {
        assertEquals(Date.from(Instant.parse("2022-02-23T09:24:19.191Z")), TEST_TIMES.getLastModifiedTimeAsDate());
    }

    @Test
    void testGetLastAccessTimeAsDate() {
        assertEquals(Date.from(Instant.parse("2022-02-23T09:28:27.781Z")), TEST_TIMES.getLastAccessTimeAsDate());
    }

    @Test
    void testGetCreationTimeAsDate() {
        assertEquals(Date.from(Instant.parse("2022-02-23T09:17:37.409Z")), TEST_TIMES.getCreationTimeAsDate());
    }
}