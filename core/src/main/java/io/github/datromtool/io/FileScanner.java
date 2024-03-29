package io.github.datromtool.io;

import com.github.junrar.exception.UnsupportedRarV5Exception;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.domain.datafile.logiqx.Datafile;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.domain.detector.Rule;
import io.github.datromtool.io.logging.FileScannerLoggingListener;
import io.github.datromtool.util.ArchiveUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.compressors.lzma.LZMAUtils;
import org.apache.commons.compress.compressors.xz.XZUtils;

import javax.annotation.Nonnull;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static io.github.datromtool.io.FileScannerParameters.forDatWithDetector;
import static io.github.datromtool.io.FileScannerParameters.withDefaults;
import static io.github.datromtool.util.ArchiveUtils.normalizePath;
import static java.lang.Math.toIntExact;
import static java.util.Objects.requireNonNull;

@Slf4j
public final class FileScanner {

    private static final Comparator<FileMetadata> FILE_SIZE_DESCENDING_COMPARATOR =
            Comparator.comparingLong(FileMetadata::getSize).reversed();

    private final AppConfig.FileScannerConfig config;
    private final ImmutableList<Detector> detectors;
    private final ImmutableList<Listener> listeners;
    private final FileScannerParameters fileScannerParameters;
    private final ThreadLocal<ThreadLocalDataHolder> threadLocalData;

    public FileScanner(
            @Nonnull AppConfig.FileScannerConfig config,
            @Nonnull Collection<Datafile> datafiles,
            @Nonnull Collection<Detector> detectors,
            @Nonnull List<Listener> listeners) {
        this.config = config;
        this.detectors = ImmutableList.copyOf(requireNonNull(detectors));
        this.listeners = processListenerList(requireNonNull(listeners));
        if (datafiles.isEmpty()) {
            this.fileScannerParameters = withDefaults();
        } else {
            this.fileScannerParameters = forDatWithDetector(config, datafiles, detectors);
        }
        this.threadLocalData = ThreadLocal.withInitial(() -> new ThreadLocalDataHolder(fileScannerParameters));
    }

    @Nonnull
    private static ImmutableList<FileScanner.Listener> processListenerList(@Nonnull List<FileScanner.Listener> listeners) {
        if (listeners.stream().noneMatch(FileScannerLoggingListener.class::isInstance)) {
            return ImmutableList.<FileScanner.Listener>builder().add(new FileScannerLoggingListener()).addAll(listeners).build();
        }
        return ImmutableList.copyOf(listeners);
    }

    @Value
    private static class ThreadLocalDataHolder {
        byte[] buffer;
        CRC32 crc32 = new CRC32();
        MessageDigest md5 = DigestUtils.getMd5Digest();
        MessageDigest sha1 = DigestUtils.getSha1Digest();
        MessageDigest sha256 = DigestUtils.getSha256Digest();

        private ThreadLocalDataHolder(FileScannerParameters fileScannerParameters) {
            this.buffer = new byte[fileScannerParameters.getBufferSize()];
        }
    }

    @With(value = AccessLevel.PACKAGE)
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
            @NonNull
            String sha256;
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

        void reportListing(Path path);

        void reportFinishedListing(int amount);

        void init(int numThreads);

        void reportTotalItems(int totalItems);

        void reportStart(int thread, Path path, long bytes);

        void reportBytesRead(int thread, long bytes);

        void reportSkip(int thread, Path path, String message);

        void reportFailure(int thread, Path path, String message, Throwable cause);

        void reportFinish(int thread, Path path);

