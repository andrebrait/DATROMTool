package io.github.datromtool.io.copy.archive.impl;

import io.github.datromtool.io.copy.FileTimes;
import io.github.datromtool.io.copy.SourceSpec;
import io.github.datromtool.io.copy.archive.AbstractArchiveDestinationSpec;
import io.github.datromtool.io.copy.archive.ArchiveDestinationInternalSpec;
import org.apache.commons.compress.archivers.zip.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

public final class ZipArchiveDestinationSpec extends AbstractArchiveDestinationSpec {

    // Stateful part
    private transient ZipArchiveOutputStream zipArchiveOutputStream;

    public ZipArchiveDestinationSpec(@Nonnull Path path) {
        super(path);
    }

    @Override
    public ArchiveDestinationInternalSpec createInternalDestinationSpecFor(String name, SourceSpec sourceSpec) throws IOException {
        if (zipArchiveOutputStream == null) {
            zipArchiveOutputStream = new ZipArchiveOutputStream(getPath());
        }
        ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(name);
        FileTimes sourceFileTimes = sourceSpec.getFileTimes();
        zipArchiveEntry.setSize(sourceSpec.getSize());
        setTimes(sourceFileTimes, zipArchiveEntry);
        zipArchiveOutputStream.putArchiveEntry(zipArchiveEntry);
        return new ZipArchiveDestinationInternalSpec(this, zipArchiveOutputStream, zipArchiveEntry);
    }

    @Override
    public void close() throws IOException {
        if (zipArchiveOutputStream != null) {
            zipArchiveOutputStream.close();
            zipArchiveOutputStream = null;
        }
    }

    private static void setTimes(FileTimes fileTimes, ZipArchiveEntry entry) {
        // These do not seem to have any effect on Apache ZipArchiveEntry, but setting it just in case
        if (fileTimes.getLastModifiedTime() !=null) {
            entry.setLastModifiedTime(fileTimes.getLastModifiedTime());
        }
        if (fileTimes.getLastAccessTime() != null) {
            entry.setLastAccessTime(fileTimes.getLastAccessTime());
        }
        if (fileTimes.getCreationTime() != null) {
            entry.setCreationTime(fileTimes.getCreationTime());
        }
        // The implementation does not seem to handle the extra fields correctly
        // We need to fill and add them manually
        // This is the same logic used by java.util.zip.ZipEntry
        if (exceedsUnixTime(fileTimes)) {
            addNTFSTimestamp(fileTimes, entry);
        } else {
            addExtendedTimestamp(fileTimes, entry);
        }
    }

    private static void addNTFSTimestamp(FileTimes fileTimes, ZipArchiveEntry entry) {
        X000A_NTFS timestamp = new X000A_NTFS();
        timestamp.setModifyTime(toWindowsTime(fileTimes.getLastModifiedTime()));
        timestamp.setAccessTime(toWindowsTime(fileTimes.getLastAccessTime()));
        timestamp.setCreateTime(toWindowsTime(fileTimes.getCreationTime()));
        entry.addExtraField(timestamp);
    }

    private static void addExtendedTimestamp(FileTimes fileTimes, ZipArchiveEntry entry) {
        X5455_ExtendedTimestamp timestamp = new X5455_ExtendedTimestamp();
        timestamp.setModifyTime(toUnixTime(fileTimes.getLastModifiedTime()));
        timestamp.setAccessTime(toUnixTime(fileTimes.getLastAccessTime()));
        timestamp.setCreateTime(toUnixTime(fileTimes.getCreationTime()));
        entry.addExtraField(timestamp);
    }

    private static boolean exceedsUnixTime(FileTimes fileTimes) {
        return exceedsUnixTime(fileTimes.getLastModifiedTime())
                || exceedsUnixTime(fileTimes.getLastAccessTime())
                || exceedsUnixTime(fileTimes.getCreationTime());
    }

    private static boolean exceedsUnixTime(@Nullable FileTime fileTime) {
        return fileTime != null && fileTimeToUnixTime(fileTime) > UPPER_UNIXTIME_BOUND;
    }

    @Nullable
    private static ZipEightByteInteger toWindowsTime(@Nullable FileTime fileTime) {
        return fileTime != null ? new ZipEightByteInteger(fileTimeToWinTime(fileTime)) : null;
    }

    @Nullable
    private static ZipLong toUnixTime(@Nullable FileTime fileTime) {
        return fileTime != null ? new ZipLong(fileTimeToUnixTime(fileTime)) : null;
    }

    /*
     * The section below was taken from java.util.zip.ZipUtils
     */

    // used to adjust values between Windows and java epoch
    private static final long WINDOWS_EPOCH_IN_MICROSECONDS = -11644473600000000L;

    /**
     * The upper bound of the 32-bit unix time, the "year 2038 problem".
     */
    private static final long UPPER_UNIXTIME_BOUND = 0x7fffffff;

    /**
     * Converts FileTime to Windows time.
     */
    private static long fileTimeToWinTime(FileTime fileTime) {
        return (fileTime.to(TimeUnit.MICROSECONDS) - WINDOWS_EPOCH_IN_MICROSECONDS) * 10;
    }

    /**
     * Converts FileTime to "standard Unix time".
     */
    private static long fileTimeToUnixTime(FileTime fileTime) {
        return fileTime.to(TimeUnit.SECONDS);
    }

}
