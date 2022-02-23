package io.github.datromtool.io.spec;

import lombok.Builder;
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
@Builder(toBuilder = true)
public class FileTimes {

    @Nullable
    FileTime lastModifiedTime;
    @Nullable
    FileTime lastAccessTime;
    @Nullable
    FileTime creationTime;

    public static FileTimes from(@Nullable FileTime lastModifiedTime, @Nullable FileTime lastAccessTime, @Nullable FileTime creationTime) {
        return new FileTimes(truncate(lastModifiedTime), truncate(lastAccessTime), truncate(creationTime));
    }

    @Nullable
    private static FileTime truncate(@Nullable FileTime lastModifiedTime) {
        return lastModifiedTime != null
                ? FileTime.from(lastModifiedTime.to(TimeUnit.MICROSECONDS), TimeUnit.MICROSECONDS)
                : null;
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

    private static Date asDate(@Nullable FileTime fileTime) {
        return fileTime != null ? new Date(fileTime.toMillis()) : null;
    }

    public static FileTimes from(Path file) throws IOException {
        return from(Files.readAttributes(file, BasicFileAttributes.class));
    }

    public static FileTimes from(BasicFileAttributes attributes) {
        return from(attributes.lastModifiedTime(), attributes.lastAccessTime(), attributes.creationTime());
    }

    public void applyTo(Path file) throws IOException {
        applyTo(Files.getFileAttributeView(file, BasicFileAttributeView.class));
    }

    public void applyTo(BasicFileAttributeView attributes) throws IOException {
        attributes.setTimes(lastModifiedTime, lastAccessTime, creationTime);
    }
}
