package io.github.datromtool.io;

import com.github.junrar.exception.RarException;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.util.ArchiveUtils;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.lzma.LZMAUtils;
import org.apache.commons.compress.compressors.xz.XZUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.InvalidParameterException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

public final class FileCopier {

    private static final Logger logger = LoggerFactory.getLogger(FileCopier.class);

    private static final int BUFFER_SIZE = 32 * 1024; // 32KB per thread
    private static final Path EMPTY_PATH = Paths.get("");

    public static abstract class Spec {

        private Spec() {
        }
    }

    @Builder
    @Value
    public static class InternalCopySpec<K, T> {

        @NonNull
        K from;
        @NonNull
        T to;
    }

    @Builder
    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class CopySpec extends Spec {

        @NonNull
        Path from;
        @NonNull
        Path to;
    }

    @Builder
    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class ExtractionSpec extends Spec {

        @NonNull
        ArchiveType fromType;
        @NonNull
        Path from;

        @NonNull
        ImmutableSet<InternalCopySpec<String, Path>> internalCopySpecs;
    }

    @Builder
    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class CompressionSpec extends Spec {

        @NonNull
        ArchiveType toType;
        @NonNull
        Path to;

        @NonNull
        ImmutableSet<InternalCopySpec<Path, String>> internalCopySpecs;
    }

    @Builder
    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class ArchiveCopySpec extends Spec {

        @NonNull
        ArchiveType fromType;
        @NonNull
        ArchiveType toType;
        @NonNull
        Path from;
        @NonNull
        Path to;

        @NonNull
        ImmutableSet<InternalCopySpec<String, String>> internalCopySpecs;
    }

    public interface Listener {

        void reportStart(Path path, Path destination, int thread);

        void reportProgress(Path path, Path destination, int thread, int percentage, long speed);

        void reportFailure(
                Path path,
                Path destination,
                int thread,
                String message,
                Throwable cause);

        void reportFinish(Path path, Path destination, int thread);
    }

    private final int numThreads;
    private final boolean allowRawZipCopy;
    private final Listener listener;

    public FileCopier(
            @Nonnull AppConfig appConfig,
            boolean allowRawZipCopy,
            @Nullable Listener listener) {
        this.allowRawZipCopy = allowRawZipCopy;
        this.numThreads = appConfig.getCopier().getThreads();
        this.listener = listener;
    }

    private final ThreadLocal<byte[]> threadLocalBuffer =
            ThreadLocal.withInitial(() -> new byte[BUFFER_SIZE]);

    public void copy(Set<? extends Spec> definitions) {
        ExecutorService executorService = Executors.newFixedThreadPool(
                numThreads,
                new IndexedThreadFactory(logger, "COPIER"));
        if (!LZMAUtils.isLZMACompressionAvailable()) {
            logger.warn("LZMA compression support is disabled");
        }
        if (!XZUtils.isXZCompressionAvailable()) {
            logger.warn("XZ compression support is disabled");
        }
        definitions.stream()
                .map(d -> executorService.submit(() -> copy(d)))
                .collect(ImmutableList.toImmutableList())
                .forEach(this::waitForCompletion);
        executorService.shutdownNow();
    }

