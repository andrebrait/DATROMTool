package io.github.datromtool.io.copy;

import lombok.Value;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Value
public class FileTimes {

    public static final long ONE_MILLISECOND_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    @Nullable
    FileTime lastModifiedTime;
    @Nullable
    FileTime lastAccessTime;
    @Nullable
    FileTime creationTime;

    /**
     * Constructs a new FileTimes from the provided parameters. Each one is truncated to microseconds, which is the
     * highest precision correctly handled by current libraries.
     */
    public static FileTimes from(@Nullable FileTime lastModifiedTime, @Nullable FileTime lastAccessTime, @Nullable FileTime creationTime) {
        return new FileTimes(truncateToWindowsTime(lastModifiedTime), truncateToWindowsTime(lastAccessTime), truncateToWindowsTime(creationTime));
    }

    public static FileTimes from(@Nullable Date lastModifiedTime, @Nullable Date lastAccessTime, @Nullable Date creationTime) {
        return new FileTimes(fromDate(lastModifiedTime), fromDate(lastAccessTime), fromDate(creationTime));
    }

    /**
     * Constructs a new FileTimes from an existing file's properties. Each time is truncated to microseconds.
     *
     * @see FileTimes#from(FileTime, FileTime, FileTime)
     */
    public static FileTimes from(Path file) throws IOException {
        return from(Files.readAttributes(file, BasicFileAttributes.class));
    }

    /**
     * Constructs a new FileTimes from aa file's basic attributes. Each time is truncated to microseconds.
     *
     * @see FileTimes#from(FileTime, FileTime, FileTime)
     */
    public static FileTimes from(BasicFileAttributes attributes) {
        return from(attributes.lastModifiedTime(), attributes.lastAccessTime(), attributes.creationTime());
    }

    /**
     * Truncates the dates to microseconds. This is the highest granularity possible in most archive formats.
     * Windows stores dates in 100-ns increments, but most implementations that use these dates only handle
     * microseconds. We are assuming no one is going to miss these tenths of microseconds.
     */
    @Nullable
    private static FileTime truncateToWindowsTime(@Nullable FileTime fileTime) {
        return truncate(fileTime, TimeUnit.MICROSECONDS);
    }

    public FileTimes toJavaTime() {
        return truncate(TimeUnit.MILLISECONDS);
    }

    public FileTimes toUnixTimes() {
        return truncate(TimeUnit.SECONDS);
    }

    public FileTimes truncate(TimeUnit timeUnit) {
        return new FileTimes(truncate(lastModifiedTime, timeUnit), truncate(lastAccessTime, timeUnit), truncate(creationTime, timeUnit));
    }

    @Nullable
    private static FileTime truncate(@Nullable FileTime fileTime, TimeUnit timeUnit) {
        return fileTime != null ? FileTime.from(fileTime.to(timeUnit), timeUnit) : null;
    }

    private FileTimes(@Nullable FileTime lastModifiedTime, @Nullable FileTime lastAccessTime, @Nullable FileTime creationTime) {
        this.lastModifiedTime = lastModifiedTime;
        this.lastAccessTime = lastAccessTime;
        this.creationTime = creationTime;
    }

    @Nullable
    public Date getLastModifiedTimeAsDate() {
        return asDate(lastModifiedTime);
    }

    @Nullable
    public Date getLastAccessTimeAsDate() {
        return asDate(lastAccessTime);
    }

    @Nullable
    public Date getCreationTimeAsDate() {
        return asDate(creationTime);
    }

    @Nullable
    private static Date asDate(@Nullable FileTime fileTime) {
        return fileTime != null ? new Date(fileTime.toMillis()) : null;
    }

    @Nullable
    private static FileTime fromDate(@Nullable Date date) {
        return date != null ? FileTime.fromMillis(date.getTime()) : null;
    }

    public void applyTo(Path file) throws IOException {
        applyTo(Files.getFileAttributeView(file, BasicFileAttributeView.class));
    }

    public void applyTo(BasicFileAttributeView attributes) throws IOException {
        attributes.setTimes(lastModifiedTime, lastAccessTime, creationTime);
    }
}
