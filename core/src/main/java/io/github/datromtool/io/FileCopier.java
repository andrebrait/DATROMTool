package io.github.datromtool.io;

import com.github.junrar.exception.RarException;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.io.logging.FileCopierLoggingListener;
import io.github.datromtool.util.ArchiveUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.*;
import org.apache.commons.compress.compressors.lzma.LZMAUtils;
import org.apache.commons.compress.compressors.xz.XZUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.InvalidParameterException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.github.datromtool.util.ArchiveUtils.normalizePath;
import static java.util.Objects.requireNonNull;

@Slf4j
public final class FileCopier {

    /**
     * The upper bound of the 32-bit unix time, the "year 2038 problem".
     */
    private static final long UPPER_UNIXTIME_BOUND = 0x7fffffff;

    /**
     * An empty path. Should resolve to the current directory.
     */
    private static final Path EMPTY_PATH = Paths.get("");

    public static abstract class Spec {

        private Spec() {
        }
    }

    @Builder
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    public static class CopySpec extends Spec {

        @NonNull
        Path from;
        @NonNull
        Path to;
    }

    @Builder
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    public static class ExtractionSpec extends Spec {

        @Builder
        @Value
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static class InternalSpec {

            @NonNull
            String from;
            @NonNull
            Path to;
        }

        @NonNull
        ArchiveType fromType;
        @NonNull
        Path from;
        @NonNull
        ImmutableSet<InternalSpec> internalSpecs;
    }

    @Builder
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    public static class CompressionSpec extends Spec {

        @Builder
        @Value
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static class InternalSpec {

            @NonNull
            Path from;
            @NonNull
            String to;
        }

        @NonNull
        ArchiveType toType;
        @NonNull
        Path to;
        @NonNull
        ImmutableSet<InternalSpec> internalSpecs;
    }

    @Builder
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    public static class ArchiveCopySpec extends Spec {

        @Builder
        @Value
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static class InternalSpec {

            @NonNull
            String from;
            @NonNull
            String to;
        }

        @NonNull
        ArchiveType fromType;
        @NonNull
        ArchiveType toType;
        @NonNull
        Path from;
        @NonNull
        Path to;
        @NonNull
        ImmutableSet<InternalSpec> internalSpecs;
    }

    public interface Listener {

        void init(int numThreads);

        void reportTotalItems(int totalItems);

        void reportStart(int thread, Path source, Path destination, long bytes);

        void reportBytesCopied(int thread, long bytes);

        void reportFailure(int thread, Path source, Path destination, String message, Throwable cause);

        void reportFinish(int thread, Path source, Path destination);

        void reportAllFinished();
    }

    private final AppConfig.FileCopierConfig config;
    private final ImmutableList<Listener> listeners;
    private final ThreadLocal<byte[]> threadLocalBuffer;

    public FileCopier(
            @Nonnull AppConfig.FileCopierConfig config,
            @Nonnull List<Listener> listeners) {
        this.config = config;
        this.listeners = processListenerList(requireNonNull(listeners));
        this.threadLocalBuffer = ThreadLocal.withInitial(() -> new byte[config.getBufferSize()]);
    }

    @Nonnull
    private static ImmutableList<Listener> processListenerList(@Nonnull List<Listener> listeners) {
        if (listeners.stream().noneMatch(FileCopierLoggingListener.class::isInstance)) {
            return ImmutableList.<Listener>builder().add(new FileCopierLoggingListener()).addAll(listeners).build();
        }
        return ImmutableList.copyOf(listeners);
    }

    public void copy(Set<? extends Spec> definitions) {
        log.debug("Copying selected files: {}", definitions);
        ExecutorService executorService = Executors.newFixedThreadPool(
                config.getThreads(),
                new IndexedThreadFactory(log, "COPIER"));
        if (!LZMAUtils.isLZMACompressionAvailable()) {
            log.warn("LZMA compression support is disabled");
        }
        if (!XZUtils.isXZCompressionAvailable()) {
            log.warn("XZ compression support is disabled");
        }
        for (Listener listener : listeners) {
            listener.init(config.getThreads());
            listener.reportTotalItems(definitions.size());
        }
        definitions.stream()
                .map(d -> executorService.submit(() -> copy(d)))
                .collect(ImmutableList.toImmutableList())
                .forEach(this::waitForCompletion);
        executorService.shutdownNow();
        for (Listener listener : listeners) {
            listener.reportAllFinished();
        }
    }

    private void waitForCompletion(Future<?> future) {
        try {
            future.get();
        } catch (Exception e) {
            log.error("Unexpected exception thrown", e);
        }
    }

    private void copy(Spec spec) {
        if (spec instanceof CopySpec) {
            copy((CopySpec) spec);
        } else if (spec instanceof ExtractionSpec) {
            copy((ExtractionSpec) spec);
        } else if (spec instanceof CompressionSpec) {
            copy((CompressionSpec) spec);
        } else if (spec instanceof ArchiveCopySpec) {
            copy((ArchiveCopySpec) spec);
        } else {
            throw new InvalidParameterException("Cannot handle " + spec);
        }
    }

    private static int getThreadIndex() {
        return ((IndexedThread) Thread.currentThread()).getIndex();
    }

    private void copy(CopySpec spec) {
        int index = getThreadIndex();
        for (Listener listener : listeners) {
            listener.reportStart(index, spec.getFrom(), spec.getTo(), 1);
        }
        try {
            BasicFileAttributes fromAttrib = Files.readAttributes(spec.getFrom(), BasicFileAttributes.class);
            try (InputStream inputStream = Files.newInputStream(spec.getFrom())) {
                try (OutputStream outputStream = Files.newOutputStream(spec.getTo())) {
                    copyWithProgress(
                            index,
                            fromAttrib.size(),
                            spec.getFrom(),
                            spec.getTo(),
                            inputStream::read,
                            outputStream::write);
                }
            }
            BasicFileAttributeView toAttrib = Files.getFileAttributeView(spec.getTo(), BasicFileAttributeView.class);
            toAttrib.setTimes(fromAttrib.lastModifiedTime(), fromAttrib.lastAccessTime(), fromAttrib.creationTime());
        } catch (Exception e) {
            log.error("Could not copy '{}' to '{}'", spec.getFrom(), spec.getTo(), e);
            for (Listener listener : listeners) {
                listener.reportFailure(index, spec.getFrom(), spec.getTo(), "Could not copy files", e);
            }
        } finally {
            for (Listener listener : listeners) {
                listener.reportFinish(index, spec.getFrom(), spec.getTo());
            }
        }
    }

