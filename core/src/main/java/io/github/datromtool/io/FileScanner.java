package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.ByteUnit;
import io.github.datromtool.Patterns;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.datafile.Game;
import io.github.datromtool.domain.datafile.Rom;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.domain.detector.Rule;
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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.CRC32;

public final class FileScanner {

    private static final Logger logger = LoggerFactory.getLogger(FileScanner.class);

    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024; // 32KB per thread
    private static final int MAX_BUFFER_NO_WARNING = 64 * 1024 * 1024; // 64MB
    private static final int MAX_BUFFER = 256 * 1024 * 1024; // 256MB

    private final ThreadLocal<CRC32> threadLocalCrc32 = ThreadLocal.withInitial(CRC32::new);
    private final ThreadLocal<MessageDigest> threadLocalMd5 =
            ThreadLocal.withInitial(DigestUtils::getMd5Digest);
    private final ThreadLocal<MessageDigest> threadLocalSha1 =
            ThreadLocal.withInitial(DigestUtils::getSha1Digest);

    private final int numThreads;
    private final Detector detector;
    private final Listener listener;
    private final long maxRomSize;
    private final String maxHeaderedSizeStr;
    private final ThreadLocal<byte[]> threadLocalBuffer;

    public FileScanner(
            int numThreads,
            @Nullable Datafile datafile,
            @Nullable Detector detector,
            @Nullable Listener listener) {
        this.numThreads = numThreads;
        this.detector = detector;
        this.listener = listener;
        int bufferSize;
        if (datafile == null) {
            bufferSize = DEFAULT_BUFFER_SIZE;
            this.maxRomSize = Long.MAX_VALUE;
        } else {
            if (detector == null) {
                bufferSize = DEFAULT_BUFFER_SIZE;
                this.maxRomSize = datafile.getGames().stream()
                        .map(Game::getRoms)
                        .flatMap(Collection::stream)
                        .filter(r -> r.getSize() != null)
                        .mapToLong(Rom::getSize)
                        .max()
                        .orElse(Long.MAX_VALUE);
            } else {
                long maxStartOffset = detector.getRules()
                        .stream()
                        .filter(r -> r.getStartOffset() != null)
                        .mapToLong(Rule::getStartOffset)
                        .max()
                        .orElse(0);
                long minEndOffset = detector.getRules()
                        .stream()
                        .filter(r -> r.getEndOffset() != null)
                        .mapToLong(Rule::getEndOffset)
                        .min()
                        .orElse(Long.MAX_VALUE);
                long maxUnheaderedSize = datafile.getGames().stream()
                        .map(Game::getRoms)
                        .flatMap(Collection::stream)
                        .filter(r -> r.getSize() != null)
                        .mapToLong(Rom::getSize)
                        .max()
                        .orElse(Long.MAX_VALUE);
                if (maxStartOffset < 0) {
                    maxStartOffset += maxUnheaderedSize;
                }
                if (minEndOffset < 0) {
                    minEndOffset += maxUnheaderedSize;
                }
                maxStartOffset = Math.max(maxStartOffset, 0);
                minEndOffset = Math.min(minEndOffset, maxUnheaderedSize);
                this.maxRomSize = maxUnheaderedSize
                        + maxStartOffset
                        + (maxUnheaderedSize - minEndOffset);
                bufferSize =
                        (int) Math.max(Math.min(maxRomSize, MAX_BUFFER), DEFAULT_BUFFER_SIZE);
                ByteUnit unit = ByteUnit.getUnit(bufferSize);
                String bufferSizeStr = String.format("%.02f", unit.convert(bufferSize));
                if (bufferSize > MAX_BUFFER_NO_WARNING) {
                    logger.warn(
                            "Using a bigger I/O buffer size of {} {} due to header detection",
                            bufferSizeStr,
                            unit.getSymbol());
                    if (bufferSize == MAX_BUFFER) {
                        logger.warn(
                                "Disabling header detection for ROMs larger than {} {}",
                                bufferSizeStr,
                                unit.getSymbol());
                    }
                } else {
                    logger.info("Using I/O buffer size of {} {}", bufferSizeStr, unit.getSymbol());
                }
            }
        }
        ByteUnit maxHeaderedSizeUnit = ByteUnit.getUnit(this.maxRomSize);
        this.maxHeaderedSizeStr = String.format(
                "%.02f %s",
                maxHeaderedSizeUnit.convert(maxRomSize),
                maxHeaderedSizeUnit.getSymbol());
        this.threadLocalBuffer = ThreadLocal.withInitial(() -> new byte[bufferSize]);
    }

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
        long unheaderedSize;
        Digest digest;
        String archivePath;
    }

    @Value
    private static class ProcessingResult {

        Result.Digest digest;
        long unheaderedSize;
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

        private IndexedThread(int index, Runnable target) {
            super(target);
            this.index = index;
        }

    }

    public ImmutableList<Result> scan(Path directory) {
        ExecutorService executorService = Executors.newFixedThreadPool(
                numThreads,
                new IndexedThreadFactory());
        try {
            ImmutableList.Builder<FileMetadata> pathsBuilder = ImmutableList.builder();
            Files.walkFileTree(directory, new AppendingFileVisitor(directory, pathsBuilder::add));
            ImmutableList<FileMetadata> paths = pathsBuilder.build();
            if (listener != null) {
                listener.reportTotalItems(paths.size());
            }
            return paths.stream()
                    .sorted(Comparator.comparingLong(fm -> -fm.getSize()))
                    .map(fm -> executorService.submit(() -> scanFile(fm)))
                    .collect(ImmutableList.toImmutableList())
                    .stream()
                    .flatMap(FileScanner::streamResults)
                    .collect(ImmutableList.toImmutableList());
        } catch (Exception e) {
            logger.error("Could not scan '{}'", directory, e);
            return ImmutableList.of();
        } finally {
            executorService.shutdownNow();
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
        int index = ((IndexedThread) Thread.currentThread()).getIndex();
        if (listener != null) {
            listener.reportStart(label, index);
        }
        try {
            ImmutableList.Builder<Result> builder = ImmutableList.builder();
            String filename = file.getFileName().toString();
            if (Patterns.ZIP.matcher(filename).find()) {
                try (ZipFile zipFile = new ZipFile(file.toFile())) {
                    Enumeration<ZipArchiveEntry> entries = zipFile.getEntriesInPhysicalOrder();
                    while (entries.hasMoreElements()) {
                        ZipArchiveEntry entry = entries.nextElement();
                        if (entry.isDirectory() || entry.isUnixSymlink()) {
                            continue;
                        }
                        try (InputStream entryInputStream = zipFile.getInputStream(entry)) {
                            long size = entry.getSize();
                            String name = entry.getName();
                            String entryLabel = relative.resolve(name).toString();
                            ProcessingResult processingResult = process(
                                    entryLabel,
                                    index,
                                    size,
                                    entryInputStream::read);
                            builder.add(new Result(
                                    file,
                                    size,
                                    processingResult.getUnheaderedSize(),
                                    processingResult.getDigest(),
                                    name));
                        }
                    }
                }
            } else if (Patterns.SEVEN_ZIP.matcher(filename).find()) {
                try (SevenZFile sevenZFile = new SevenZFile(file.toFile())) {
                    SevenZArchiveEntry entry;
                    while ((entry = sevenZFile.getNextEntry()) != null) {
                        if (entry.isDirectory() || entry.isAntiItem()) {
                            continue;
                        }
                        long size = entry.getSize();
                        String name = entry.getName();
                        String entryLabel = relative.resolve(name).toString();
                        ProcessingResult processingResult = process(
                                entryLabel,
                                index,
                                size,
                                sevenZFile::read);
                        builder.add(new Result(
                                file,
                                size,
                                processingResult.getUnheaderedSize(),
                                processingResult.getDigest(),
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
                            ProcessingResult processingResult = process(
                                    entryLabel,
                                    index,
                                    size,
                                    tarArchiveInputStream::read);
                            builder.add(new Result(
                                    file,
                                    size,
                                    processingResult.getUnheaderedSize(),
                                    processingResult.getDigest(),
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
                    ProcessingResult processingResult = process(
                            label,
                            index,
                            size,
                            inputStream::read);
                    builder.add(new Result(
                            file,
                            size,
                            processingResult.getUnheaderedSize(),
                            processingResult.getDigest(),
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

    @Nonnull
    private ProcessingResult process(
            String label,
            int index,
            long size,
            TriFunction<byte[], Integer, Integer, Integer, IOException> function)
            throws IOException {
        if (size > maxRomSize) {
            logger.info(
                    "File is larger than maximum ROM size of {}. Skip calculation of hashes: '{}'",
                    maxHeaderedSizeStr,
                    label);
            return new ProcessingResult(null, size);
        }
        CRC32 crc32 = threadLocalCrc32.get();
        MessageDigest md5 = threadLocalMd5.get();
        MessageDigest sha1 = threadLocalSha1.get();
        byte[] buffer = threadLocalBuffer.get();
        int reportedPercentage = 0;
        long totalRead = 0;
        long start = System.nanoTime();
        int bytesRead;
        int remainingBytes;
        if (size <= buffer.length && detector != null) {
            // Read the whole entry to the buffer and check headers
            while ((remainingBytes = (int) Math.min(size - totalRead, buffer.length)) > 0
                    && (bytesRead = function.apply(buffer, (int) totalRead, remainingBytes)) > -1) {
                totalRead += bytesRead;
            }
            // Apply logic to detect headers
            // This is the only time we're going to read the file anyway
            // We can safely redefine the buffer variable here
            for (Rule r : detector.getRules()) {
                try {
                    byte[] newBuffer = r.apply(buffer, (int) totalRead);
                    if (newBuffer != buffer) {
                        logger.info(
                                "Detected header using '{}' for '{}'",
                                detector.getName(),
                                label);
                        totalRead = newBuffer.length;
                        buffer = newBuffer;
                        break;
                    }
                } catch (Exception e) {
                    logger.error("Error while processing rule for '{}'", label, e);
                }
            }
            crc32.update(buffer, 0, (int) totalRead);
            md5.update(buffer, 0, (int) totalRead);
            sha1.update(buffer, 0, (int) totalRead);
            if (listener != null) {
                double secondsPassed = (System.nanoTime() - start) / 1E9d;
                long bytesPerSecond = Math.round(totalRead / secondsPassed);
                listener.reportProgress(label, index, 100, bytesPerSecond);
            }
        } else {
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
        }
        ProcessingResult processingResult = new ProcessingResult(
                new Result.Digest(
                        Long.toHexString(crc32.getValue()),
                        Hex.encodeHexString(md5.digest()),
                        Hex.encodeHexString(sha1.digest())),
                totalRead);
        crc32.reset();
        md5.reset();
        sha1.reset();
        return processingResult;
    }

    private interface TriFunction<K, L, M, N, E extends Throwable> {

        N apply(K k, L l, M m) throws E;
    }

}