        void reportAllFinished();

    }

    @AllArgsConstructor
    private final static class AppendingFileVisitor extends SimpleFileVisitor<Path> {

        private final Consumer<FileMetadata> onVisited;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (attrs.isRegularFile()) {
                log.info("Adding file to scan list: '{}'", file);
                onVisited.accept(new FileMetadata(file, attrs.size()));
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            log.warn("Failed to scan '{}'", file, exc);
            return FileVisitResult.SKIP_SUBTREE;
        }
    }

    public ImmutableList<Result> scan(Collection<Path> directories) {
        ExecutorService executorService = Executors.newFixedThreadPool(
                config.getThreads(),
                new IndexedThreadFactory(log, "SCANNER"));
        if (!LZMAUtils.isLZMACompressionAvailable()) {
            log.warn("LZMA compression support is disabled");
        }
        if (!XZUtils.isXZCompressionAvailable()) {
            log.warn("XZ compression support is disabled");
        }
        try {
            ImmutableList.Builder<FileMetadata> pathsBuilder = ImmutableList.builder();
            for (Path directory : directories) {
                for (Listener listener : listeners) {
                    listener.reportListing(directory);
                }
                try {
                    Files.walkFileTree(directory, new AppendingFileVisitor(pathsBuilder::add));
                } catch (Exception e) {
                    log.error("Could not scan '{}'", directory, e);
                }
            }
            ImmutableList<FileMetadata> paths = pathsBuilder.build();
            for (Listener listener : listeners) {
                listener.reportFinishedListing(paths.size());
                listener.init(config.getThreads());
                listener.reportTotalItems(paths.size());
            }
            ImmutableList<Result> results = paths.stream()
                    .sorted(FILE_SIZE_DESCENDING_COMPARATOR)
                    .map(fm -> executorService.submit(() -> scanFile(fm)))
                    .collect(ImmutableList.toImmutableList())
                    .stream()
                    .flatMap(FileScanner::streamResults)
                    .collect(ImmutableList.toImmutableList());
            for (Listener listener : listeners) {
                listener.reportAllFinished();
            }
            return results;
        } catch (Exception e) {
            log.error("Could not scan '{}'", directories, e);
            throw e;
        } finally {
            executorService.shutdownNow();
        }
    }

    private static <T> Stream<T> streamResults(Future<ImmutableList<T>> future) {
        try {
            return future.get().stream();
        } catch (Exception e) {
            log.error("Unexpected exception thrown", e);
            return Stream.empty();
        }
    }

    private boolean shouldSkip(Path path, int index, long size) {
        if (size < fileScannerParameters.getMinRomSize()) {
            log.info(
                    "File is smaller than minimum ROM size of {}. Skip calculation of hashes: '{}'",
                    fileScannerParameters.getMinRomSizeStr(),
                    path);
            for (Listener listener : listeners) {
                listener.reportSkip(index, path, "File too small");
            }
            return true;
        }
        if (size > fileScannerParameters.getMaxRomSize()) {
            log.info(
                    "File is larger than maximum ROM size of {}. Skip calculation of hashes: '{}'",
                    fileScannerParameters.getMaxRomSizeStr(),
                    path);
            for (Listener listener : listeners) {
                listener.reportSkip(index, path, "File too big");
            }
            return true;
        }
        return false;
    }

    private ImmutableList<Result> scanFile(FileMetadata fileMetadata) {
        Path file = fileMetadata.getPath();
        int index = ((IndexedThread) Thread.currentThread()).getIndex();
        for (Listener listener : listeners) {
            listener.reportStart(index, file, fileMetadata.getSize());
        }
        try {
            ImmutableList.Builder<Result> builder = ImmutableList.builder();
            boolean scanned = false;
            ArchiveType archiveType = ArchiveType.parse(file);
            if (archiveType != null) {
                try {
                    switch (archiveType) {
                        case ZIP -> {
                            scanZip(file, index, builder);
                            scanned = true;
                        }
                        case RAR -> {
                            scanRar(file, index, builder);
                            scanned = true;
                        }
                        case SEVEN_ZIP -> {
                            scanSevenZip(file, index, builder);
                            scanned = true;
                        }
                        case TAR, TAR_BZ2, TAR_GZ, TAR_LZ4, TAR_LZMA, TAR_XZ -> {
                            scanTar(archiveType, file, index, builder);
                            scanned = true;
                        }
                    }
                } catch (UnsupportedRarV5Exception e) {
                    log.error(
                            "Unexpected error while reading archive '{}' detected as {}. "
                                    + "Reason: RAR5 is not supported yet",
                            file,
                            archiveType);
                    scanned = true;
                } catch (Exception e) {
                    log.error(
                            "Unexpected error while reading archive '{}' detected as {}",
                            file,
                            archiveType,
                            e);
                }
            }
            if (!scanned || fileScannerParameters.getAlsoScanArchives().contains(archiveType)) {
                scanFile(fileMetadata, file, index, builder);
            }
            return builder.build();
        } catch (Exception e) {
            log.error("Could not read file '{}'", file, e);
            for (Listener listener : listeners) {
                listener.reportFailure(index, file, "Could not read the file", e);
            }
            return ImmutableList.of();
        } finally {
            for (Listener listener : listeners) {
                listener.reportFinish(index, file);
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
            String name = normalizePath(zipArchiveEntry.getName());
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
                String name = normalizePath(fileHeader.getFileName());
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
            if (isUseUnrar()) {
                scanRarWithUnrar(file, index, builder);
            } else if (isUseSevenZip()) {
                scanRarWithSevenZip(file, index, builder);
            } else {
                throw e;
            }
        }
    }

    private boolean isUseSevenZip() {
        return !config.isForceUnrar() && ArchiveUtils.isSevenZipAvailable(config.getCustomSevenZipPath());
    }

    private boolean isUseUnrar() {
        return !config.isForceSevenZip() && ArchiveUtils.isUnrarAvailable(config.getCustomUnrarPath());
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
        ArchiveUtils.readRarWithUnrar(
                file,
                desiredEntryNames,
                (entry, processInputStream) -> processRarEntry(file, index, builder, entry, processInputStream));
    }

    private void scanRarWithSevenZip(
            Path file,
            int index,
            ImmutableList.Builder<Result> builder) throws Exception {
        ImmutableSet<String> desiredEntryNames =
                ArchiveUtils.listRarEntriesWithSevenZip(file)
                        .stream()
                        .filter(e -> !shouldSkip(file.resolve(e.getName()), index, e.getSize()))
                        .map(UnrarArchiveEntry::getName)
                        .collect(ImmutableSet.toImmutableSet());
        if (desiredEntryNames.isEmpty()) {
            return;
        }
        ArchiveUtils.readRarWithSevenZip(
                file,
                desiredEntryNames,
                (entry, processInputStream) -> processRarEntry(file, index, builder, entry, processInputStream));
    }

    private void processRarEntry(
            Path file,
            int index,
            ImmutableList.Builder<Result> builder,
            UnrarArchiveEntry unrarArchiveEntry,
            InputStream processInputStream) throws IOException {
        long size = unrarArchiveEntry.getSize();
        String name = unrarArchiveEntry.getName();
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
    }

    private void scanSevenZip(
            Path file,
            int index,
            ImmutableList.Builder<Result> builder) throws IOException {
        ArchiveUtils.readSevenZip(file, (sevenZFile, sevenZArchiveEntry) -> {
            long size = sevenZArchiveEntry.getSize();
            String name = normalizePath(sevenZArchiveEntry.getName());
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
            String name = normalizePath(tarArchiveEntry.getName());
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
        Checksum crc32 = threadLocalDataHolder.getCrc32();
        MessageDigest md5 = threadLocalDataHolder.getMd5();
        MessageDigest sha1 = threadLocalDataHolder.getSha1();
        MessageDigest sha256 = threadLocalDataHolder.getSha256();
        byte[] buffer = threadLocalDataHolder.getBuffer();
        crc32.reset();
        md5.reset();
        sha1.reset();
        long totalRead;
        if (size <= buffer.length && detectors != null) {
            totalRead = readAllAtOnce(path, index, size, function, crc32, md5, sha1, sha256, buffer);
        } else {
            totalRead = readInSteps(path, index, size, function, crc32, md5, sha1, sha256, buffer);
        }
        return new ProcessingResult(
                new Result.Digest(
                        Strings.padStart(Long.toHexString(crc32.getValue()), 8, '0'),
                        Hex.encodeHexString(md5.digest()),
                        Hex.encodeHexString(sha1.digest()),
                        Hex.encodeHexString(sha256.digest())),
                totalRead);
    }

    private long readAllAtOnce(
            Path path,
            int index,
            long size,
            TriFunction<byte[], Integer, Integer, Integer, IOException> function,
            Checksum crc32,
            MessageDigest md5,
            MessageDigest sha1,
            MessageDigest sha256, byte[] buffer) throws IOException {
        for (Listener listener : listeners) {
            listener.reportStart(index, path, size);
        }
        long totalRead = 0;
        int bytesRead;
        int bytesLeft;
        // Read the whole entry to the buffer and check headers
        while ((bytesLeft = toIntExact(Math.min(size - totalRead, buffer.length))) > 0
                && (bytesRead = function.apply(buffer, toIntExact(totalRead), bytesLeft)) > -1) {
            totalRead += bytesRead;
        }
        // Apply logic to detect headers
        // This is the only time we're going to read the file anyway
        // We can safely redefine the buffer variable here
        for (Detector detector : detectors) {
            boolean detected = false;
            for (Rule rule : detector.getRules()) {
                try {
                    byte[] newBuffer = rule.apply(buffer, toIntExact(totalRead), size);
                    // Identity check is enough here
                    if (newBuffer != buffer) {
                        detected = true;
                        buffer = newBuffer;
                        break;
                    }
                } catch (Exception e) {
                    log.error("Error while processing rule for '{}'", path, e);
                    for (Listener listener : listeners) {
                        listener.reportFailure(index, path, "Error while processing rule", e);
                    }
                }
            }
            if (detected) {
                log.info(
                        "Detected header using '{}' for '{}'",
                        detector.getName(),
                        path);
                break;
            }
        }
        int endRead = toIntExact(Math.min(totalRead, buffer.length));
        crc32.update(buffer, 0, endRead);
        md5.update(buffer, 0, endRead);
        sha1.update(buffer, 0, endRead);
        for (Listener listener : listeners) {
            listener.reportBytesRead(index, totalRead);
        }
        return totalRead;
    }

    private long readInSteps(
            Path path,
            int index,
            long size,
            TriFunction<byte[], Integer, Integer, Integer, IOException> function,
            Checksum crc32,
            MessageDigest md5,
            MessageDigest sha1,
            MessageDigest sha256,
            byte[] buffer) throws IOException {
        for (Listener listener : listeners) {
            listener.reportStart(index, path, size);
        }
        long totalRead = 0;
        long endOffset = size;
        int bytesRead;
        int bytesLeft;
        if (!detectors.isEmpty() && fileScannerParameters.isUseLazyDetector()) {
            long startOffset = 0;
            // Read as much as possible to the buffer and check headers
            while ((bytesLeft = toIntExact(buffer.length - totalRead)) > 0
                    && (bytesRead = function.apply(buffer, toIntExact(totalRead), bytesLeft)) > -1) {
                totalRead += bytesRead;
            }
            // Apply logic to detect headers
            for (Detector detector : detectors) {
                boolean detected = false;
                for (Rule rule : detector.getRules()) {
                    try {
                        if (rule.test(buffer, toIntExact(totalRead), size)) {
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
                        log.error("Error while processing rule for '{}'", path, e);
                        for (Listener listener : listeners) {
                            listener.reportFailure(
                                    index,
                                    path,
                                    "Error while processing rule",
                                    e);
                        }
                    }
                }
                if (detected) {
                    log.info(
                            "Detected header using '{}' for '{}'",
                            detector.getName(),
                            path);
                    break;
                }
            }
            totalRead = Math.min(totalRead, endOffset) - startOffset;
            int startOffsetInt = toIntExact(startOffset);
            int totalReadInt = toIntExact(totalRead);
            crc32.update(buffer, startOffsetInt, totalReadInt);
            md5.update(buffer, startOffsetInt, totalReadInt);
            sha1.update(buffer, startOffsetInt, totalReadInt);
            sha256.update(buffer, startOffsetInt, totalReadInt);
            for (Listener listener : listeners) {
                listener.reportBytesRead(index, totalRead);
            }
        }
        // Regular reading until we get to the end offset
        while ((bytesLeft = toIntExact(Math.min(endOffset - totalRead, buffer.length))) > 0
                && (bytesRead = function.apply(buffer, 0, bytesLeft)) > -1) {
            totalRead += bytesRead;
            crc32.update(buffer, 0, bytesRead);
            md5.update(buffer, 0, bytesRead);
            sha1.update(buffer, 0, bytesRead);
            sha256.update(buffer, 0, bytesRead);
            for (Listener listener : listeners) {
                listener.reportBytesRead(index, bytesRead);
            }
        }
        return totalRead;
    }

    @FunctionalInterface
    private interface TriFunction<K, L, M, N, E extends Throwable> {

        N apply(K k, L l, M m) throws E;
    }

}