    private void copy(ExtractionSpec spec) {
        int index = getThreadIndex();
        for (Listener listener : listeners) {
            listener.reportStart(index, spec.getFrom(), EMPTY_PATH, 1);
        }
        try {
            switch (spec.getFromType()) {
                case ZIP:
                    extractZipEntries(spec, index);
                    return;
                case RAR:
                    extractRarEntries(spec, index);
                    return;
                case SEVEN_ZIP:
                    extractSevenZipEntries(spec, index);
                    return;
                case TAR:
                case TAR_BZ2:
                case TAR_GZ:
                case TAR_LZ4:
                case TAR_LZMA:
                case TAR_XZ:
                    extractTarEntries(spec, index);
            }
        } catch (UnsupportedRarV5Exception e) {
            log.error("Could not extract '{}'. RAR5 is not supported yet", spec.getFrom());
            for (Listener listener : listeners) {
                listener.reportFailure(
                        index,
                        spec.getFrom(),
                        EMPTY_PATH,
                        "Could not extract archive. RAR5 is not supported yet",
                        e);
            }
        } catch (Exception e) {
            log.error("Could not extract '{}'", spec.getFrom(), e);
            for (Listener listener : listeners) {
                listener.reportFailure(
                        index,
                        spec.getFrom(),
                        EMPTY_PATH,
                        "Could not extract archive",
                        e);
            }
        } finally {
            for (Listener listener : listeners) {
                listener.reportFinish(index, spec.getFrom(), EMPTY_PATH);
            }
        }
    }

    private void copy(CompressionSpec spec) {
        int index = getThreadIndex();
        for (Listener listener : listeners) {
            listener.reportStart(index, EMPTY_PATH, spec.getTo(), 1);
        }
        try {
            switch (spec.getToType()) {
                case ZIP:
                    compressZipEntries(spec, index);
                    return;
                case RAR:
                    throw new UnsupportedOperationException("RAR compression is not supported");
                case SEVEN_ZIP:
                    compressSevenZipEntries(spec, index);
                    return;
                case TAR:
                case TAR_BZ2:
                case TAR_GZ:
                case TAR_LZ4:
                case TAR_LZMA:
                case TAR_XZ:
                    compressTarEntries(spec, index);
            }
        } catch (Exception e) {
            log.error("Could not compress files to '{}'", spec.getTo(), e);
            for (Listener listener : listeners) {
                listener.reportFailure(index, EMPTY_PATH, spec.getTo(), "Could not compress files", e);
            }
        } finally {
            for (Listener listener : listeners) {
                listener.reportFinish(index, EMPTY_PATH, spec.getTo());
            }
        }
    }

    private void copy(ArchiveCopySpec spec) {
        int index = getThreadIndex();
        for (Listener listener : listeners) {
            listener.reportStart(index, spec.getFrom(), spec.getTo(), 1);
        }
        try {
            switch (spec.getFromType()) {
                case ZIP:
                    fromZipToArchive(spec, index);
                    return;
                case RAR:
                    fromRarToArchive(spec, index);
                    return;
                case SEVEN_ZIP:
                    fromSevenZipToArchive(spec, index);
                    return;
                case TAR:
                case TAR_BZ2:
                case TAR_GZ:
                case TAR_LZ4:
                case TAR_LZMA:
                case TAR_XZ:
                    fromTarToArchive(spec, index);
            }
        } catch (UnsupportedRarV5Exception e) {
            log.error(
                    "Could not copy contents of '{}' to '{}'. RAR5 is not natively supported yet.",
                    spec.getFrom(),
                    spec.getTo());
            for (Listener listener : listeners) {
                listener.reportFailure(
                        index,
                        spec.getFrom(),
                        spec.getTo(),
                        "Could not copy contents of archive. RAR5 is not supported yet",
                        e);
            }
        } catch (Exception e) {
            log.error(
                    "Could not copy contents of '{}' to '{}'",
                    spec.getFrom(),
                    spec.getTo(),
                    e);
            for (Listener listener : listeners) {
                listener.reportFailure(index, spec.getFrom(), spec.getTo(), "Could not copy contents of archive", e);
            }
        } finally {
            for (Listener listener : listeners) {
                listener.reportFinish(index, spec.getFrom(), spec.getTo());
            }
        }
    }

    private void extractZipEntries(ExtractionSpec spec, int index) throws IOException {
        ArchiveUtils.readZip(spec.getFrom(), (zipFile, zipArchiveEntry) -> {
            String name = zipArchiveEntry.getName();
            ExtractionSpec.InternalSpec internal = findInternalSpec(spec, name);
            if (internal == null) {
                return;
            }
            try (InputStream inputStream = zipFile.getInputStream(zipArchiveEntry)) {
                Path to = internal.getTo();
                try (OutputStream outputStream = Files.newOutputStream(to)) {
                    Path source = spec.getFrom().resolve(name);
                    long size = zipArchiveEntry.getSize();
                    copyWithProgress(
                            index,
                            size,
                            source,
                            to,
                            inputStream::read,
                            outputStream::write);
                } catch (FileAlreadyExistsException e) {
                    throw e;
                } catch (IOException e) {
                    Files.delete(to);
                    throw e;
                }
                BasicFileAttributeView toAttrib = Files.getFileAttributeView(to, BasicFileAttributeView.class);
                toAttrib.setTimes(zipArchiveEntry.getLastModifiedTime(), zipArchiveEntry.getLastAccessTime(), zipArchiveEntry.getCreationTime());
            }
        });
    }

    private void extractRarEntries(ExtractionSpec spec, int index) throws Exception {
        try {
            ArchiveUtils.readRar(spec.getFrom(), (archive, fileHeader) -> {
                String name = normalizePath(fileHeader.getFileName());
                ExtractionSpec.InternalSpec internal = findInternalSpec(spec, name);
                if (internal == null) {
                    return;
                }
                try (InputStream inputStream = archive.getInputStream(fileHeader)) {
                    Path to = internal.getTo();
                    try (OutputStream outputStream = Files.newOutputStream(to)) {
                        Path source = spec.getFrom().resolve(name);
                        long size = fileHeader.getFullUnpackSize();
                        copyWithProgress(
                                index,
                                size,
                                source,
                                to,
                                inputStream::read,
                                outputStream::write);
                    } catch (FileAlreadyExistsException e) {
                        throw e;
                    } catch (IOException e) {
                        Files.delete(to);
                        throw e;
                    }
                    BasicFileAttributeView toAttrib = Files.getFileAttributeView(to, BasicFileAttributeView.class);
                    toAttrib.setTimes(from(fileHeader.getMTime()), from(fileHeader.getATime()), from(fileHeader.getCTime()));
                }
            });
        } catch (UnsupportedRarV5Exception e) {
            if (isUseUnrar()) {
                extractRarEntriesWithUnrar(spec, index);
            } else if (isUseSevenZip()) {
                extractRarEntriesWithSevenZip(spec, index);
            } else {
                throw e;
            }
        }
    }

