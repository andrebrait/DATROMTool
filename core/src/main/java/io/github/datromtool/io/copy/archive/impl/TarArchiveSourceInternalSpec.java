package io.github.datromtool.io.copy.archive.impl;

import io.github.datromtool.io.copy.FileTimes;
import io.github.datromtool.io.copy.archive.AbstractArchiveSourceInternalSpec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.BoundedInputStream;

import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class TarArchiveSourceInternalSpec extends AbstractArchiveSourceInternalSpec {

    @NonNull
    @Getter
    private final TarArchiveSourceSpec parent;
    @NonNull
    private final TarArchiveInputStream tarArchiveInputStream;
    @NonNull
    private final TarArchiveEntry tarArchiveEntry;

    // Stateful part
    private transient InputStream inputStream;

    @Override
    public String getName() {
        return tarArchiveEntry.getName();
    }

    @Override
    public long getSize() {
        return tarArchiveEntry.getRealSize();
    }

    @Override
    public FileTimes getFileTimes() {
        // TODO handle ctime and atime
//        Map<String, String> extraHeaders = tarArchiveEntry.getExtraPaxHeaders();
//        if (extraHeaders != null && !extraHeaders.isEmpty()) {
//            String mtime = extraHeaders.get("mtime");
//            String atime = extraHeaders.get("atime");
//            String ctime = extraHeaders.get("ctime");
//            if (mtime != null || atime != null || ctime != null) {
//                return FileTimes.from(
//                        fromEpochSeconds(mtime, tarArchiveEntry.getLastModifiedDate()),
//                        fromEpochSeconds(atime, null),
//                        fromEpochSeconds(ctime, null));
//            }
//        }
        return FileTimes.from(tarArchiveEntry.getLastModifiedDate(), null, null);
    }

//    @Nullable
//    private static FileTime fromEpochSeconds(@Nullable String seconds, @Nullable Date defaultDate) {
//        if (seconds == null || seconds.isEmpty()) {
//            return defaultDate != null ? FileTime.fromMillis(defaultDate.getTime()) : null;
//        }
//        BigDecimal epochSeconds = new BigDecimal(seconds);
//        return FileTime.from(Instant.ofEpochSecond(epochSeconds.longValue(), epochSeconds.remainder(BigDecimal.ONE).movePointRight(9).longValue()));
//    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            inputStream = new BoundedInputStream(tarArchiveInputStream, tarArchiveEntry.getRealSize());
        }
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        // No need to close this InputStream
        inputStream = null;
    }
}
