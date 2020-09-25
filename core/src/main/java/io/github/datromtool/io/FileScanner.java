package io.github.datromtool.io;

import com.github.junrar.exception.UnsupportedRarV5Exception;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.domain.detector.Rule;
import io.github.datromtool.util.ArchiveUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
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
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import static io.github.datromtool.io.FileScannerParameters.forDatWithDetector;
import static io.github.datromtool.io.FileScannerParameters.withDefaults;

public final class FileScanner {

    private static final Logger logger = LoggerFactory.getLogger(FileScanner.class);

    private final int numThreads;
    private final ImmutableList<Detector> detectors;
    private final Listener listener;
    private final FileScannerParameters fileScannerParameters;
    private final ThreadLocal<ThreadLocalDataHolder> threadLocalData;

    public FileScanner(
            @Nonnull AppConfig appConfig,
            @Nonnull List<Datafile> datafiles,
            @Nonnull List<Detector> detectors,
            @Nullable Listener listener) {
        this.numThreads = appConfig.getScanner().getThreads();
        this.detectors = ImmutableList.copyOf(detectors);
        this.listener = listener;
        if (datafiles.isEmpty()) {
            this.fileScannerParameters = withDefaults();
        } else {
            this.fileScannerParameters = forDatWithDetector(appConfig, datafiles, detectors);
        }
        this.threadLocalData =
                ThreadLocal.withInitial(() -> new ThreadLocalDataHolder(fileScannerParameters));
    }

    @Value
    private static class ThreadLocalDataHolder {
        byte[] buffer;
        CRC32 crc32 = new CRC32();
        MessageDigest md5 = DigestUtils.getMd5Digest();
        MessageDigest sha1 = DigestUtils.getSha1Digest();

