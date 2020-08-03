package io.github.datromtool.io;

import com.github.junrar.exception.RarException;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.ByteUnit;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.datafile.Game;
import io.github.datromtool.domain.datafile.Rom;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.domain.detector.Rule;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.compressors.lzma.LZMAUtils;
import org.apache.commons.compress.compressors.xz.XZUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    private final long minRomSize;
    private final long maxRomSize;
    private final String minRomSizeStr;
    private final String maxRomSizeStr;
    private final ThreadLocal<byte[]> threadLocalBuffer;

    private final ImmutableSet<ArchiveType> alsoScanArchives;

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
            this.minRomSize = 0L;
            this.alsoScanArchives = ImmutableSet.of();
        } else {
            this.minRomSize = datafile.getGames().stream()
                    .map(Game::getRoms)
                    .flatMap(Collection::stream)
                    .filter(r -> r.getSize() != null)
                    .mapToLong(Rom::getSize)
                    .min()
                    .orElse(0);
            this.alsoScanArchives = datafile.getGames().stream()
                    .map(Game::getRoms)
                    .flatMap(Collection::stream)
                    .filter(r -> r.getSize() != null)
                    .map(Rom::getName)
                    .map(ArchiveType::parse)
                    .filter(at -> at != ArchiveType.NONE)
                    .collect(ImmutableSet.toImmutableSet());
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
        this.minRomSizeStr = makeRomSizeStr(this.minRomSize);
        this.maxRomSizeStr = makeRomSizeStr(this.maxRomSize);
        this.threadLocalBuffer = ThreadLocal.withInitial(() -> new byte[bufferSize]);
    }

    private static String makeRomSizeStr(long size) {
        ByteUnit minRomSizeUnit = ByteUnit.getUnit(size);
        return String.format("%.02f %s", minRomSizeUnit.convert(size), minRomSizeUnit.getSymbol());
    }

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

        ArchiveType archiveType;
        Path path;
        long size;
        long unheaderedSize;
        Digest digest;
        String archivePath;
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class ProcessingResult {

        Result.Digest digest;
        long unheaderedSize;
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class FileMetadata {

        Path path;
        long size;
    }

    public interface Listener {

        void reportTotalItems(int totalItems);

        void reportStart(Path path, int thread);

        void reportProgress(Path path, int thread, int percentage, long speed);

        void reportSkip(Path path, int thread, String message);

        void reportFailure(Path path, int thread, String message, Throwable cause);

        void reportFinish(Path path, int thread);

    }

    @AllArgsConstructor
    private final static class AppendingFileVisitor extends SimpleFileVisitor<Path> {

        private final Consumer<FileMetadata> onVisited;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (attrs.isRegularFile()) {
                onVisited.accept(new FileMetadata(file, attrs.size()));
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            logger.warn("Failed to scan '{}'", file, exc);
            return FileVisitResult.SKIP_SUBTREE;
        }
    }

    public ImmutableList<Result> scan(Path directory) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(
                numThreads,
                new IndexedThreadFactory(logger, "SCANNER"));
        if (!LZMAUtils.isLZMACompressionAvailable()) {
            logger.warn("LZMA compression support is disabled");
        }
        if (!XZUtils.isXZCompressionAvailable()) {
            logger.warn("XZ compression support is disabled");
        }
        try {
            ImmutableList.Builder<FileMetadata> pathsBuilder = ImmutableList.builder();
            Files.walkFileTree(directory, new AppendingFileVisitor(pathsBuilder::add));
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
            throw e;
        } finally {
            executorService.shutdownNow();
        }
    }

    private static <T> Stream<T> streamResults(Future<ImmutableList<T>> future) {
        try {
            return future.get().stream();
        } catch (Exception e) {
            logger.error("Unexpected exception thrown", e);
            return Stream.empty();
        }
    }

    private boolean shouldSkip(Path path, int index, long size) {
        if (size < minRomSize) {
            logger.info(
                    "File is smaller than minimum ROM size of {}. Skip calculation of hashes: '{}'",
                    minRomSizeStr,
                    path);
            if (listener != null) {
                listener.reportSkip(path, index, "File too small");
            }
            return true;
        }
        if (size > maxRomSize) {
            logger.info(
                    "File is larger than maximum ROM size of {}. Skip calculation of hashes: '{}'",
                    maxRomSizeStr,
                    path);
            if (listener != null) {
                listener.reportSkip(path, index, "File too big");
            }
            return true;
        }
        return false;
    }

    private ImmutableList<Result> scanFile(FileMetadata fileMetadata) {
        Path file = fileMetadata.getPath();
        int index = ((IndexedThread) Thread.currentThread()).getIndex();
        if (listener != null) {
            listener.reportStart(file, index);
        }
        try {
            ImmutableList.Builder<Result> builder = ImmutableList.builder();
            boolean scanned = false;
            ArchiveType archiveType = ArchiveType.parse(file);
            try {
                switch (archiveType) {
                    case ZIP:
                        scanZip(file, index, builder);
                        scanned = true;
                        break;
                    case RAR:
                        scanRar(file, index, builder);
                        scanned = true;
                        break;
                    case SEVEN_ZIP:
                        scanSevenZip(file, index, builder);
                        scanned = true;
                        break;
                    case TAR:
                    case TAR_BZ2:
                    case TAR_GZ:
                    case TAR_LZ4:
                    case TAR_LZMA:
                    case TAR_XZ:
                        scanTar(archiveType, file, index, builder);
                        scanned = true;
                        break;
                }
            } catch (UnsupportedRarV5Exception e) {
                logger.error(
                        "Unexpected error while reading archive '{}' detected as {}. "
                                + "Reason: RAR5 is not supported yet",
                        file,
                        archiveType);
            } catch (Exception e) {
                logger.error(
                        "Unexpected error while reading archive '{}' detected as {}",
                        file,
                        archiveType,
                        e);
            }
            if (!scanned || alsoScanArchives.contains(archiveType)) {
                scanFile(fileMetadata, file, index, builder);
            }
            if (listener != null) {
                listener.reportFinish(file, index);
            }
            return builder.build();
        } catch (Exception e) {
            logger.error("Could not read file '{}'", file, e);
            if (listener != null) {
                listener.reportFailure(file, index, "Could not read the file", e);
            }
            return ImmutableList.of();
        } finally {
            if (listener != null) {
                listener.reportFinish(file, index);
            }
        }
    }

    private void scanFile(
            FileMetadata fileMetadata,
            Path file,
            int index,
            ImmutableList.Builder<Result> builder) throws IOException {
        long size = fileMetadata.getSize();
        if (!shouldSkip(file, index, size)) {
            try (InputStream inputStream = Files.newInputStream(file)) {
                ProcessingResult processingResult = process(
                        file,
                        index,
                        size,
                        inputStream::read);
                builder.add(new Result(
                        ArchiveType.NONE,
                        file,
                        size,
                        processingResult.getUnheaderedSize(),
                        processingResult.getDigest(),
                        null));
            }
        }
    }

    private void scanZip(
            Path file,
            int index,
            ImmutableList.Builder<Result> builder) throws IOException {
        ArchiveUtils.readZip(file, (zipFile, zipArchiveEntry) -> {
            long size = zipArchiveEntry.getSize();
            String name = zipArchiveEntry.getName();
            Path entryPath = file.resolve(name);
            if (shouldSkip(entryPath, index, size)) {
                return;
            }
            try (InputStream entryInputStream = zipFile.getInputStream(zipArchiveEntry)) {
                ProcessingResult processingResult = process(
                        entryPath,
                        index,
                        size,
                        entryInputStream::read);
                builder.add(new Result(
                        ArchiveType.ZIP,
                        file,
                        size,
                        processingResult.getUnheaderedSize(),
                        processingResult.getDigest(),
                        name));
            }
        });
    }

    private void scanRar(
            Path file,
            int index,
            ImmutableList.Builder<Result> builder) throws IOException, RarException {
        ArchiveUtils.readRar(file, (archive, fileHeader) -> {
            long size = fileHeader.getFullUnpackSize();
            String name = fileHeader.getFileName();
            Path entryPath = file.resolve(name);
            if (shouldSkip(entryPath, index, size)) {
                return;
            }
            try (InputStream rarFileInputStream = archive.getInputStream(fileHeader)) {
                ProcessingResult processingResult = process(
                        entryPath,
                        index,
                        size,
                        rarFileInputStream::read);
                builder.add(new Result(
                        ArchiveType.RAR,
                        file,
                        size,
                        processingResult.getUnheaderedSize(),
                        processingResult.getDigest(),
                        name));
            }
        });
    }

    private void scanSevenZip(
            Path file,
            int index,
            ImmutableList.Builder<Result> builder) throws IOException {
        ArchiveUtils.readSevenZip(file, (sevenZFile, sevenZArchiveEntry) -> {
            long size = sevenZArchiveEntry.getSize();
            String name = sevenZArchiveEntry.getName();
            Path entryPath = file.resolve(name);
            if (shouldSkip(entryPath, index, size)) {
                return;
            }
            ProcessingResult processingResult = process(
                    entryPath,
                    index,
                    size,
                    sevenZFile::read);
            builder.add(new Result(
                    ArchiveType.SEVEN_ZIP,
                    file,
                    size,
                    processingResult.getUnheaderedSize(),
                    processingResult.getDigest(),
                    name));
        });
    }

    private void scanTar(
            ArchiveType archiveType,
            Path file,
            int index,
            ImmutableList.Builder<Result> builder) throws IOException {
        ArchiveUtils.readTar(archiveType, file, (tarArchiveEntry, tarArchiveInputStream) -> {
            long size = tarArchiveEntry.getRealSize();
            String name = tarArchiveEntry.getName();
            Path entryPath = file.resolve(name);
            if (shouldSkip(entryPath, index, size)) {
                return;
            }
            ProcessingResult processingResult = process(
                    entryPath,
                    index,
                    size,
                    tarArchiveInputStream::read);
            builder.add(new Result(
                    archiveType,
                    file,
                    size,
                    processingResult.getUnheaderedSize(),
                    processingResult.getDigest(),
                    name));
        });
    }

    @Nonnull
    private ProcessingResult process(
            Path path,
            int index,
            long size,
            TriFunction<byte[], Integer, Integer, Integer, IOException> function)
            throws IOException {
        CRC32 crc32 = threadLocalCrc32.get();
        MessageDigest md5 = threadLocalMd5.get();
        MessageDigest sha1 = threadLocalSha1.get();
        byte[] buffer = threadLocalBuffer.get();
        crc32.reset();
        md5.reset();
        sha1.reset();
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
                    // Identity check is enough here
                    if (newBuffer != buffer) {
                        logger.info(
                                "Detected header using '{}' for '{}'",
                                detector.getName(),
                                path);
                        totalRead = newBuffer.length;
                        buffer = newBuffer;
                        break;
                    }
                } catch (Exception e) {
                    logger.error("Error while processing rule for '{}'", path, e);
                    if (listener != null) {
                        listener.reportFailure(path, index, "Error while processing rule", e);
                    }
                }
            }
            int totalReadInt = Math.toIntExact(totalRead);
            crc32.update(buffer, 0, totalReadInt);
            md5.update(buffer, 0, totalReadInt);
            sha1.update(buffer, 0, totalReadInt);
            if (listener != null) {
                double secondsPassed = (System.nanoTime() - start) / 1E9d;
                long bytesPerSecond = Math.round(totalRead / secondsPassed);
                listener.reportProgress(path, index, 100, bytesPerSecond);
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
                        listener.reportProgress(path, index, percentage, bytesPerSecond);
                        reportedPercentage = percentage;
                    }
                    start = System.nanoTime();
                }
            }
        }
        return new ProcessingResult(
                new Result.Digest(
                        Long.toHexString(crc32.getValue()),
                        Hex.encodeHexString(md5.digest()),
                        Hex.encodeHexString(sha1.digest())),
                totalRead);
    }

    @FunctionalInterface
    private interface TriFunction<K, L, M, N, E extends Throwable> {

        N apply(K k, L l, M m) throws E;
    }

}