    private void extractRarEntriesWithUnrar(ExtractionSpec spec, int index) throws Exception {
        ImmutableSet<String> desiredEntryNames = spec.getInternalSpecs().stream()
                .map(ExtractionSpec.InternalSpec::getFrom)
                .collect(ImmutableSet.toImmutableSet());
        ArchiveUtils.readRarWithUnrar(
                spec.getFrom(),
                desiredEntryNames,
                (entry, processInputStream) -> processRarEntry(spec, index, entry, processInputStream));
    }

    private void extractRarEntriesWithSevenZip(ExtractionSpec spec, int index) throws Exception {
        ImmutableSet<String> desiredEntryNames = spec.getInternalSpecs().stream()
                .map(ExtractionSpec.InternalSpec::getFrom)
                .collect(ImmutableSet.toImmutableSet());
        ArchiveUtils.readRarWithSevenZip(
                spec.getFrom(),
                desiredEntryNames,
                (entry, processInputStream) -> processRarEntry(spec, index, entry, processInputStream));
    }

    private void processRarEntry(
            ExtractionSpec spec,
            int index,
            UnrarArchiveEntry entry,
            InputStream processInputStream) throws IOException {
        String name = entry.getName();
        ExtractionSpec.InternalSpec internal = findInternalSpec(spec, name);
        if (internal == null) {
            return;
        }
        Path to = internal.getTo();
        try (OutputStream outputStream = Files.newOutputStream(to)) {
            Path source = spec.getFrom().resolve(name);
            long size = entry.getSize();
            copyWithProgress(
                    index,
                    size,
                    source,
                    to,
                    processInputStream::read,
                    outputStream::write);
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(to);
            throw e;
        }
        BasicFileAttributeView toAttrib = Files.getFileAttributeView(to, BasicFileAttributeView.class);
        toAttrib.setTimes(from(entry.getModificationTime()), null, null);
    }

    private void extractSevenZipEntries(ExtractionSpec spec, int index) throws IOException {
        ArchiveUtils.readSevenZip(spec.getFrom(), (sevenZFile, sevenZArchiveEntry) -> {
            String name = sevenZArchiveEntry.getName();
            ExtractionSpec.InternalSpec internal = findInternalSpec(spec, name);
            if (internal == null) {
                return;
            }
            Path to = internal.getTo();
            try (OutputStream outputStream = Files.newOutputStream(to)) {
                Path source = spec.getFrom().resolve(name);
                long size = sevenZArchiveEntry.getSize();
                copyWithProgress(index, size, source, to, sevenZFile::read, outputStream::write);
            } catch (FileAlreadyExistsException e) {
                throw e;
            } catch (IOException e) {
                Files.delete(to);
                throw e;
            }
            BasicFileAttributeView toAttrib = Files.getFileAttributeView(to, BasicFileAttributeView.class);
            toAttrib.setTimes(
                    sevenZArchiveEntry.getHasLastModifiedDate() ? from(sevenZArchiveEntry.getLastModifiedDate()) : null,
                    sevenZArchiveEntry.getHasAccessDate() ? from(sevenZArchiveEntry.getAccessDate()) : null,
                    sevenZArchiveEntry.getHasCreationDate() ? from(sevenZArchiveEntry.getCreationDate()) : null);
        });
    }

    private void extractTarEntries(ExtractionSpec spec, int index) throws IOException {
        ArchiveUtils.readTar(
                spec.getFromType(),
                spec.getFrom(),
                (tarArchiveEntry, tarArchiveInputStream) -> {
                    String name = tarArchiveEntry.getName();
                    ExtractionSpec.InternalSpec internal = findInternalSpec(spec, name);
                    if (internal == null) {
                        return;
                    }
                    Path to = internal.getTo();
                    try (OutputStream outputStream = Files.newOutputStream(to)) {
                        Path source = spec.getFrom().resolve(name);
                        long size = tarArchiveEntry.getRealSize();
                        copyWithProgress(
                                index,
                                size,
                                source,
                                to,
                                tarArchiveInputStream::read,
                                outputStream::write);
                    } catch (FileAlreadyExistsException e) {
                        throw e;
                    } catch (IOException e) {
                        Files.delete(to);
                        throw e;
                    }
                    BasicFileAttributeView toAttrib = Files.getFileAttributeView(to, BasicFileAttributeView.class);
                    toAttrib.setTimes(from(tarArchiveEntry.getLastModifiedDate()), null, null);
                });
    }

