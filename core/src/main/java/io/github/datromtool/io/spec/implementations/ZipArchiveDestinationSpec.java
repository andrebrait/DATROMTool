package io.github.datromtool.io.spec.implementations;

import io.github.datromtool.SerializationHelper;
import io.github.datromtool.io.ArchiveType;
import io.github.datromtool.io.spec.ArchiveDestinationInternalSpec;
import io.github.datromtool.io.spec.ArchiveDestinationSpec;
import io.github.datromtool.io.spec.FileTimes;
import io.github.datromtool.io.spec.SourceSpec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.zip.X000A_NTFS;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipEightByteInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZipArchiveDestinationSpec implements ArchiveDestinationSpec {

    @Getter
    private final Path path;

    @Nonnull
    public static ZipArchiveDestinationSpec of(@Nonnull Path path) {
        return new ZipArchiveDestinationSpec(path.toAbsolutePath().normalize());
    }

    // Stateful part
    private transient ZipArchiveOutputStream zipArchiveOutputStream;

    @Override
    public ArchiveType getType() {
        return ArchiveType.ZIP;
    }

    @Override
    public ArchiveDestinationInternalSpec createInternalDestinationSpecFor(String name, SourceSpec sourceSpec) throws IOException {
        if (zipArchiveOutputStream == null) {
            zipArchiveOutputStream = new ZipArchiveOutputStream(path);
        }
        ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(name);
        FileTimes sourceFileTimes = sourceSpec.getFileTimes();
        if (sourceFileTimes.getLastModifiedTime() != null) {
            zipArchiveEntry.setTime(sourceFileTimes.getLastModifiedTime());
        }
        zipArchiveEntry.setSize(sourceSpec.getSize());
        setZipExtraProperties(sourceFileTimes, zipArchiveEntry);
        zipArchiveOutputStream.putArchiveEntry(zipArchiveEntry);
        return new ZipArchiveDestinationInternalSpec(this, zipArchiveOutputStream, zipArchiveEntry);
    }

    @Override
    public void close() throws IOException {
        if (zipArchiveOutputStream != null) {
            zipArchiveOutputStream.close();
        }
    }

    private static void setZipExtraProperties(FileTimes fileTimes, ZipArchiveEntry entry) {
        boolean requireExtraTimestamp = exceedsUnixTime(fileTimes.getLastModifiedTime()) || fileTimes.getLastAccessTime() != null || fileTimes.getCreationTime() != null;
        if (requireExtraTimestamp) {
            // This doesn't seem to have any effect on Apache's ZipArchiveEntry, but setting it just in case
            if (fileTimes.getLastModifiedTime() != null) {
                entry.setLastModifiedTime(fileTimes.getLastModifiedTime());
            }
            if (fileTimes.getLastAccessTime() != null) {
                entry.setLastAccessTime(fileTimes.getLastAccessTime());
            }
            if (fileTimes.getCreationTime() != null) {
                entry.setCreationTime(fileTimes.getCreationTime());
            }
            // The implementation does not seem to handle the X000A_NTFS extra fields
            // We need to add them manually
            addNTFSTimestamp(fileTimes, entry);
        }
        entry.setComment(format("Compressed using DATROMTool v%s", SerializationHelper.getInstance().getVersionString()));
    }

    private static void addNTFSTimestamp(FileTimes fileTimes, ZipArchiveEntry entry) {
        X000A_NTFS timestamp = new X000A_NTFS();
        timestamp.setModifyTime(fileTimeToWinTime(fileTimes.getLastModifiedTime()));
        timestamp.setAccessTime(fileTimeToWinTime(fileTimes.getLastAccessTime()));
        timestamp.setCreateTime(fileTimeToWinTime(fileTimes.getCreationTime()));
        entry.addExtraField(timestamp);
    }

    private static boolean exceedsUnixTime(@Nullable FileTime fileTime) {
        return fileTime != null && fileTime.toMillis() > UPPER_UNIXTIME_BOUND;
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
    @Nullable
    private static ZipEightByteInteger fileTimeToWinTime(@Nullable FileTime fileTime) {
        if (fileTime == null) {
            return null;
        }
        return new ZipEightByteInteger((fileTime.to(TimeUnit.MICROSECONDS) - WINDOWS_EPOCH_IN_MICROSECONDS) * 10);
    }
}