    private void waitForCompletion(Future<?> future) {
        try {
            future.get();
        } catch (Exception e) {
            logger.error("Unexpected exception thrown", e);
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

    private <K, T> ImmutableList<T> transform(Collection<K> o, Function<K, T> f) {
        return o.stream().map(f).collect(ImmutableList.toImmutableList());
    }

    private void copy(CopySpec spec) {
        int index = getThreadIndex();
        if (listener != null) {
            listener.reportStart(spec.getFrom(), spec.getTo(), index);
        }
        try {
            try (InputStream inputStream = Files.newInputStream(spec.getFrom())) {
                try (OutputStream outputStream = Files.newOutputStream(spec.getTo())) {
                    copyWithProgress(
                            index,
                            Files.size(spec.getFrom()),
                            spec.getFrom(),
                            spec.getTo(),
                            inputStream::read,
                            outputStream::write);
                }
            }
            Files.setLastModifiedTime(spec.getTo(), Files.getLastModifiedTime(spec.getFrom()));
        } catch (Exception e) {
            logger.error("Could not copy '{}' to '{}'", spec.getFrom(), spec.getTo(), e);
            if (listener != null) {
                listener.reportFailure(
                        spec.getFrom(),
                        spec.getTo(),
                        index,
                        "Could not copy files",
                        e);
            }
        } finally {
            if (listener != null) {
                listener.reportFinish(spec.getFrom(), spec.getTo(), index);
            }
        }
    }

    private void copy(ExtractionSpec spec) {
        int index = getThreadIndex();
        if (listener != null) {
            listener.reportStart(spec.getFrom(), EMPTY_PATH, index);
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
            logger.error("Could not extract '{}'. RAR5 is not supported yet", spec.getFrom());
            if (listener != null) {
                listener.reportFailure(
                        spec.getFrom(),
                        EMPTY_PATH,
                        index,
                        "Could not extract archive. RAR5 is not supported yet",
                        e);
            }
        } catch (Exception e) {
            logger.error("Could not extract '{}'", spec.getFrom(), e);
            if (listener != null) {
                listener.reportFailure(
                        spec.getFrom(),
                        EMPTY_PATH,
                        index,
                        "Could not extract archive",
                        e);
            }
        } finally {
            if (listener != null) {
                listener.reportFinish(spec.getFrom(), EMPTY_PATH, index);
            }
        }
    }

    private void copy(CompressionSpec spec) {
        int index = getThreadIndex();
        if (listener != null) {
            listener.reportStart(EMPTY_PATH, spec.getTo(), index);
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
            logger.error("Could not compress files to '{}'", spec.getTo(), e);
            if (listener != null) {
                listener.reportFailure(
                        EMPTY_PATH,
                        spec.getTo(),
                        index,
                        "Could not compress files",
                        e);
            }
        } finally {
            if (listener != null) {
                listener.reportFinish(EMPTY_PATH, spec.getTo(), index);
            }
        }
    }

    private void copy(ArchiveCopySpec spec) {
        int index = getThreadIndex();
        if (listener != null) {
            listener.reportStart(spec.getFrom(), spec.getTo(), index);
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
            logger.error(
                    "Could not copy contents of '{}' to '{}'. RAR5 is not supported yet",
                    spec.getFrom(),
                    spec.getTo());
            if (listener != null) {
                listener.reportFailure(
                        spec.getFrom(),
                        spec.getTo(),
                        index,
                        "Could not copy contents of archive. RAR5 is not supported yet",
                        e);
            }
        } catch (Exception e) {
            logger.error(
                    "Could not copy contents of '{}' to '{}'",
                    spec.getFrom(),
                    spec.getTo(),
                    e);
            if (listener != null) {
                listener.reportFailure(
                        spec.getFrom(),
                        spec.getTo(),
                        index,
                        "Could not copy contents of archive",
                        e);
            }
        } finally {
            if (listener != null) {
                listener.reportFinish(spec.getFrom(), spec.getTo(), index);
            }
        }
    }

    private void extractZipEntries(ExtractionSpec spec, int index) throws IOException {
        ArchiveUtils.readZip(spec.getFrom(), (zipFile, zipArchiveEntry) -> {
            String name = zipArchiveEntry.getName();
            InternalCopySpec<String, Path> internal = findInternalSpec(spec, name);
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
                if (zipArchiveEntry.getLastModifiedDate() != null) {
                    Files.setLastModifiedTime(to, zipArchiveEntry.getLastAccessTime());
                } else if (zipArchiveEntry.getCreationTime() != null) {
                    Files.setLastModifiedTime(to, zipArchiveEntry.getCreationTime());
                }
            }
        });
    }

    private void extractRarEntries(ExtractionSpec spec, int index) throws Exception {
        try {
            ArchiveUtils.readRar(spec.getFrom(), (archive, fileHeader) -> {
                String name = fileHeader.getFileName().replace('\\', '/');
                InternalCopySpec<String, Path> internal = findInternalSpec(spec, name);
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
                    if (fileHeader.getMTime() != null) {
                        Files.setLastModifiedTime(to, from(fileHeader.getMTime()));
                    } else if (fileHeader.getCTime() != null) {
                        Files.setLastModifiedTime(to, from(fileHeader.getCTime()));
                    }
                }
            });
        } catch (UnsupportedRarV5Exception e) {
            if (ArchiveUtils.isUnrarAvailable()) {
                extractRarEntriesWithUnrar(spec, index);
            } else {
                throw e;
            }
        }
    }

