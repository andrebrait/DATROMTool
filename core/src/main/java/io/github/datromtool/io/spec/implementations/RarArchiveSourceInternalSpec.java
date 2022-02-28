package io.github.datromtool.io.spec.implementations;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.datromtool.io.spec.AbstractArchiveSourceInternalSpec;
import io.github.datromtool.io.spec.FileTimes;
import io.github.datromtool.util.ArchiveUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class RarArchiveSourceInternalSpec extends AbstractArchiveSourceInternalSpec {

    private transient String $nameCache;

    @Getter
    @NonNull
    private final RarArchiveSourceSpec parent;
    @NonNull
    private final Archive archive;
    @NonNull
    private final FileHeader fileHeader;

    // Stateful part
    private transient InputStream inputStream;

    @Override
    public String getName() {
        if ($nameCache == null) {
            $nameCache = ArchiveUtils.normalizePath(fileHeader.getFileName());
        }
        return $nameCache;
    }

    @Override
    public long getSize() {
        return fileHeader.getFullUnpackSize();
    }

    @Override
    public FileTimes getFileTimes() {
        // atime and ctime will always be null in the current implementation of JUnrar
        return FileTimes.from(fileHeader.getMTime(), fileHeader.getATime(), fileHeader.getCTime());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            inputStream = RarArchiveExtractorService.getInputStream(archive, fileHeader);
        }
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }

    /**
     * This allows us to
     */
    @Slf4j
    private final static class RarArchiveExtractorService {
        private final static ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                .setNameFormat("RAR-EXTRACTOR-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler((t, e) -> log.error("Uncaught error", e))
                .build());

        /**
         * The overhead of {@link Archive#getInputStream(FileHeader)} with the creation of an extra thread for each rar file
         * was too much. Using a cached executor here. The cached nature of it will likely limit the number of threads
         * to the number of I/O threads we are currently using.
         */
        public static InputStream getInputStream(Archive archive, FileHeader hd) throws IOException {
            PipedInputStream in = new PipedInputStream(32 * 1024);
            PipedOutputStream out = new PipedOutputStream(in);

            RarArchiveExtractorService.executor.submit(() -> {
                // We MUST NOT use try with resources here because then the pipe will fail!
                try {
                    archive.extractFile(hd, out);
                } catch (RarException e) {
                    log.error("Could not read '{}'", hd.getFileName(), e);
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        log.error("Could not close output stream while reading '{}'", hd.getFileName(), e);
                    }
                }
            });

            return in;
        }
    }


}
