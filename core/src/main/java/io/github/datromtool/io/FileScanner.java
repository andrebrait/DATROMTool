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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.CRC32;

@AllArgsConstructor
public final class FileScanner {

    private static final Logger logger = LoggerFactory.getLogger(FileScanner.class);
    private static final Comparator<Pair<Path, Long>> fileComparator =
            Comparator.comparingLong(p -> -p.getRight());

    private static final int BUFFER_SIZE = 32 * 1024; // 32KB per thread

    private final Listener listener;
    private final int threads;

    // TODO handle scanning archives as ROMs

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

        void reportStart(String label, int thread);

        void reportProgress(String label, int thread, int percentage, long speed);

        void reportFinish(String label, int thread);

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
        private final byte[] buffer = new byte[BUFFER_SIZE];
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
        Thread hook = new Thread(executorService::shutdownNow);
        Runtime.getRuntime().addShutdownHook(hook);
        try {
            List<Pair<Path, Long>> paths = new ArrayList<>();
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(
                        Path file, BasicFileAttributes attrs) throws IOException {
                    paths.add(ImmutablePair.of(file, Files.size(file)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    logger.warn("Failed to scan '{}'", file, exc);
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });
            if (listener != null) {
                listener.reportTotalItems(paths.size());
            }
            ImmutableList<Future<ImmutableList<Result>>> futures = paths.stream()
                    .sorted(fileComparator)
                    .map(Pair::getLeft)
                    .map(p -> executorService.submit(() -> scanFile(directory, p)))
                    .collect(ImmutableList.toImmutableList());
            ImmutableList<Result> results = futures.stream()
                    .flatMap(f -> {
                        try {
                            return f.get().stream();
                        } catch (Exception e) {
                            logUnexpected(e);
                            return Stream.empty();
                        }
                    }).collect(ImmutableList.toImmutableList());
            executorService.shutdown();
            Runtime.getRuntime().removeShutdownHook(hook);
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
        Path relative = baseDirectory.relativize(file);
        String label = relative.toString();
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
            buffer = new byte[BUFFER_SIZE];
            crc32 = new CRC32();
            md5 = DigestUtils.getMd5Digest();
            sha1 = DigestUtils.getSha1Digest();
        }
        if (listener != null) {
            listener.reportStart(label, index);
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
                            String name = entry.getName();
                            String entryLabel = relative.resolve(name).toString();
                            builder.add(new Result(
                                    file,
                                    size,
                                    processDigest(
                                            entryLabel,
                                            index,
                                            size,
                                            entryInputStream,
                                            buffer,
                                            crc32,
                                            md5,
                                            sha1),
                                    name));
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
        int reportedPercentage = 0;
        long totalRead = 0;
        long start = System.nanoTime();
        int bytesRead = inputStream.read(buffer, 0, buffer.length);
        while (bytesRead > -1) {
            crc32.update(buffer, 0, bytesRead);
            md5.update(buffer, 0, bytesRead);
            sha1.update(buffer, 0, bytesRead);
            if (listener != null) {
                totalRead += bytesRead;
                int percentage = (int) ((totalRead * 100d) / size);
                if (reportedPercentage != percentage) {
                    double secondsPassed = (System.nanoTime() - start) / 1E9d;
                    long bytesPerSecond = Math.round(bytesRead / secondsPassed);
                    listener.reportProgress(label, index, percentage, bytesPerSecond);
                    reportedPercentage = percentage;
                }
                start = System.nanoTime();
            }
            bytesRead = inputStream.read(buffer, 0, buffer.length);
        }
        Result.Digest digest = new Result.Digest(
                Long.toHexString(crc32.getValue()),
                Hex.encodeHexString(md5.digest()),
                Hex.encodeHexString(sha1.digest()));
        crc32.reset();
        md5.reset();
        sha1.reset();
        return digest;
    }

}
