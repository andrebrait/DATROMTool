package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.Patterns;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.CRC32;

@AllArgsConstructor
public final class FileScanner {

    private static final Logger logger = LoggerFactory.getLogger(FileScanner.class);

    private static final int BUFFER_SIZE = 8 * 1024 * 1024; // 8MB per thread

    private final Listener listener;
    private final int numThreads;

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
        long size;
        Digest digest;
        String archivePath;
    }

    @Value
    private static class FileMetadata {

        Path baseDirectory;
        Path path;
        long size;
    }

    public interface Listener {

        void reportTotalItems(int totalItems);

        void reportStart(String label, int thread);

        void reportProgress(String label, int thread, int percentage, long speed);

        void reportFinish(String label, int thread);

    }

    @AllArgsConstructor
    private final static class AppendingFileVisitor extends SimpleFileVisitor<Path> {

        private final Path baseDirectory;
        private final Consumer<FileMetadata> onVisited;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (attrs.isRegularFile()) {
                onVisited.accept(new FileMetadata(baseDirectory, file, attrs.size()));
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            logger.warn("Failed to scan '{}'", file, exc);
            return FileVisitResult.SKIP_SUBTREE;
        }
    }

    private final static class IndexedThreadFactory implements ThreadFactory {

        private final AtomicInteger indexCounter = new AtomicInteger(1);

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            IndexedThread thread = new IndexedThread(indexCounter.getAndIncrement(), r);
            thread.setDaemon(true);
            thread.setName("SCANNER-" + thread.getIndex());
            thread.setUncaughtExceptionHandler((t, e) -> logUnexpected(e));
            return thread;
        }
    }

    @Getter
    private final static class IndexedThread extends Thread {

        private final int index;
        private final byte[] buffer = new byte[BUFFER_SIZE];
        private final CRC32 crc32 = new CRC32();
        private final MessageDigest md5 = DigestUtils.getMd5Digest();
        private final MessageDigest sha1 = DigestUtils.getSha1Digest();

        private IndexedThread(int index, Runnable target) {
            super(target);
            this.index = index;
        }

    }

    public ImmutableList<Result> scan(Path directory) {
        ExecutorService executorService = Executors.newFixedThreadPool(
                numThreads,
                new IndexedThreadFactory());
        Thread hook = new Thread(executorService::shutdownNow);
        Runtime.getRuntime().addShutdownHook(hook);
        try {
            ImmutableList.Builder<FileMetadata> pathsBuilder = ImmutableList.builder();
            Files.walkFileTree(directory, new AppendingFileVisitor(directory, pathsBuilder::add));
            ImmutableList<FileMetadata> paths = pathsBuilder.build();
            if (listener != null) {
                listener.reportTotalItems(paths.size());
            }
            ImmutableList<Result> results = paths
                    .stream()
                    .sorted(Comparator.comparingLong(fm -> -fm.getSize()))
                    .map(fm -> executorService.submit(() -> scanFile(fm)))
                    .collect(ImmutableList.toImmutableList())
                    .stream()
                    .flatMap(FileScanner::streamResults)
                    .collect(ImmutableList.toImmutableList());
            executorService.shutdown();
            return results;
        } catch (Exception e) {
            logger.error("Could not scan '{}'", directory, e);
            return ImmutableList.of();
        } finally {
            if (!executorService.isTerminated()) {
                executorService.shutdownNow();
            }
            Runtime.getRuntime().removeShutdownHook(hook);
        }
    }

    private static <T> Stream<T> streamResults(Future<ImmutableList<T>> future) {
        try {
            return future.get().stream();
        } catch (Exception e) {
            logUnexpected(e);
            return Stream.empty();
        }
    }

    private static void logUnexpected(Throwable e) {
        logger.error("Unexpected exception thrown", e);
    }

    private ImmutableList<Result> scanFile(FileMetadata fileMetadata) {
        Path file = fileMetadata.getPath();
        Path relative = fileMetadata.getBaseDirectory().relativize(file);
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
            String filename = file.getFileName().toString();
            if (Patterns.ZIP.matcher(filename).find()) {
                SeekableByteChannel seekableByteChannel =
                        Files.newByteChannel(file, EnumSet.of(StandardOpenOption.READ));
                try (ZipFile zipFile = new ZipFile(seekableByteChannel)) {
                    Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
                    while (entries.hasMoreElements()) {
                        ZipArchiveEntry entry = entries.nextElement();
                        if (entry.isDirectory()) {
                            continue;
                        }
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
                                            entryInputStream::read,
                                            buffer,
                                            crc32,
                                            md5,
                                            sha1),
                                    name));
                        }
                    }
                }
            } else if (Patterns.SEVEN_ZIP.matcher(filename).find()) {
                SeekableByteChannel seekableByteChannel =
                        Files.newByteChannel(file, EnumSet.of(StandardOpenOption.READ));
                try (SevenZFile sevenZFile = new SevenZFile(seekableByteChannel)) {
                    SevenZArchiveEntry entry;
                    while ((entry = sevenZFile.getNextEntry()) != null) {
                        if (entry.isDirectory()) {
                            continue;
                        }
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
                                        sevenZFile::read,
                                        buffer,
                                        crc32,
                                        md5,
                                        sha1),
                                name));
                    }
                }
            } else if (Patterns.TAR_ARCHIVE.matcher(filename).find()) {
                InputStream inputStream = inputStreamForTar(file);
                if (inputStream != null) {
                    try (TarArchiveInputStream tarArchiveInputStream =
                            new TarArchiveInputStream(inputStream)) {
                        TarArchiveEntry entry;
                        while ((entry = tarArchiveInputStream.getNextTarEntry()) != null) {
                            if (!entry.isFile() || !tarArchiveInputStream.canReadEntryData(entry)) {
                                continue;
                            }
                            long size = entry.getRealSize();
                            String name = entry.getName();
                            String entryLabel = relative.resolve(name).toString();
                            builder.add(new Result(
                                    file,
                                    size,
                                    processDigest(
                                            entryLabel,
                                            index,
                                            size,
                                            tarArchiveInputStream::read,
                                            buffer,
                                            crc32,
                                            md5,
                                            sha1),
                                    name));
                        }
                    }
                } else {
                    logger.warn("Unsupported TAR archive compression for '{}'", file);
                }
            } else {
                // TODO also read the archive itself
                try (InputStream inputStream = Files.newInputStream(file)) {
                    long size = fileMetadata.getSize();
                    builder.add(new Result(
                            file,
                            size,
                            processDigest(
                                    label,
                                    index,
                                    size,
                                    inputStream::read,
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

    @Nullable
    private static InputStream inputStreamForTar(Path file) throws IOException {
        InputStream inputStream = null;
        String filename = file.getFileName().toString();
        if (Patterns.TAR_GZ.matcher(filename).find()) {
            inputStream = new GzipCompressorInputStream(Files.newInputStream(file));
        } else if (Patterns.TAR_BZ2.matcher(filename).find()) {
            inputStream = new BZip2CompressorInputStream(Files.newInputStream(file));
        } else if (Patterns.TAR_XZ.matcher(filename).find()) {
            inputStream = new XZCompressorInputStream(Files.newInputStream(file));
        } else if (Patterns.TAR_LZMA.matcher(filename).find()) {
            inputStream = new LZMACompressorInputStream(Files.newInputStream(file));
        } else if (Patterns.TAR_LZ4.matcher(filename).find()) {
            inputStream = new BlockLZ4CompressorInputStream(Files.newInputStream(file));
        } else if (Patterns.TAR_UNCOMPRESSED.matcher(filename).find()) {
            inputStream = Files.newInputStream(file);
        }
        return inputStream;
    }

    // TODO: extract this to writer
    @Nullable
    private static OutputStream outputStreamForTar(Path file) throws IOException {
        OutputStream outputStream = null;
        String filename = file.getFileName().toString();
        if (Patterns.TAR_GZ.matcher(filename).find()) {
            outputStream = new GzipCompressorOutputStream(Files.newOutputStream(
                    file,
                    StandardOpenOption.CREATE_NEW));
        } else if (Patterns.TAR_BZ2.matcher(filename).find()) {
            outputStream = new BZip2CompressorOutputStream(Files.newOutputStream(
                    file,
                    StandardOpenOption.CREATE_NEW));
        } else if (Patterns.TAR_XZ.matcher(filename).find()) {
            outputStream = new XZCompressorOutputStream(Files.newOutputStream(
                    file,
                    StandardOpenOption.CREATE_NEW));
        } else if (Patterns.TAR_LZMA.matcher(filename).find()) {
            outputStream = new LZMACompressorOutputStream(Files.newOutputStream(
                    file,
                    StandardOpenOption.CREATE_NEW));
        } else if (Patterns.TAR_LZ4.matcher(filename).find()) {
            outputStream = new BlockLZ4CompressorOutputStream(Files.newOutputStream(
                    file,
                    StandardOpenOption.CREATE_NEW));
        } else if (Patterns.TAR_UNCOMPRESSED.matcher(filename).find()) {
            outputStream = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW);
        }
        return outputStream;
    }

    private Result.Digest processDigest(
            String label,
            int index,
            long size,
            TriFunction<byte[], Integer, Integer, Integer, IOException> function,
            byte[] buffer,
            CRC32 crc32,
            MessageDigest md5,
            MessageDigest sha1) throws IOException {
        // TODO add support for headers
        crc32.reset();
        md5.reset();
        sha1.reset();
        int reportedPercentage = 0;
        long totalRead = 0;
        long start = System.nanoTime();
        int bytesRead;
        int remainingBytes;
        while ((remainingBytes = (int) Math.min(size - totalRead, buffer.length)) > 0
                && (bytesRead = function.apply(buffer, 0, remainingBytes)) > -1) {
            totalRead += bytesRead;
            crc32.update(buffer, 0, bytesRead);
            md5.update(buffer, 0, bytesRead);
            sha1.update(buffer, 0, bytesRead);
            if (listener != null) {
                int percentage = (int) ((totalRead * 100d) / size);
                if (reportedPercentage != percentage) {
                    double secondsPassed = (System.nanoTime() - start) / 1E9d;
                    long bytesPerSecond = Math.round(bytesRead / secondsPassed);
                    listener.reportProgress(label, index, percentage, bytesPerSecond);
                    reportedPercentage = percentage;
                }
                start = System.nanoTime();
            }
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

    private interface TriFunction<K, L, M, N, E extends Throwable> {

        N apply(K k, L l, M m) throws E;
    }

}
