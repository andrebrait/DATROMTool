package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.datromtool.Patterns;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.CRC32;

@AllArgsConstructor
public final class FileScanner {

    private static final Logger logger = LoggerFactory.getLogger(FileScanner.class);
    private static final int STREAM_BUFFER_LENGTH = 32 * 1024;

    private final Listener listener;
    private final int threads;

    // TODO add callback to report progress in scanning

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Result {

        @Value
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Digest {

            String crc;
            String md5;
            String sha1;
        }

        Path path;
        Long size;
        Digest digest;
        String archivePath;
    }

    public interface Listener {

        void reportTotalItems(int totalItems);

        void reportProgress(String label, int thread, int percentage);

        void reportFinish(String label, int thread);

        void reportAllFinish();
    }

    private final static class IndexedThreadFactory implements ThreadFactory {

        private final AtomicInteger indexCounter = new AtomicInteger();

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            return new IndexedThread(indexCounter.getAndIncrement(), r);
        }
    }

    @Getter
    private final static class IndexedThread extends Thread {

        private final int index;
        private final byte[] buffer = new byte[STREAM_BUFFER_LENGTH];
        private final CRC32 crc32 = new CRC32();
        private final MessageDigest md5 = DigestUtils.getMd5Digest();
        private final MessageDigest sha1 = DigestUtils.getSha1Digest();

        public IndexedThread(int index, Runnable target) {
            super(target);
            this.index = index;
        }

    }

    public ImmutableList<Result> scan(Path directory) {
        ExecutorService executorService = Executors.newFixedThreadPool(
                threads,
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("SCANNER-%d")
                        .setUncaughtExceptionHandler((t, e) -> logUnexpected(e))
                        .setThreadFactory(new IndexedThreadFactory())
                        .build());
        ImmutableList.Builder<Callable<ImmutableList<Result>>> builder = ImmutableList.builder();
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(
                        Path file, BasicFileAttributes attrs) throws IOException {
                    FileVisitResult result = super.visitFile(file, attrs);
                    builder.add(() -> scanFile(directory, file));
                    return result;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    logger.warn("Failed to scan '{}'", file, exc);
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });
            ImmutableList<Callable<ImmutableList<Result>>> callables = builder.build();
            if (listener != null) {
                listener.reportTotalItems(callables.size());
            }
            ImmutableList<Result> results = executorService.invokeAll(callables).stream()
                    .flatMap(f -> {
                        try {
                            return f.get().stream();
                        } catch (Exception e) {
                            logUnexpected(e);
                            return Stream.empty();
                        }
                    }).collect(ImmutableList.toImmutableList());
            executorService.shutdown();
            if (listener != null) {
                listener.reportAllFinish();
            }
            return results;
        } catch (Exception e) {
            logger.error("Could not scan '{}'", directory, e);
            return ImmutableList.of();
        }
    }

    private static void logUnexpected(Throwable e) {
        logger.error("Unexpected exception thrown", e);
    }

    private ImmutableList<Result> scanFile(Path baseDirectory, Path file) {
        // TODO make label relative to input directory
        String label = file.toString();
        Thread t = Thread.currentThread();
        int index;
        byte[] buffer;
        MessageDigest md5;
        MessageDigest sha1;
        CRC32 crc32;
        if (t instanceof IndexedThread) {
            index = ((IndexedThread) t).getIndex();
            buffer = ((IndexedThread) t).getBuffer();
            crc32 = ((IndexedThread) t).getCrc32();
            md5 = ((IndexedThread) t).getMd5();
            sha1 = ((IndexedThread) t).getSha1();
        } else {
            index = 0;
            buffer = new byte[STREAM_BUFFER_LENGTH];
            crc32 = new CRC32();
            md5 = DigestUtils.getMd5Digest();
            sha1 = DigestUtils.getSha1Digest();
        }
        if (listener != null) {
            listener.reportProgress(label, index, 0);
        }
        try {
            ImmutableList.Builder<Result> builder = ImmutableList.builder();
            if (Patterns.ZIP.matcher(file.getFileName().toString()).find()) {
                try (ZipFile zipFile = new ZipFile(file.toFile())) {
                    Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
                    while (entries.hasMoreElements()) {
                        ZipArchiveEntry entry = entries.nextElement();
                        try (InputStream entryInputStream = zipFile.getInputStream(entry)) {
                            long size = entry.getSize();
                            // TODO make label for zip entry
                            builder.add(new Result(
                                    file,
                                    size,
                                    processDigest(
                                            label,
                                            index,
                                            size,
                                            entryInputStream,
                                            buffer,
                                            crc32,
                                            md5,
                                            sha1),
                                    entry.getName()));
                        }
                    }
                }
            } else {
                try (InputStream inputStream = Files.newInputStream(file)) {
                    long size = Files.size(file);
                    builder.add(new Result(
                            file,
                            size,
                            processDigest(
                                    label,
                                    index,
                                    size,
                                    inputStream,
                                    buffer,
                                    crc32,
                                    md5,
                                    sha1),
                            null));
                }
            }
            if (listener != null) {
                listener.reportFinish(label, index);
            }
            return builder.build();
        } catch (Exception e) {
            logger.error("Could not read file '{}'", file, e);
            if (listener != null) {
                listener.reportFinish(label, index);
            }
            return ImmutableList.of();
        }
    }

    private Result.Digest processDigest(
            String label,
            int index,
            long size,
            InputStream inputStream,
            byte[] buffer,
            CRC32 crc32,
            MessageDigest md5,
            MessageDigest sha1) throws IOException {
        crc32.reset();
        md5.reset();
        sha1.reset();
        Result.Digest digest;
        long totalRead = 0;
        int read = inputStream.read(buffer, 0, buffer.length);
        while (read > -1) {
            totalRead += read;
            crc32.update(buffer, 0, read);
            md5.update(buffer, 0, read);
            sha1.update(buffer, 0, read);
            if (listener != null) {
                listener.reportProgress(label, index, (int) ((totalRead * 100) / size));
            }
            read = inputStream.read(buffer, 0, buffer.length);
        }
        digest = new Result.Digest(
                Long.toHexString(crc32.getValue()),
                Hex.encodeHexString(md5.digest()),
                Hex.encodeHexString(sha1.digest()));
        crc32.reset();
        md5.reset();
        sha1.reset();
        return digest;
    }

}