    private void extractRarEntriesWithUnrar(ExtractionSpec spec, int index) throws Exception {
        ImmutableSet<String> desiredEntryNames = spec.getInternalCopySpecs().stream()
                .map(InternalCopySpec::getFrom)
                .collect(ImmutableSet.toImmutableSet());
        ArchiveUtils.readRarWithUnrar(
                spec.getFrom(),
                desiredEntryNames,
                (entry, processInputStream) -> {
                    String name = entry.getName();
                    InternalCopySpec<String, Path> internal = findInternalSpec(spec, name);
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
                    if (entry.getModificationTime() != null) {
                        Files.setLastModifiedTime(to, from(entry.getModificationTime()));
                    }
                });
    }

    private void extractSevenZipEntries(ExtractionSpec spec, int index) throws IOException {
        ArchiveUtils.readSevenZip(spec.getFrom(), (sevenZFile, sevenZArchiveEntry) -> {
            String name = sevenZArchiveEntry.getName();
            InternalCopySpec<String, Path> internal = findInternalSpec(spec, name);
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
            if (sevenZArchiveEntry.getHasLastModifiedDate()) {
                FileTime fileTime = from(sevenZArchiveEntry.getLastModifiedDate());
                Files.setLastModifiedTime(to, fileTime);
            } else if (sevenZArchiveEntry.getHasCreationDate()) {
                FileTime fileTime = from(sevenZArchiveEntry.getCreationDate());
                Files.setLastModifiedTime(to, fileTime);
            }
        });
    }

    private void extractTarEntries(ExtractionSpec spec, int index) throws IOException {
        ArchiveUtils.readTar(
                spec.getFromType(),
                spec.getFrom(),
                (tarArchiveEntry, tarArchiveInputStream) -> {
                    String name = tarArchiveEntry.getName();
                    InternalCopySpec<String, Path> internal = findInternalSpec(spec, name);
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
                    if (tarArchiveEntry.getLastModifiedDate() != null) {
                        FileTime fileTime = from(tarArchiveEntry.getLastModifiedDate());
                        Files.setLastModifiedTime(to, fileTime);
                    }
                });
    }