        private ThreadLocalDataHolder(FileScannerParameters fileScannerParameters) {
            this.buffer = new byte[fileScannerParameters.getBufferSize()];
        }
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Result {

        @Value
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Digest {
            @NonNull
            String crc;
            @NonNull
            String md5;
            @NonNull
            String sha1;
        }

        ArchiveType archiveType;
        @NonNull
        Path path;
        long size;
        long unheaderedSize;
        @NonNull
        Digest digest;
        String archivePath;
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class ProcessingResult {
        @NonNull
        Result.Digest digest;
        long unheaderedSize;
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class FileMetadata {
        @NonNull
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

    public ImmutableList<Result> scan(List<Path> directories) {
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
            for (Path directory : directories) {
                try {
                    Files.walkFileTree(directory, new AppendingFileVisitor(pathsBuilder::add));
                } catch (Exception e) {
                    logger.error("Could not scan '{}'", directory, e);
                }
            }
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
            logger.error("Could not scan '{}'", directories, e);
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
        if (size < fileScannerParameters.getMinRomSize()) {
            logger.info(
                    "File is smaller than minimum ROM size of {}. Skip calculation of hashes: '{}'",
                    fileScannerParameters.getMinRomSizeStr(),
                    path);
            if (listener != null) {
                listener.reportSkip(path, index, "File too small");
            }
            return true;
        }
        if (size > fileScannerParameters.getMaxRomSize()) {
            logger.info(
                    "File is larger than maximum ROM size of {}. Skip calculation of hashes: '{}'",
                    fileScannerParameters.getMaxRomSizeStr(),
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
            if (archiveType != null) {
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
                    scanned = true;
                } catch (Exception e) {
                    logger.error(
                            "Unexpected error while reading archive '{}' detected as {}",
                            file,
                            archiveType,
                            e);
                }
            }
            if (!scanned || fileScannerParameters.getAlsoScanArchives().contains(archiveType)) {
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
                        null,
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
            ImmutableList.Builder<Result> builder) throws Exception {
        try {
            ArchiveUtils.readRar(file, (archive, fileHeader) -> {
                long size = fileHeader.getFullUnpackSize();
                String name = fileHeader.getFileName().replace('\\', '/');
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
        } catch (UnsupportedRarV5Exception e) {
            if (ArchiveUtils.isUnrarAvailable()) {
                scanRarWithUnrar(file, index, builder);
            } else {
                throw e;
            }
        }
    }

    private void scanRarWithUnrar(
            Path file,
            int index,
            ImmutableList.Builder<Result> builder) throws Exception {
        ImmutableSet<String> desiredEntryNames =
                ArchiveUtils.listRarEntriesWithUnrar(file)
                        .stream()
                        .filter(e -> !shouldSkip(file.resolve(e.getName()), index, e.getSize()))
                        .map(UnrarArchiveEntry::getName)
                        .collect(ImmutableSet.toImmutableSet());
        if (desiredEntryNames.isEmpty()) {
            return;
        }
        ArchiveUtils.readRarWithUnrar(file, desiredEntryNames, (entry, processInputStream) -> {
            long size = entry.getSize();
            String name = entry.getName();
            Path entryPath = file.resolve(name);
            ProcessingResult processingResult = process(
                    entryPath,
                    index,
                    size,
                    processInputStream::read);
            builder.add(new Result(
                    ArchiveType.RAR,
                    file,
                    size,
                    processingResult.getUnheaderedSize(),
                    processingResult.getDigest(),
                    name));
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
        ThreadLocalDataHolder threadLocalDataHolder = threadLocalData.get();
        CRC32 crc32 = threadLocalDataHolder.getCrc32();
        MessageDigest md5 = threadLocalDataHolder.getMd5();
        MessageDigest sha1 = threadLocalDataHolder.getSha1();
        byte[] buffer = threadLocalDataHolder.getBuffer();
        crc32.reset();
        md5.reset();
        sha1.reset();
        long totalRead;
        if (size <= buffer.length && detectors != null) {
            totalRead = readAllAtOnce(path, index, size, function, crc32, md5, sha1, buffer);
        } else {
            totalRead = readInSteps(path, index, size, function, crc32, md5, sha1, buffer);
        }
        return new ProcessingResult(
                new Result.Digest(
                        Strings.padStart(Long.toHexString(crc32.getValue()), 8, '0'),
                        Hex.encodeHexString(md5.digest()),
                        Hex.encodeHexString(sha1.digest())),
                totalRead);
    }

    private long readAllAtOnce(
            Path path,
            int index,
            long size,
            TriFunction<byte[], Integer, Integer, Integer, IOException> function,
            CRC32 crc32, MessageDigest md5, MessageDigest sha1, byte[] buffer) throws IOException {
        long totalRead = 0;
        long start = System.nanoTime();
        int bytesRead;
        int bytesLeft;
        // Read the whole entry to the buffer and check headers
        while ((bytesLeft = toInt(Math.min(size - totalRead, buffer.length))) > 0
                && (bytesRead = function.apply(buffer, toInt(totalRead), bytesLeft)) > -1) {
            totalRead += bytesRead;
        }
        // Apply logic to detect headers
        // This is the only time we're going to read the file anyway
        // We can safely redefine the buffer variable here
        for (Detector detector : detectors) {
            boolean detected = false;
            for (Rule rule : detector.getRules()) {
                try {
                    byte[] newBuffer = rule.apply(buffer, toInt(totalRead), size);
                    // Identity check is enough here
                    if (newBuffer != buffer) {
                        detected = true;
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
            if (detected) {
                logger.info(
                        "Detected header using '{}' for '{}'",
                        detector.getName(),
                        path);
                break;
            }
        }
        int endRead = toInt(Math.min(totalRead, buffer.length));
        crc32.update(buffer, 0, endRead);
        md5.update(buffer, 0, endRead);
        sha1.update(buffer, 0, endRead);
        if (listener != null) {
            double secondsPassed = (System.nanoTime() - start) / 1E9d;
            long bytesPerSecond = Math.round(totalRead / secondsPassed);
            listener.reportProgress(path, index, 100, bytesPerSecond);
        }
        return totalRead;
    }

    private long readInSteps(
            Path path,
            int index,
            long size,
            TriFunction<byte[], Integer, Integer, Integer, IOException> function,
            CRC32 crc32, MessageDigest md5, MessageDigest sha1, byte[] buffer) throws IOException {
        long totalRead = 0;
        long start = System.nanoTime();
        int reportedPercentage = 0;
        long endOffset = size;
        int bytesRead;
        int bytesLeft;
        if (!detectors.isEmpty() && fileScannerParameters.isUseLazyDetector()) {
            long startOffset = 0;
            // Read as much as possible to the buffer and check headers
            while ((bytesLeft = toInt(buffer.length - totalRead)) > 0
                    && (bytesRead = function.apply(buffer, toInt(totalRead), bytesLeft)) > -1) {
                totalRead += bytesRead;
            }
            // Apply logic to detect headers
            for (Detector detector : detectors) {
                boolean detected = false;
                for (Rule rule : detector.getRules()) {
                    try {
                        if (rule.test(buffer, toInt(totalRead), size)) {
                            detected = true;
                            startOffset = Math.max(rule.getStartOffset(), startOffset);
                            long currEndOffset = rule.getEndOffset();
                            if (currEndOffset < 0) {
                                currEndOffset += size;
                            }
                            endOffset = Math.min(currEndOffset, endOffset);
                            // The file is smaller than the detected header portion
                            if (startOffset > endOffset) {
                                startOffset = 0;
                                endOffset = size;
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error while processing rule for '{}'", path, e);
                        if (listener != null) {
                            listener.reportFailure(
                                    path,
                                    index,
                                    "Error while processing rule",
                                    e);
                        }
                    }
                }
                if (detected) {
                    logger.info(
                            "Detected header using '{}' for '{}'",
                            detector.getName(),
                            path);
                    break;
                }
            }
            totalRead = Math.min(totalRead, endOffset) - startOffset;
            int startOffsetInt = toInt(startOffset);
            int totalReadInt = toInt(totalRead);
            crc32.update(buffer, startOffsetInt, totalReadInt);
            md5.update(buffer, startOffsetInt, totalReadInt);
            sha1.update(buffer, startOffsetInt, totalReadInt);
            if (listener != null) {
                int percentage = toInt((totalRead * 100d) / size);
                if (reportedPercentage != percentage) {
                    double secondsPassed = (System.nanoTime() - start) / 1E9d;
                    long bytesPerSecond = Math.round(totalRead / secondsPassed);
                    listener.reportProgress(path, index, percentage, bytesPerSecond);
                    reportedPercentage = percentage;
                }
                start = System.nanoTime();
            }
        }
        // Regular reading until we get to the end offset
        while ((bytesLeft = toInt(Math.min(endOffset - totalRead, buffer.length))) > 0
                && (bytesRead = function.apply(buffer, 0, bytesLeft)) > -1) {
            totalRead += bytesRead;
            crc32.update(buffer, 0, bytesRead);
            md5.update(buffer, 0, bytesRead);
            sha1.update(buffer, 0, bytesRead);
            if (listener != null) {
                int percentage = toInt((totalRead * 100d) / size);
                if (reportedPercentage != percentage) {
                    double secondsPassed = (System.nanoTime() - start) / 1E9d;
                    long bytesPerSecond = Math.round(bytesRead / secondsPassed);
                    listener.reportProgress(path, index, percentage, bytesPerSecond);
                    reportedPercentage = percentage;
                }
                start = System.nanoTime();
            }
        }
        return totalRead;
    }

    private static int toInt(double a) {
        return toInt((long) a);
    }

    private static int toInt(long a) {
        return Math.toIntExact(a);
    }

    @FunctionalInterface
    private interface TriFunction<K, L, M, N, E extends Throwable> {

        N apply(K k, L l, M m) throws E;
    }

}