    private void compressZipEntries(CompressionSpec spec, int index) throws IOException {
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(spec.getTo())) {
            for (CompressionSpec.InternalSpec internal : spec.getInternalSpecs()) {
                Path source = internal.getFrom();
                try (InputStream inputStream = Files.newInputStream(source)) {
                    ZipArchiveEntry archiveEntry = (ZipArchiveEntry) zipArchiveOutputStream.createArchiveEntry(
                            source.toFile(),
                            internal.getTo());
                    BasicFileAttributes fromAttrib = Files.readAttributes(source, BasicFileAttributes.class);
                    setTimes(fromAttrib.lastModifiedTime(), fromAttrib.lastAccessTime(), fromAttrib.creationTime(), archiveEntry);
                    Path destination = spec.getTo().resolve(internal.getTo());
                    zipArchiveOutputStream.putArchiveEntry(archiveEntry);
                    copyWithProgress(
                            index,
                            fromAttrib.size(),
                            source,
                            destination,
                            inputStream::read,
                            zipArchiveOutputStream::write);
                    zipArchiveOutputStream.closeArchiveEntry();
                }
            }
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void compressSevenZipEntries(CompressionSpec spec, int index) throws IOException {
        try (SevenZOutputFile sevenZOutputFile = new SevenZOutputFile(spec.getTo().toFile())) {
            for (CompressionSpec.InternalSpec internal : spec.getInternalSpecs()) {
                Path source = internal.getFrom();
                try (InputStream inputStream = Files.newInputStream(source)) {
                    SevenZArchiveEntry archiveEntry =
                            sevenZOutputFile.createArchiveEntry(source.toFile(), internal.getTo());
                    BasicFileAttributes fromAttrib = Files.readAttributes(source, BasicFileAttributes.class);
                    archiveEntry.setAccessDate(toDate(fromAttrib.lastAccessTime()));
                    archiveEntry.setCreationDate(toDate(fromAttrib.creationTime()));
                    Path destination = spec.getTo().resolve(internal.getFrom());
                    sevenZOutputFile.putArchiveEntry(archiveEntry);
                    copyWithProgress(
                            index,
                            fromAttrib.size(),
                            source,
                            destination,
                            inputStream::read,
                            sevenZOutputFile::write);
                    sevenZOutputFile.closeArchiveEntry();
                }
            }
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void compressTarEntries(CompressionSpec spec, int index) throws IOException {
        OutputStream outputStream = ArchiveUtils.outputStreamForTar(spec.getToType(), spec.getTo());
        if (outputStream == null) {
            return;
        }
        try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(outputStream)) {
            for (CompressionSpec.InternalSpec internal : spec.getInternalSpecs()) {
                Path source = internal.getFrom();
                try (InputStream inputStream = Files.newInputStream(source)) {
                    ArchiveEntry archiveEntry =
                            tarOutputStream.createArchiveEntry(source.toFile(), internal.getTo());
                    Path destination = spec.getTo().resolve(internal.getTo());
                    tarOutputStream.putArchiveEntry(archiveEntry);
                    copyWithProgress(
                            index,
                            archiveEntry.getSize(),
                            source,
                            destination,
                            inputStream::read,
                            tarOutputStream::write);
                    tarOutputStream.closeArchiveEntry();
                }
            }
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromZipToArchive(ArchiveCopySpec spec, int index) throws IOException {
        switch (spec.getToType()) {
            case ZIP:
                if (config.isAllowRawZipCopy()) {
                    fromZipToZipRaw(spec, index);
                } else {
                    fromZipToZip(spec, index);
                }
                return;
            case RAR:
                throw new UnsupportedOperationException("RAR compression is not supported");
            case SEVEN_ZIP:
                fromZipToSevenZip(spec, index);
                return;
            case TAR:
            case TAR_BZ2:
            case TAR_GZ:
            case TAR_LZ4:
            case TAR_LZMA:
            case TAR_XZ:
                fromZipToTar(spec, index);
        }
    }

    private void fromRarToArchive(ArchiveCopySpec spec, int index) throws Exception {
        switch (spec.getToType()) {
            case ZIP:
                fromRarToZip(spec, index);
                return;
            case RAR:
                throw new UnsupportedOperationException("RAR compression is not supported");
            case SEVEN_ZIP:
                fromRarToSevenZip(spec, index);
                return;
            case TAR:
            case TAR_BZ2:
            case TAR_GZ:
            case TAR_LZ4:
            case TAR_LZMA:
            case TAR_XZ:
                fromRarToTar(spec, index);
        }
    }

    private void fromSevenZipToArchive(ArchiveCopySpec spec, int index) throws IOException {
        switch (spec.getToType()) {
            case ZIP:
                fromSevenZipToZip(spec, index);
                return;
            case RAR:
                throw new UnsupportedOperationException("RAR compression is not supported");
            case SEVEN_ZIP:
                fromSevenZipToSevenZip(spec, index);
                return;
            case TAR:
            case TAR_BZ2:
            case TAR_GZ:
            case TAR_LZ4:
            case TAR_LZMA:
            case TAR_XZ:
                fromSevenZipToTar(spec, index);
        }
    }

    private void fromTarToArchive(ArchiveCopySpec spec, int index) throws IOException {
        switch (spec.getToType()) {
            case ZIP:
                fromTarToZip(spec, index);
                return;
            case RAR:
                throw new UnsupportedOperationException("RAR compression is not supported");
            case SEVEN_ZIP:
                fromTarToSevenZip(spec, index);
                return;
            case TAR:
            case TAR_BZ2:
            case TAR_GZ:
            case TAR_LZ4:
            case TAR_LZMA:
            case TAR_XZ:
                fromTarToTar(spec, index);
        }
    }

    private void fromZipToZipRaw(ArchiveCopySpec spec, int index) throws IOException {
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(spec.getTo())) {
            ArchiveUtils.readZip(spec.getFrom(), (zipFile, zipArchiveEntry) -> {
                String name = zipArchiveEntry.getName();
                ArchiveCopySpec.InternalSpec internal = findInternalSpec(spec, name);
                if (internal == null) {
                    return;
                }
                Path source = spec.getFrom().resolve(name);
                Path to = spec.getTo().resolve(name);
                if (!name.equals(internal.getTo())) {
                    log.warn(
                            "Cannot rename files inside ZIPs when using raw copy. "
                                    + "Writing '{}' as '{}'",
                            spec.getTo().resolve(internal.getTo()),
                            to);
                }
                for (Listener listener : listeners) {
                    listener.reportStart(index, source, to, zipArchiveEntry.getSize());
                }
                zipArchiveOutputStream.addRawArchiveEntry(
                        zipArchiveEntry,
                        zipFile.getRawInputStream(zipArchiveEntry));
                for (Listener listener : listeners) {
                    listener.reportBytesCopied(index, zipArchiveEntry.getSize());
                }
            });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromZipToZip(ArchiveCopySpec spec, int index) throws IOException {
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(spec.getTo())) {
            ArchiveUtils.readZip(
                    spec.getFrom(),
                    (zipFile, zipArchiveEntry) -> {
                        String name = zipArchiveEntry.getName();
                        ArchiveCopySpec.InternalSpec internal = findInternalSpec(spec, name);
                        if (internal == null) {
                            return;
                        }
                        try (InputStream inputStream = zipFile.getInputStream(zipArchiveEntry)) {
                            toZip(
                                    index,
                                    inputStream::read,
                                    zipArchiveOutputStream,
                                    spec,
                                    internal,
                                    name,
                                    zipArchiveEntry.getLastModifiedTime(),
                                    zipArchiveEntry.getLastAccessTime(),
                                    zipArchiveEntry.getCreationTime(),
                                    zipArchiveEntry.getSize());
                        }
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromZipToSevenZip(ArchiveCopySpec spec, int index) throws IOException {
        try (SevenZOutputFile sevenZOutputFile = new SevenZOutputFile(spec.getTo().toFile())) {
            ArchiveUtils.readZip(
                    spec.getFrom(),
                    (zipFile, zipArchiveEntry) -> {
                        String name = zipArchiveEntry.getName();
                        ArchiveCopySpec.InternalSpec internal = findInternalSpec(spec, name);
                        if (internal == null) {
                            return;
                        }
                        try (InputStream inputStream = zipFile.getInputStream(zipArchiveEntry)) {
                            toSevenZip(
                                    index,
                                    inputStream::read,
                                    sevenZOutputFile,
                                    spec,
                                    internal,
                                    name,
                                    toDate(zipArchiveEntry.getLastModifiedTime()),
                                    toDate(zipArchiveEntry.getLastAccessTime()),
                                    toDate(zipArchiveEntry.getCreationTime()),
                                    zipArchiveEntry.getSize());
                        }
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromZipToTar(ArchiveCopySpec spec, int index) throws IOException {
        OutputStream outputStream = ArchiveUtils.outputStreamForTar(spec.getToType(), spec.getTo());
        if (outputStream == null) {
            return;
        }
        try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(outputStream)) {
            ArchiveUtils.readZip(
                    spec.getFrom(),
                    (zipFile, zipArchiveEntry) -> {
                        String name = zipArchiveEntry.getName();
                        ArchiveCopySpec.InternalSpec internal = findInternalSpec(spec, name);
                        if (internal == null) {
                            return;
                        }
                        try (InputStream inputStream = zipFile.getInputStream(zipArchiveEntry)) {
                            toTar(
                                    index,
                                    inputStream::read,
                                    tarOutputStream,
                                    spec,
                                    internal,
                                    name,
                                    toDate(zipArchiveEntry.getLastModifiedTime()),
                                    zipArchiveEntry.getSize());
                        }
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromRarToZip(ArchiveCopySpec spec, int index) throws Exception {
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(spec.getTo())) {
            ArchiveUtils.readRar(
                    spec.getFrom(),
                    (archive, fileHeader) -> {
                        String name = normalizePath(fileHeader.getFileName());
                        ArchiveCopySpec.InternalSpec internal = findInternalSpec(spec, name);
                        if (internal == null) {
                            return;
                        }
                        try (InputStream inputStream = archive.getInputStream(fileHeader)) {
                            toZip(
                                    index,
                                    inputStream::read,
                                    zipArchiveOutputStream,
                                    spec,
                                    internal,
                                    name,
                                    from(fileHeader.getMTime()),
                                    from(fileHeader.getATime()),
                                    from(fileHeader.getCTime()),
                                    fileHeader.getFullUnpackSize());
                        }
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (UnsupportedRarV5Exception e) {
            if (isUseUnrar()) {
                fromRarWithUnrarToZip(spec, index);
            } else if (isUseSevenZip()) {
                fromRarWithSevenZipToZip(spec, index);
            } else {
                Files.delete(spec.getTo());
                throw e;
            }
        } catch (IOException | RarException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromRarWithUnrarToZip(ArchiveCopySpec spec, int index) throws Exception {
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(spec.getTo())) {
            ImmutableSet<String> desiredEntryNames = getInternalSources(spec);
            ArchiveUtils.readRarWithUnrar(
                    spec.getFrom(),
                    desiredEntryNames,
                    (entry, processInputStream) -> fromRarEntryToZip(spec, index, zipArchiveOutputStream, entry, processInputStream));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException | RarException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromRarWithSevenZipToZip(ArchiveCopySpec spec, int index) throws Exception {
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(spec.getTo())) {
            ImmutableSet<String> desiredEntryNames = getInternalSources(spec);
            ArchiveUtils.readRarWithSevenZip(
                    spec.getFrom(),
                    desiredEntryNames,
                    (entry, processInputStream) -> fromRarEntryToZip(spec, index, zipArchiveOutputStream, entry, processInputStream));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException | RarException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromRarEntryToZip(
            ArchiveCopySpec spec,
            int index,
            ZipArchiveOutputStream zipArchiveOutputStream,
            UnrarArchiveEntry entry,
            InputStream processInputStream) throws IOException {
        String name = entry.getName();
        ArchiveCopySpec.InternalSpec internal = findInternalSpec(spec, name);
        if (internal == null) {
            return;
        }
        toZip(
                index,
                processInputStream::read,
                zipArchiveOutputStream,
                spec,
                internal,
                name,
                from(entry.getModificationTime()),
                null,
                null,
                entry.getSize());
    }

    private void fromRarToSevenZip(ArchiveCopySpec spec, int index) throws Exception {
        try (SevenZOutputFile sevenZOutputFile = new SevenZOutputFile(spec.getTo().toFile())) {
            ArchiveUtils.readRar(
                    spec.getFrom(),
                    (archive, fileHeader) -> {
                        String name = normalizePath(fileHeader.getFileName());
                        ArchiveCopySpec.InternalSpec internal = findInternalSpec(spec, name);
                        if (internal == null) {
                            return;
                        }
                        try (InputStream inputStream = archive.getInputStream(fileHeader)) {
                            toSevenZip(
                                    index,
                                    inputStream::read,
                                    sevenZOutputFile,
                                    spec,
                                    internal,
                                    name,
                                    fileHeader.getMTime(),
                                    fileHeader.getATime(),
                                    fileHeader.getCTime(),
                                    fileHeader.getFullUnpackSize());
                        }
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (UnsupportedRarV5Exception e) {
            if (isUseUnrar()) {
                fromRarWithUnrarToSevenZip(spec, index);
            } else if (isUseSevenZip()) {
                fromRarWithSevenZipToSevenZip(spec, index);
            } else {
                Files.delete(spec.getTo());
                throw e;
            }
        } catch (IOException | RarException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromRarWithUnrarToSevenZip(ArchiveCopySpec spec, int index) throws Exception {
        try (SevenZOutputFile sevenZOutputFile = new SevenZOutputFile(spec.getTo().toFile())) {
            ImmutableSet<String> desiredEntryNames = getInternalSources(spec);
            ArchiveUtils.readRarWithUnrar(
                    spec.getFrom(),
                    desiredEntryNames,
                    (entry, processInputStream) -> fromRarEntryToSevenZip(spec, index, sevenZOutputFile, entry, processInputStream));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException | RarException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromRarWithSevenZipToSevenZip(ArchiveCopySpec spec, int index) throws Exception {
        try (SevenZOutputFile sevenZOutputFile = new SevenZOutputFile(spec.getTo().toFile())) {
            ImmutableSet<String> desiredEntryNames = getInternalSources(spec);
            ArchiveUtils.readRarWithSevenZip(
                    spec.getFrom(),
                    desiredEntryNames,
                    (entry, processInputStream) -> fromRarEntryToSevenZip(spec, index, sevenZOutputFile, entry, processInputStream));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException | RarException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromRarEntryToSevenZip(
            ArchiveCopySpec spec,
            int index,
            SevenZOutputFile sevenZOutputFile,
            UnrarArchiveEntry entry,
            InputStream processInputStream) throws IOException {
        String name = entry.getName();
        ArchiveCopySpec.InternalSpec internal = findInternalSpec(spec, name);
        if (internal == null) {
            return;
        }
        toSevenZip(
                index,
                processInputStream::read,
                sevenZOutputFile,
                spec,
                internal,
                name,
                toDate(entry.getModificationTime()),
                null,
                null,
                entry.getSize());
    }

    private void fromRarToTar(ArchiveCopySpec spec, int index) throws Exception {
        OutputStream outputStream = ArchiveUtils.outputStreamForTar(spec.getToType(), spec.getTo());
        if (outputStream == null) {
            return;
        }
        try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(outputStream)) {
            ArchiveUtils.readRar(
                    spec.getFrom(),
                    (archive, fileHeader) -> {
                        String name = normalizePath(fileHeader.getFileName());
                        ArchiveCopySpec.InternalSpec internal = findInternalSpec(spec, name);
                        if (internal == null) {
                            return;
                        }
                        try (InputStream inputStream = archive.getInputStream(fileHeader)) {
                            toTar(
                                    index,
                                    inputStream::read,
                                    tarOutputStream,
                                    spec,
                                    internal,
                                    name,
                                    fileHeader.getMTime(),
                                    fileHeader.getFullUnpackSize());
                        }
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (UnsupportedRarV5Exception e) {
            if (isUseUnrar()) {
                fromRarWithUnrarToTar(spec, index);
            } else if (isUseSevenZip()) {
                fromRarWithSevenZipToTar(spec, index);
            } else {
                Files.delete(spec.getTo());
                throw e;
            }
        } catch (IOException | RarException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private boolean isUseSevenZip() {
        return !config.isForceUnrar() && ArchiveUtils.isSevenZipAvailable(config.getCustomSevenZipPath());
    }

    private boolean isUseUnrar() {
        return !config.isForceSevenZip() && ArchiveUtils.isUnrarAvailable(config.getCustomUnrarPath());
    }

    private void fromRarWithUnrarToTar(
            ArchiveCopySpec spec,
            int index) throws Exception {
        OutputStream outputStream = ArchiveUtils.outputStreamForTar(spec.getToType(), spec.getTo());
        if (outputStream == null) {
            return;
        }
        try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(outputStream)) {
            ImmutableSet<String> desiredEntryNames = getInternalSources(spec);
            ArchiveUtils.readRarWithUnrar(
                    spec.getFrom(),
                    desiredEntryNames,
                    (entry, processInputStream) -> fromRarEntryToTar(spec, index, tarOutputStream, entry, processInputStream));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException | RarException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromRarWithSevenZipToTar(
            ArchiveCopySpec spec,
            int index) throws Exception {
        OutputStream outputStream = ArchiveUtils.outputStreamForTar(spec.getToType(), spec.getTo());
        if (outputStream == null) {
            return;
        }
        try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(outputStream)) {
            ImmutableSet<String> desiredEntryNames = getInternalSources(spec);
            ArchiveUtils.readRarWithSevenZip(
                    spec.getFrom(),
                    desiredEntryNames,
                    (entry, processInputStream) -> fromRarEntryToTar(spec, index, tarOutputStream, entry, processInputStream));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException | RarException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromRarEntryToTar(
            ArchiveCopySpec spec,
            int index,
            TarArchiveOutputStream tarOutputStream,
            UnrarArchiveEntry entry,
            InputStream processInputStream) throws IOException {
        String name = entry.getName();
        ArchiveCopySpec.InternalSpec internal = findInternalSpec(spec, name);
        if (internal == null) {
            return;
        }
        toTar(
                index,
                processInputStream::read,
                tarOutputStream,
                spec,
                internal,
                name,
                toDate(entry.getModificationTime()),
                entry.getSize());
    }

    private void fromSevenZipToZip(ArchiveCopySpec spec, int index) throws IOException {
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(spec.getTo())) {
            ArchiveUtils.readSevenZip(
                    spec.getFrom(),
                    (sevenZFile, sevenZArchiveEntry) -> toZip(
                            index,
                            sevenZFile::read,
                            zipArchiveOutputStream,
                            spec,
                            sevenZArchiveEntry.getName(),
                            sevenZArchiveEntry.getHasLastModifiedDate()
                                    ? from(sevenZArchiveEntry.getLastModifiedDate())
                                    : null,
                            sevenZArchiveEntry.getHasAccessDate()
                                    ? from(sevenZArchiveEntry.getAccessDate())
                                    : null,
                            sevenZArchiveEntry.getHasCreationDate()
                                    ? from(sevenZArchiveEntry.getCreationDate())
                                    : null,
                            sevenZArchiveEntry.getSize()));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromSevenZipToSevenZip(ArchiveCopySpec spec, int index) throws IOException {
        try (SevenZOutputFile sevenZOutputFile =
                new SevenZOutputFile(spec.getTo().toFile())) {
            ArchiveUtils.readSevenZip(
                    spec.getFrom(),
                    (sevenZFile, sevenZArchiveEntry) -> toSevenZip(
                            index,
                            sevenZFile::read,
                            sevenZOutputFile,
                            spec,
                            sevenZArchiveEntry.getName(),
                            sevenZArchiveEntry.getHasLastModifiedDate()
                                    ? sevenZArchiveEntry.getLastModifiedDate()
                                    : null,
                            sevenZArchiveEntry.getHasAccessDate()
                                    ? sevenZArchiveEntry.getAccessDate()
                                    : null,
                            sevenZArchiveEntry.getHasCreationDate()
                                    ? sevenZArchiveEntry.getCreationDate()
                                    : null,
                            sevenZArchiveEntry.getSize()));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromSevenZipToTar(ArchiveCopySpec spec, int index) throws IOException {
        OutputStream outputStream = ArchiveUtils.outputStreamForTar(spec.getToType(), spec.getTo());
        if (outputStream == null) {
            return;
        }
        try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(outputStream)) {
            ArchiveUtils.readSevenZip(
                    spec.getFrom(),
                    (sevenZFile, sevenZArchiveEntry) -> toTar(
                            index,
                            sevenZFile::read,
                            tarOutputStream,
                            spec,
                            sevenZArchiveEntry.getName(),
                            sevenZArchiveEntry.getHasLastModifiedDate()
                                    ? sevenZArchiveEntry.getLastModifiedDate()
                                    : null,
                            sevenZArchiveEntry.getSize()));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromTarToZip(ArchiveCopySpec spec, int index)
            throws IOException {
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(spec.getTo())) {
            ArchiveUtils.readTar(
                    spec.getFromType(),
                    spec.getFrom(),
                    (tarArchiveEntry, tarArchiveInputStream) -> toZip(
                            index,
                            tarArchiveInputStream::read,
                            zipArchiveOutputStream,
                            spec,
                            tarArchiveEntry.getName(),
                            from(tarArchiveEntry.getLastModifiedDate()),
                            null,
                            null,
                            tarArchiveEntry.getRealSize()));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromTarToSevenZip(ArchiveCopySpec spec, int index) throws IOException {
        try (SevenZOutputFile sevenZOutputFile = new SevenZOutputFile(spec.getTo().toFile())) {
            ArchiveUtils.readTar(
                    spec.getFromType(),
                    spec.getFrom(),
                    (tarArchiveEntry, tarArchiveInputStream) -> toSevenZip(
                            index,
                            tarArchiveInputStream::read,
                            sevenZOutputFile,
                            spec,
                            tarArchiveEntry.getName(),
                            tarArchiveEntry.getLastModifiedDate(),
                            null,
                            null,
                            tarArchiveEntry.getRealSize()));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromTarToTar(ArchiveCopySpec spec, int index) throws IOException {
        OutputStream outputStream = ArchiveUtils.outputStreamForTar(spec.getToType(), spec.getTo());
        if (outputStream == null) {
            return;
        }
        try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(outputStream)) {
            ArchiveUtils.readTar(
                    spec.getFromType(),
                    spec.getFrom(),
                    (tarArchiveEntry, tarArchiveInputStream) -> toTar(
                            index,
                            tarArchiveInputStream::read,
                            tarOutputStream,
                            spec,
                            tarArchiveEntry.getName(),
                            tarArchiveEntry.getLastModifiedDate(),
                            tarArchiveEntry.getRealSize()));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private static ImmutableSet<String> getInternalSources(ArchiveCopySpec spec) {
        return spec.getInternalSpecs().stream()
                .map(ArchiveCopySpec.InternalSpec::getFrom)
                .collect(ImmutableSet.toImmutableSet());
    }

    private void toZip(
            int index,
            TriFunction<byte[], Integer, Integer, Integer, IOException> readFunction,
            ZipArchiveOutputStream zipArchiveOutputStream,
            ArchiveCopySpec spec,
            String name,
            @Nullable FileTime lastModifiedTime,
            @Nullable FileTime lastAccessTime,
            @Nullable FileTime creationTime,
            long size)
            throws IOException {
        toZip(
                index,
                readFunction,
                zipArchiveOutputStream,
                spec,
                findInternalSpec(spec, name),
                name,
                lastModifiedTime,
                lastAccessTime,
                creationTime,
                size);
    }

    private void toZip(
            int index,
            TriFunction<byte[], Integer, Integer, Integer, IOException> readFunction,
            ZipArchiveOutputStream zipArchiveOutputStream,
            ArchiveCopySpec spec,
            @Nullable ArchiveCopySpec.InternalSpec internal,
            String name,
            @Nullable FileTime lastModifiedTime,
            @Nullable FileTime lastAccessTime,
            @Nullable FileTime creationTime,
            long size)
            throws IOException {
        if (internal == null) {
            return;
        }
        Path to = spec.getTo().resolve(internal.getTo());
        ZipArchiveEntry zae = new ZipArchiveEntry(internal.getTo());
        setTimes(lastModifiedTime, lastAccessTime, creationTime, zae);
        Path source = spec.getFrom().resolve(name);
        zipArchiveOutputStream.putArchiveEntry(zae);
        copyWithProgress(
                index,
                size,
                source,
                to,
                readFunction,
                zipArchiveOutputStream::write);
        zipArchiveOutputStream.closeArchiveEntry();
    }

    private void setTimes(
            @Nullable FileTime lastModifiedTime,
            @Nullable FileTime lastAccessTime,
            @Nullable FileTime creationTime,
            ZipArchiveEntry zae) {
        // This doesn't seem to have any effect on Apache's ZipArchiveEntry, but setting it just in case
        if (lastModifiedTime != null) {
            zae.setLastModifiedTime(lastModifiedTime);
        }
        if (lastAccessTime != null) {
            zae.setLastAccessTime(lastAccessTime);
        }
        if (creationTime != null) {
            zae.setCreationTime(creationTime);
        }
        boolean exceedsUnixTime = exceedsUnixTime(lastModifiedTime)
                || exceedsUnixTime(lastAccessTime)
                || exceedsUnixTime(creationTime);
        if (exceedsUnixTime) {
            addNTFSTimestamp(lastModifiedTime, lastAccessTime, creationTime, zae);
        } else {
            addExtendedTimestamp(lastModifiedTime, lastAccessTime, creationTime, zae);
        }
    }

    private void addExtendedTimestamp(
            @Nullable FileTime lastModifiedTime,
            @Nullable FileTime lastAccessTime,
            @Nullable FileTime creationTime,
            ZipArchiveEntry zae) {
        X5455_ExtendedTimestamp timestamp = new X5455_ExtendedTimestamp();
        timestamp.setModifyTime(toUnixTime(lastModifiedTime));
        timestamp.setAccessTime(toUnixTime(lastAccessTime));
        timestamp.setCreateTime(toUnixTime(creationTime));
        zae.addExtraField(timestamp);
    }

    private void addNTFSTimestamp(
            @Nullable FileTime lastModifiedTime,
            @Nullable FileTime lastAccessTime,
            @Nullable FileTime creationTime,
            ZipArchiveEntry zae) {
        X000A_NTFS timestamp = new X000A_NTFS();
        timestamp.setModifyTime(toWindowsTime(lastModifiedTime));
        timestamp.setAccessTime(toWindowsTime(lastAccessTime));
        timestamp.setCreateTime(toWindowsTime(creationTime));
        zae.addExtraField(timestamp);
    }

    private static boolean exceedsUnixTime(@Nullable FileTime fileTime) {
        return fileTime != null && fileTimeToUnixTime(fileTime) > UPPER_UNIXTIME_BOUND;
    }

    private static long fileTimeToUnixTime(@Nonnull FileTime fileTime) {
        return fileTime.to(TimeUnit.SECONDS);
    }

    private void toSevenZip(
            int index,
            TriFunction<byte[], Integer, Integer, Integer, IOException> readFunction,
            SevenZOutputFile sevenZOutputFile,
            ArchiveCopySpec spec,
            String name,
            @Nullable Date lastModifiedDate,
            @Nullable Date lastAccessDate,
            @Nullable Date creationDate,
            long size)
            throws IOException {
        toSevenZip(
                index,
                readFunction,
                sevenZOutputFile,
                spec,
                findInternalSpec(spec, name),
                name,
                lastModifiedDate,
                lastAccessDate,
                creationDate,
                size);
    }

    private void toSevenZip(
            int index,
            TriFunction<byte[], Integer, Integer, Integer, IOException> readFunction,
            SevenZOutputFile sevenZOutputFile,
            ArchiveCopySpec spec,
            @Nullable ArchiveCopySpec.InternalSpec internal,
            String name,
            @Nullable Date lastModifiedDate,
            @Nullable Date lastAccessDate,
            @Nullable Date creationDate,
            long size)
            throws IOException {
        if (internal == null) {
            return;
        }
        Path to = spec.getTo().resolve(internal.getTo());
        SevenZArchiveEntry zae = new SevenZArchiveEntry();
        zae.setName(internal.getTo());
        zae.setSize(size);
        if (lastModifiedDate != null) {
            zae.setLastModifiedDate(lastModifiedDate);
        }
        if (lastAccessDate != null) {
            zae.setAccessDate(lastAccessDate);
        }
        if (creationDate != null) {
            zae.setCreationDate(creationDate);
        }
        Path source = spec.getFrom().resolve(name);
        sevenZOutputFile.putArchiveEntry(zae);
        copyWithProgress(
                index,
                size,
                source,
                to,
                readFunction,
                sevenZOutputFile::write);
        sevenZOutputFile.closeArchiveEntry();
    }

    private void toTar(
            int index,
            TriFunction<byte[], Integer, Integer, Integer, IOException> readFunction,
            TarArchiveOutputStream tarOutputStream,
            ArchiveCopySpec spec,
            String name,
            @Nullable Date lastModifiedDate,
            long size)
            throws IOException {
        toTar(
                index,
                readFunction,
                tarOutputStream,
                spec,
                findInternalSpec(spec, name),
                name,
                lastModifiedDate,
                size);
    }

    private void toTar(
            int index,
            TriFunction<byte[], Integer, Integer, Integer, IOException> readFunction,
            TarArchiveOutputStream tarOutputStream,
            ArchiveCopySpec spec,
            @Nullable ArchiveCopySpec.InternalSpec internal,
            String name,
            @Nullable Date lastModifiedDate,
            long size)
            throws IOException {
        if (internal == null) {
            return;
        }
        Path to = spec.getTo().resolve(internal.getTo());
        TarArchiveEntry zae = new TarArchiveEntry(internal.getTo());
        zae.setSize(size);
        if (lastModifiedDate != null) {
            zae.setModTime(lastModifiedDate);
        }
        Path source = spec.getFrom().resolve(name);
        tarOutputStream.putArchiveEntry(zae);
        copyWithProgress(
                index,
                size,
                source,
                to,
                readFunction,
                tarOutputStream::write);
        tarOutputStream.closeArchiveEntry();
    }

    @Nullable
    private Date toDate(@Nullable FileTime fileTime) {
        return fileTime != null ? new Date(fileTime.toMillis()) : null;
    }

    @Nullable
    private ZipLong toUnixTime(@Nullable FileTime fileTime) {
        return fileTime != null ? new ZipLong(fileTimeToUnixTime(fileTime)) : null;
    }

    @Nullable
    private ZipEightByteInteger toWindowsTime(@Nullable FileTime fileTime) {
        return fileTime != null ? new ZipEightByteInteger(fileTime.to(TimeUnit.MICROSECONDS) * 10) : null;
    }

    @Nullable
    private Date toDate(@Nullable LocalDateTime date) {
        return date != null ? new Date(date.atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()) : null;
    }

    @Nullable
    private static FileTime from(@Nullable Date date) {
        return date != null ? FileTime.fromMillis(date.getTime()) : null;
    }

    @Nullable
    private static FileTime from(@Nullable LocalDateTime date) {
        return date != null ? FileTime.from(date.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    @Nullable
    private ExtractionSpec.InternalSpec findInternalSpec(ExtractionSpec spec, String name) {
        return spec.getInternalSpecs().stream()
                .filter(ad -> ad.getFrom().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private ArchiveCopySpec.InternalSpec findInternalSpec(ArchiveCopySpec spec, String name) {
        return spec.getInternalSpecs().stream()
                .filter(ad -> ad.getFrom().equals(name))
                .findFirst()
                .orElse(null);
    }

    private void copyWithProgress(
            int index,
            long size,
            Path source,
            Path destination,
            TriFunction<byte[], Integer, Integer, Integer, IOException> readFunction,
            TriConsumer<byte[], Integer, Integer, IOException> writeConsumer) throws IOException {
        for (Listener listener : listeners) {
            listener.reportStart(index, source, destination, size);
        }
        byte[] buffer = threadLocalBuffer.get();
        long totalRead = 0;
        int bytesRead;
        int remainingBytes;
        while ((remainingBytes = (int) Math.min(size - totalRead, buffer.length)) > 0
                && (bytesRead = readFunction.apply(buffer, 0, remainingBytes)) > -1) {
            totalRead += bytesRead;
            writeConsumer.accept(buffer, 0, bytesRead);
            for (Listener listener : listeners) {
                listener.reportBytesCopied(index, bytesRead);
            }
        }
    }

    @FunctionalInterface
    private interface TriFunction<K, L, M, N, E extends Throwable> {

        N apply(K k, L l, M m) throws E;
    }

    @FunctionalInterface
    private interface TriConsumer<K, L, M, E extends Throwable> {

        void accept(K k, L l, M m) throws E;
    }

}