    private void compressZipEntries(CompressionSpec spec, int index) throws IOException {
        try (ZipArchiveOutputStream zipArchiveOutputStream =
                new ZipArchiveOutputStream(spec.getTo().toFile())) {
            for (InternalCopySpec<Path, String> internal : spec.getInternalCopySpecs()) {
                Path source = internal.getFrom();
                try (InputStream inputStream = Files.newInputStream(source)) {
                    ArchiveEntry archiveEntry = zipArchiveOutputStream.createArchiveEntry(
                            source.toFile(),
                            internal.getTo());
                    Path destination = spec.getTo().resolve(internal.getTo());
                    zipArchiveOutputStream.putArchiveEntry(archiveEntry);
                    copyWithProgress(
                            index,
                            Files.size(source),
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
            for (InternalCopySpec<Path, String> internal : spec.getInternalCopySpecs()) {
                Path source = internal.getFrom();
                try (InputStream inputStream = Files.newInputStream(source)) {
                    SevenZArchiveEntry archiveEntry =
                            sevenZOutputFile.createArchiveEntry(source.toFile(), internal.getTo());
                    Path destination = spec.getTo().resolve(internal.getFrom());
                    sevenZOutputFile.putArchiveEntry(archiveEntry);
                    copyWithProgress(
                            index,
                            Files.size(source),
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
            for (InternalCopySpec<Path, String> internal : spec.getInternalCopySpecs()) {
                Path source = internal.getFrom();
                try (InputStream inputStream = Files.newInputStream(source)) {
                    ArchiveEntry archiveEntry =
                            tarOutputStream.createArchiveEntry(source.toFile(), internal.getTo());
                    Path destination = spec.getTo().resolve(internal.getTo());
                    tarOutputStream.putArchiveEntry(archiveEntry);
                    copyWithProgress(
                            index,
                            Files.size(source),
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
                if (allowRawZipCopy) {
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
        try (ZipArchiveOutputStream zipArchiveOutputStream =
                new ZipArchiveOutputStream(spec.getTo().toFile())) {
            ArchiveUtils.readZip(spec.getFrom(), (zipFile, zipArchiveEntry) -> {
                String name = zipArchiveEntry.getName();
                InternalCopySpec<String, String> internal = findInternalSpec(spec, name);
                if (internal == null) {
                    return;
                }
                Path source = spec.getFrom().resolve(name);
                Path to = spec.getTo().resolve(name);
                if (!name.equals(internal.getTo())) {
                    logger.warn(
                            "Cannot rename files inside ZIPs when using raw copy. "
                                    + "Writing '{}' as '{}'",
                            spec.getTo().resolve(internal.getTo()),
                            to);
                }
                if (listener != null) {
                    listener.reportProgress(source, to, index, 0, 0);
                }
                long start = System.nanoTime();
                zipArchiveOutputStream.addRawArchiveEntry(
                        zipArchiveEntry,
                        zipFile.getRawInputStream(zipArchiveEntry));
                if (listener != null) {
                    double secondsPassed = (System.nanoTime() - start) / 1E9d;
                    long bytesPerSecond = Math.round(zipArchiveEntry.getSize() / secondsPassed);
                    listener.reportProgress(
                            source,
                            to,
                            index,
                            100,
                            bytesPerSecond);
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
        try (ZipArchiveOutputStream zipArchiveOutputStream =
                new ZipArchiveOutputStream(spec.getTo().toFile())) {
            ArchiveUtils.readZip(
                    spec.getFrom(),
                    (zipFile, zipArchiveEntry) -> {
                        String name = zipArchiveEntry.getName();
                        InternalCopySpec<String, String> internal = findInternalSpec(spec, name);
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
                                    zipArchiveEntry.getCreationTime(),
                                    zipArchiveEntry.getLastModifiedTime(),
                                    zipArchiveEntry.getLastAccessTime(),
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
                        InternalCopySpec<String, String> internal = findInternalSpec(spec, name);
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
                                    toDate(zipArchiveEntry.getCreationTime()),
                                    toDate(zipArchiveEntry.getLastModifiedTime()),
                                    toDate(zipArchiveEntry.getLastAccessTime()),
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
                        InternalCopySpec<String, String> internal = findInternalSpec(spec, name);
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
        try (ZipArchiveOutputStream zipArchiveOutputStream =
                new ZipArchiveOutputStream(spec.getTo().toFile())) {
            ArchiveUtils.readRar(
                    spec.getFrom(),
                    (archive, fileHeader) -> {
                        String name = fileHeader.getFileName().replace('\\', '/');;
                        InternalCopySpec<String, String> internal = findInternalSpec(spec, name);
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
                                    from(fileHeader.getCTime()),
                                    from(fileHeader.getMTime()),
                                    from(fileHeader.getATime()),
                                    fileHeader.getFullUnpackSize());
                        }
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (UnsupportedRarV5Exception e) {
            if (ArchiveUtils.isUnrarAvailable()) {
                fromRarWithUnrarToZip(spec, index);
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
        try (ZipArchiveOutputStream zipArchiveOutputStream =
                new ZipArchiveOutputStream(spec.getTo().toFile())) {
            ImmutableSet<String> desiredEntryNames = getInternalSources(spec);
            ArchiveUtils.readRarWithUnrar(
                    spec.getFrom(),
                    desiredEntryNames,
                    (entry, processInputStream) -> {
                        String name = entry.getName();
                        InternalCopySpec<String, String> internal = findInternalSpec(spec, name);
                        if (internal == null) {
                            return;
                        }
                        FileTime time = from(entry.getModificationTime());
                        toZip(
                                index,
                                processInputStream::read,
                                zipArchiveOutputStream,
                                spec,
                                internal,
                                name,
                                time,
                                time,
                                time,
                                entry.getSize());
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException | RarException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromRarToSevenZip(ArchiveCopySpec spec, int index) throws Exception {
        try (SevenZOutputFile sevenZOutputFile = new SevenZOutputFile(spec.getTo().toFile())) {
            ArchiveUtils.readRar(
                    spec.getFrom(),
                    (archive, fileHeader) -> {
                        String name = fileHeader.getFileName().replace('\\', '/');
                        InternalCopySpec<String, String> internal = findInternalSpec(spec, name);
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
                                    fileHeader.getCTime(),
                                    fileHeader.getMTime(),
                                    fileHeader.getATime(),
                                    fileHeader.getFullUnpackSize());
                        }
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (UnsupportedRarV5Exception e) {
            if (ArchiveUtils.isUnrarAvailable()) {
                fromRarWithUnrarToSevenZip(spec, index);
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
                    (entry, processInputStream) -> {
                        String name = entry.getName();
                        InternalCopySpec<String, String> internal = findInternalSpec(spec, name);
                        if (internal == null) {
                            return;
                        }
                        Date time = toDate(entry.getModificationTime());
                        toSevenZip(
                                index,
                                processInputStream::read,
                                sevenZOutputFile,
                                spec,
                                internal,
                                name,
                                time,
                                time,
                                time,
                                entry.getSize());
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException | RarException e) {
            Files.delete(spec.getTo());
            throw e;
        }
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
                        String name = fileHeader.getFileName().replace('\\', '/');
                        InternalCopySpec<String, String> internal = findInternalSpec(spec, name);
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
            if (ArchiveUtils.isUnrarAvailable()) {
                fromRarWithUnrarToTar(spec, index);
            } else {
                Files.delete(spec.getTo());
                throw e;
            }
        } catch (IOException | RarException e) {
            Files.delete(spec.getTo());
            throw e;
        }
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
                    (entry, processInputStream) -> {
                        String name = entry.getName();
                        InternalCopySpec<String, String> internal = findInternalSpec(spec, name);
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
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException | RarException e) {
            Files.delete(spec.getTo());
            throw e;
        }
    }

    private void fromSevenZipToZip(ArchiveCopySpec spec, int index) throws IOException {
        try (ZipArchiveOutputStream zipArchiveOutputStream =
                new ZipArchiveOutputStream(spec.getTo().toFile())) {
            ArchiveUtils.readSevenZip(
                    spec.getFrom(),
                    (sevenZFile, sevenZArchiveEntry) -> toZip(
                            index,
                            sevenZFile::read,
                            zipArchiveOutputStream,
                            spec,
                            sevenZArchiveEntry.getName(),
                            sevenZArchiveEntry.getHasCreationDate()
                                    ? from(sevenZArchiveEntry.getCreationDate())
                                    : null,
                            sevenZArchiveEntry.getHasLastModifiedDate()
                                    ? from(sevenZArchiveEntry.getLastModifiedDate())
                                    : null,
                            sevenZArchiveEntry.getHasAccessDate()
                                    ? from(sevenZArchiveEntry.getAccessDate())
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
                            sevenZArchiveEntry.getHasCreationDate()
                                    ? sevenZArchiveEntry.getCreationDate()
                                    : null,
                            sevenZArchiveEntry.getHasLastModifiedDate()
                                    ? sevenZArchiveEntry.getLastModifiedDate()
                                    : null,
                            sevenZArchiveEntry.getHasAccessDate()
                                    ? sevenZArchiveEntry.getAccessDate()
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
        try (ZipArchiveOutputStream zipArchiveOutputStream =
                new ZipArchiveOutputStream(spec.getTo().toFile())) {
            ArchiveUtils.readTar(
                    spec.getFromType(),
                    spec.getFrom(),
                    (tarArchiveEntry, tarArchiveInputStream) -> toZip(
                            index,
                            tarArchiveInputStream::read,
                            zipArchiveOutputStream,
                            spec,
                            tarArchiveEntry.getName(),
                            null,
                            from(tarArchiveEntry.getLastModifiedDate()),
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
                            null,
                            tarArchiveEntry.getLastModifiedDate(),
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
        return spec.getInternalCopySpecs().stream()
                .map(InternalCopySpec::getFrom)
                .collect(ImmutableSet.toImmutableSet());
    }

    private void toZip(
            int index,
            TriFunction<byte[], Integer, Integer, Integer, IOException> readFunction,
            ZipArchiveOutputStream zipArchiveOutputStream,
            ArchiveCopySpec spec,
            String name,
            @Nullable FileTime creationTime,
            @Nullable FileTime lastModifiedTime,
            @Nullable FileTime lastAccessTime,
            long size)
            throws IOException {
        toZip(
                index,
                readFunction,
                zipArchiveOutputStream,
                spec,
                findInternalSpec(spec, name),
                name,
                creationTime,
                lastModifiedTime,
                lastAccessTime,
                size);
    }

    private void toZip(
            int index,
            TriFunction<byte[], Integer, Integer, Integer, IOException> readFunction,
            ZipArchiveOutputStream zipArchiveOutputStream,
            ArchiveCopySpec spec,
            @Nullable InternalCopySpec<String, String> internal,
            String name,
            @Nullable FileTime creationTime,
            @Nullable FileTime lastModifiedTime,
            @Nullable FileTime lastAccessTime,
            long size)
            throws IOException {
        if (internal == null) {
            return;
        }
        Path to = spec.getTo().resolve(internal.getTo());
        ZipArchiveEntry zae = new ZipArchiveEntry(internal.getTo());
        zae.setSize(size);
        if (creationTime != null) {
            zae.setCreationTime(creationTime);
        }
        if (lastModifiedTime != null) {
            zae.setLastModifiedTime(lastModifiedTime);
        }
        if (lastAccessTime != null) {
            zae.setLastAccessTime(lastAccessTime);
        }
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

    private void toSevenZip(
            int index,
            TriFunction<byte[], Integer, Integer, Integer, IOException> readFunction,
            SevenZOutputFile sevenZOutputFile,
            ArchiveCopySpec spec,
            String name,
            @Nullable Date creationDate,
            @Nullable Date lastModifiedDate,
            @Nullable Date lastAccessDate,
            long size)
            throws IOException {
        toSevenZip(
                index,
                readFunction,
                sevenZOutputFile,
                spec,
                findInternalSpec(spec, name),
                name,
                creationDate,
                lastModifiedDate,
                lastAccessDate,
                size);
    }

    private void toSevenZip(
            int index,
            TriFunction<byte[], Integer, Integer, Integer, IOException> readFunction,
            SevenZOutputFile sevenZOutputFile,
            ArchiveCopySpec spec,
            @Nullable InternalCopySpec<String, String> internal,
            String name,
            @Nullable Date creationDate,
            @Nullable Date lastModifiedDate,
            @Nullable Date lastAccessDate,
            long size)
            throws IOException {
        if (internal == null) {
            return;
        }
        Path to = spec.getTo().resolve(internal.getTo());
        SevenZArchiveEntry zae = new SevenZArchiveEntry();
        zae.setName(internal.getTo());
        zae.setSize(size);
        if (creationDate != null) {
            zae.setCreationDate(creationDate);
        }
        if (lastModifiedDate != null) {
            zae.setLastModifiedDate(lastModifiedDate);
        }
        if (lastAccessDate != null) {
            zae.setLastModifiedDate(lastAccessDate);
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
            @Nullable InternalCopySpec<String, String> internal,
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
    private InternalCopySpec<String, Path> findInternalSpec(ExtractionSpec spec, String name) {
        return findInternalSpec(spec.getInternalCopySpecs(), name);
    }

    @Nullable
    private InternalCopySpec<String, String> findInternalSpec(ArchiveCopySpec spec, String name) {
        return findInternalSpec(spec.getInternalCopySpecs(), name);
    }

    @Nullable
    private <K, T> InternalCopySpec<K, T> findInternalSpec(
            Collection<InternalCopySpec<K, T>> collection,
            K name) {
        return collection
                .stream()
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
        byte[] buffer = threadLocalBuffer.get();
        int reportedPercentage = 0;
        long totalRead = 0;
        long start = System.nanoTime();
        int bytesRead;
        int remainingBytes;
        while ((remainingBytes = (int) Math.min(size - totalRead, buffer.length)) > 0
                && (bytesRead = readFunction.apply(buffer, 0, remainingBytes)) > -1) {
            totalRead += bytesRead;
            writeConsumer.accept(buffer, 0, bytesRead);
            if (listener != null) {
                int percentage = (int) ((totalRead * 100d) / size);
                if (reportedPercentage != percentage) {
                    double secondsPassed = (System.nanoTime() - start) / 1E9d;
                    long bytesPerSecond = Math.round(bytesRead / secondsPassed);
                    listener.reportProgress(source, destination, index, percentage, bytesPerSecond);
                    reportedPercentage = percentage;
                }
                start = System.nanoTime();
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
