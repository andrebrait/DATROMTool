package io.github.datromtool.io;

import com.github.junrar.exception.RarException;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.config.AppConfig;
import lombok.Builder;
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
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class FileCopier {

    private static final Logger logger = LoggerFactory.getLogger(FileCopier.class);

    private static final int BUFFER_SIZE = 32 * 1024; // 32KB per thread

    @Builder
    @Value
    public static class ArchiveCopyDefinition {

        @NonNull
        String source;
        @NonNull
        String destination;
    }

    @Builder
    @Value
    public static class CopyDefinition {

        ArchiveType fromType;
        @NonNull
        Path from;
        @NonNull
        Path to;
        @NonNull
        @Builder.Default
        ImmutableSet<ArchiveCopyDefinition> archiveCopyDefinitions = ImmutableSet.of();
    }

    public interface Listener {

        void reportStart(Path path, Path destination, int thread);

        void reportProgress(Path path, Path destination, int thread, int percentage, long speed);

        void reportSkip(Path path, Path destination, int thread, String message);

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

    public void copy(Set<CopyDefinition> copyDefinitions) {
        ExecutorService executorService = Executors.newFixedThreadPool(
                numThreads,
                new IndexedThreadFactory(logger, "COPIER"));
        if (!LZMAUtils.isLZMACompressionAvailable()) {
            logger.warn("LZMA compression support is disabled");
        }
        if (!XZUtils.isXZCompressionAvailable()) {
            logger.warn("XZ compression support is disabled");
        }
        copyDefinitions.stream()
                .map(cd -> executorService.submit(() -> copy(cd)))
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

    private void copy(CopyDefinition copyDefinition) {
        int index = ((IndexedThread) Thread.currentThread()).getIndex();
        if (listener != null) {
            listener.reportStart(copyDefinition.getFrom(), copyDefinition.getTo(), index);
        }
        try {
            if (copyDefinition.getArchiveCopyDefinitions().isEmpty()) {
                try (InputStream inputStream = Files.newInputStream(copyDefinition.getFrom())) {
                    try (OutputStream outputStream =
                            Files.newOutputStream(copyDefinition.getTo())) {
                        copyWithProgress(
                                index,
                                Files.size(copyDefinition.getFrom()),
                                copyDefinition.getFrom(),
                                copyDefinition.getTo(),
                                inputStream::read,
                                outputStream::write);
                    }
                }
                Files.setLastModifiedTime(
                        copyDefinition.getTo(),
                        Files.getLastModifiedTime(copyDefinition.getFrom()));
            } else {
                extractOrCompress(copyDefinition, index);
            }
        } catch (UnsupportedRarV5Exception e) {
            logger.error(
                    "Unexpected error while copying '{}' to '{}'. "
                            + "Reason: RAR5 is not supported yet",
                    copyDefinition.getFrom(),
                    copyDefinition.getTo());
            if (listener != null) {
                listener.reportFailure(
                        copyDefinition.getFrom(),
                        copyDefinition.getTo(),
                        index,
                        "Could not read archive. Reason: RAR5 is not supported yet",
                        e);
            }
        } catch (Exception e) {
            logger.error(
                    "Unexpected error while copying '{}' to '{}'",
                    copyDefinition.getFrom(),
                    copyDefinition.getTo(),
                    e);
            if (listener != null) {
                listener.reportFailure(
                        copyDefinition.getFrom(),
                        copyDefinition.getTo(),
                        index,
                        "Unexpected error while copying files",
                        e);
            }
        } finally {
            if (listener != null) {
                listener.reportFinish(copyDefinition.getFrom(), copyDefinition.getTo(), index);
            }
        }
    }

    private void extractOrCompress(CopyDefinition copyDefinition, int index)
            throws Exception {
        ArchiveType fromType = copyDefinition.getFromType() != null
                ? copyDefinition.getFromType()
                : ArchiveType.parse(copyDefinition.getFrom());
        ArchiveType toType = ArchiveType.parse(copyDefinition.getTo());
        if (fromType == ArchiveType.NONE && toType == ArchiveType.NONE) {
            logger.error(
                    "Expected either '{}' or '{}' to be a recognized type of archive",
                    copyDefinition.getFrom(),
                    copyDefinition.getTo());
            if (listener != null) {
                listener.reportSkip(
                        copyDefinition.getFrom(),
                        copyDefinition.getTo(),
                        index,
                        "Expected to be a supported type of archive");
            }
            return;
        }
        if (toType == ArchiveType.NONE) {
            // Extract from an archive to uncompressed files
            switch (fromType) {
                case ZIP:
                    extractZipEntries(copyDefinition, index);
                    return;
                case RAR:
                    extractRarEntries(copyDefinition, index);
                    return;
                case SEVEN_ZIP:
                    extractSevenZipEntries(copyDefinition, index);
                    return;
                case TAR:
                case TAR_BZ2:
                case TAR_GZ:
                case TAR_LZ4:
                case TAR_LZMA:
                case TAR_XZ:
                    extractTarEntries(fromType, copyDefinition, index);
            }
        } else if (fromType == ArchiveType.NONE) {
            // Compress files to an archive
            switch (toType) {
                case ZIP:
                    compressZipEntries(copyDefinition, index);
                    return;
                case RAR:
                    throw new UnsupportedOperationException("RAR compression is not supported");
                case SEVEN_ZIP:
                    compressSevenZipEntries(copyDefinition, index);
                    return;
                case TAR:
                case TAR_BZ2:
                case TAR_GZ:
                case TAR_LZ4:
                case TAR_LZMA:
                case TAR_XZ:
                    compressTarEntries(toType, copyDefinition, index);
            }
        } else {
            // Copy files from an archive to another
            switch (fromType) {
                case ZIP:
                    fromZipToArchive(toType, copyDefinition, index);
                    return;
                case RAR:
                    fromRarToArchive(toType, copyDefinition, index);
                    return;
                case SEVEN_ZIP:
                    fromSevenZipToArchive(toType, copyDefinition, index);
                    return;
                case TAR:
                case TAR_BZ2:
                case TAR_GZ:
                case TAR_LZ4:
                case TAR_LZMA:
                case TAR_XZ:
                    fromTarToArchive(fromType, toType, copyDefinition, index);
            }
        }
    }

    private void extractZipEntries(CopyDefinition copyDefinition, int index) throws IOException {
        ArchiveUtils.readZip(copyDefinition.getFrom(), (zipFile, zipArchiveEntry) -> {
            String name = zipArchiveEntry.getName();
            ArchiveCopyDefinition archiveCopyDefinition =
                    findArchiveCopyDefinition(copyDefinition, name);
            if (archiveCopyDefinition == null) {
                return;
            }
            try (InputStream inputStream = zipFile.getInputStream(zipArchiveEntry)) {
                Path to = copyDefinition.getTo().resolve(archiveCopyDefinition.getDestination());
                try (OutputStream outputStream = Files.newOutputStream(to)) {
                    Path source = copyDefinition.getFrom().resolve(name);
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

    private void extractRarEntries(CopyDefinition copyDefinition, int index)
            throws Exception {
        ArchiveUtils.readRar(copyDefinition.getFrom(), (archive, fileHeader) -> {
            String name = fileHeader.getFileName();
            ArchiveCopyDefinition archiveCopyDefinition =
                    findArchiveCopyDefinition(copyDefinition, name);
            if (archiveCopyDefinition == null) {
                return;
            }
            try (InputStream inputStream = archive.getInputStream(fileHeader)) {
                Path to = copyDefinition.getTo().resolve(archiveCopyDefinition.getDestination());
                try (OutputStream outputStream = Files.newOutputStream(to)) {
                    Path source = copyDefinition.getFrom().resolve(name);
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
                    Files.setLastModifiedTime(to, fromDate(fileHeader.getMTime()));
                } else if (fileHeader.getCTime() != null) {
                    Files.setLastModifiedTime(to, fromDate(fileHeader.getCTime()));
                }
            }
        });
    }

    private void extractSevenZipEntries(CopyDefinition copyDefinition, int index)
            throws IOException {
        ArchiveUtils.readSevenZip(copyDefinition.getFrom(), (sevenZFile, sevenZArchiveEntry) -> {
            String name = sevenZArchiveEntry.getName();
            ArchiveCopyDefinition archiveCopyDefinition =
                    findArchiveCopyDefinition(copyDefinition, name);
            if (archiveCopyDefinition == null) {
                return;
            }
            Path to = copyDefinition.getTo().resolve(archiveCopyDefinition.getDestination());
            try (OutputStream outputStream = Files.newOutputStream(to)) {
                Path source = copyDefinition.getFrom().resolve(name);
                long size = sevenZArchiveEntry.getSize();
                copyWithProgress(index, size, source, to, sevenZFile::read, outputStream::write);
            } catch (FileAlreadyExistsException e) {
                throw e;
            } catch (IOException e) {
                Files.delete(to);
                throw e;
            }
            if (sevenZArchiveEntry.getHasLastModifiedDate()) {
                FileTime fileTime = fromDate(sevenZArchiveEntry.getLastModifiedDate());
                Files.setLastModifiedTime(to, fileTime);
            } else if (sevenZArchiveEntry.getHasCreationDate()) {
                FileTime fileTime = fromDate(sevenZArchiveEntry.getCreationDate());
                Files.setLastModifiedTime(to, fileTime);
            }
        });
    }

    private void extractTarEntries(
            ArchiveType archiveType,
            CopyDefinition copyDefinition,
            int index) throws IOException {
        ArchiveUtils.readTar(
                archiveType,
                copyDefinition.getFrom(),
                (tarArchiveEntry, tarArchiveInputStream) -> {
                    String name = tarArchiveEntry.getName();
                    ArchiveCopyDefinition archiveCopyDefinition =
                            findArchiveCopyDefinition(copyDefinition, name);
                    if (archiveCopyDefinition == null) {
                        return;
                    }
                    Path to =
                            copyDefinition.getTo().resolve(archiveCopyDefinition.getDestination());
                    try (OutputStream outputStream = Files.newOutputStream(to)) {
                        Path source = copyDefinition.getFrom().resolve(name);
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
                        FileTime fileTime = fromDate(tarArchiveEntry.getLastModifiedDate());
                        Files.setLastModifiedTime(to, fileTime);
                    }
                });
    }

    private void compressZipEntries(CopyDefinition copyDefinition, int index) throws IOException {
        try (ZipArchiveOutputStream zipArchiveOutputStream =
                new ZipArchiveOutputStream(copyDefinition.getTo().toFile())) {
            for (ArchiveCopyDefinition archiveCopyDefinition :
                    copyDefinition.getArchiveCopyDefinitions()) {
                Path source = copyDefinition.getFrom().resolve(archiveCopyDefinition.getSource());
                try (InputStream inputStream = Files.newInputStream(source)) {
                    ArchiveEntry archiveEntry = zipArchiveOutputStream.createArchiveEntry(
                            source.toFile(),
                            archiveCopyDefinition.getDestination());
                    Path destination =
                            copyDefinition.getTo().resolve(archiveCopyDefinition.getDestination());
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
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void compressSevenZipEntries(CopyDefinition copyDefinition, int index)
            throws IOException {
        try (SevenZOutputFile sevenZOutputFile =
                new SevenZOutputFile(copyDefinition.getTo().toFile())) {
            for (ArchiveCopyDefinition archiveCopyDefinition :
                    copyDefinition.getArchiveCopyDefinitions()) {
                Path source = copyDefinition.getFrom().resolve(archiveCopyDefinition.getSource());
                try (InputStream inputStream = Files.newInputStream(source)) {
                    SevenZArchiveEntry archiveEntry = sevenZOutputFile.createArchiveEntry(
                            source.toFile(),
                            archiveCopyDefinition.getDestination());
                    Path destination =
                            copyDefinition.getTo().resolve(archiveCopyDefinition.getDestination());
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
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void compressTarEntries(
            ArchiveType archiveType,
            CopyDefinition copyDefinition,
            int index) throws IOException {
        OutputStream outputStream =
                ArchiveUtils.outputStreamForTar(archiveType, copyDefinition.getTo());
        if (outputStream == null) {
            return;
        }
        try (TarArchiveOutputStream tarArchiveOutputStream =
                new TarArchiveOutputStream(outputStream)) {
            for (ArchiveCopyDefinition archiveCopyDefinition :
                    copyDefinition.getArchiveCopyDefinitions()) {
                Path source = copyDefinition.getFrom().resolve(archiveCopyDefinition.getSource());
                try (InputStream inputStream = Files.newInputStream(source)) {
                    ArchiveEntry archiveEntry = tarArchiveOutputStream.createArchiveEntry(
                            source.toFile(),
                            archiveCopyDefinition.getDestination());
                    Path destination =
                            copyDefinition.getTo().resolve(archiveCopyDefinition.getDestination());
                    tarArchiveOutputStream.putArchiveEntry(archiveEntry);
                    copyWithProgress(
                            index,
                            Files.size(source),
                            source,
                            destination,
                            inputStream::read,
                            tarArchiveOutputStream::write);
                    tarArchiveOutputStream.closeArchiveEntry();
                }
            }
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void fromZipToArchive(ArchiveType toType, CopyDefinition copyDefinition, int index)
            throws IOException {
        switch (toType) {
            case ZIP:
                if (allowRawZipCopy) {
                    fromZipToZipRaw(copyDefinition, index);
                } else {
                    fromZipToZip(copyDefinition, index);
                }
                return;
            case RAR:
                throw new UnsupportedOperationException("RAR compression is not supported");
            case SEVEN_ZIP:
                fromZipToSevenZip(copyDefinition, index);
                return;
            case TAR:
            case TAR_BZ2:
            case TAR_GZ:
            case TAR_LZ4:
            case TAR_LZMA:
            case TAR_XZ:
                fromZipToTar(toType, copyDefinition, index);
        }
    }

    private void fromRarToArchive(ArchiveType toType, CopyDefinition copyDefinition, int index)
            throws Exception {
        switch (toType) {
            case ZIP:
                fromRarToZip(copyDefinition, index);
                return;
            case RAR:
                throw new UnsupportedOperationException("RAR compression is not supported");
            case SEVEN_ZIP:
                fromRarToSevenZip(copyDefinition, index);
                return;
            case TAR:
            case TAR_BZ2:
            case TAR_GZ:
            case TAR_LZ4:
            case TAR_LZMA:
            case TAR_XZ:
                fromRarToTar(toType, copyDefinition, index);
        }
    }

    private void fromSevenZipToArchive(
            ArchiveType toType,
            CopyDefinition copyDefinition,
            int index) throws IOException {
        switch (toType) {
            case ZIP:
                fromSevenZipToZip(copyDefinition, index);
                return;
            case RAR:
                throw new UnsupportedOperationException("RAR compression is not supported");
            case SEVEN_ZIP:
                fromSevenZipToSevenZip(copyDefinition, index);
                return;
            case TAR:
            case TAR_BZ2:
            case TAR_GZ:
            case TAR_LZ4:
            case TAR_LZMA:
            case TAR_XZ:
                fromSevenZipToTar(toType, copyDefinition, index);
        }
    }

    private void fromTarToArchive(
            ArchiveType fromType,
            ArchiveType toType,
            CopyDefinition copyDefinition,
            int index)
            throws IOException {
        switch (toType) {
            case ZIP:
                fromTarToZip(fromType, copyDefinition, index);
                return;
            case RAR:
                throw new UnsupportedOperationException("RAR compression is not supported");
            case SEVEN_ZIP:
                fromTarToSevenZip(fromType, copyDefinition, index);
                return;
            case TAR:
            case TAR_BZ2:
            case TAR_GZ:
            case TAR_LZ4:
            case TAR_LZMA:
            case TAR_XZ:
                fromTarToTar(fromType, toType, copyDefinition, index);
        }
    }

    private void fromZipToZipRaw(
            CopyDefinition copyDefinition,
            int index) throws IOException {
        try (ZipArchiveOutputStream zipArchiveOutputStream =
                new ZipArchiveOutputStream(copyDefinition.getTo().toFile())) {
            ArchiveUtils.readZip(copyDefinition.getFrom(), (zipFile, zipArchiveEntry) -> {
                String name = zipArchiveEntry.getName();
                ArchiveCopyDefinition archiveCopyDefinition =
                        findArchiveCopyDefinition(copyDefinition, name);
                if (archiveCopyDefinition == null) {
                    return;
                }
                Path source = copyDefinition.getFrom().resolve(name);
                Path to = copyDefinition.getTo().resolve(name);
                if (!name.equals(archiveCopyDefinition.getDestination())) {
                    logger.warn(
                            "Cannot rename files inside ZIPs when using raw copy. "
                                    + "Writing '{}' as '{}'",
                            copyDefinition.getTo().resolve(archiveCopyDefinition.getDestination()),
                            to);
                }
                if (listener != null) {
                    listener.reportProgress(
                            source,
                            to,
                            index,
                            0,
                            0);
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
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void fromZipToZip(
            CopyDefinition copyDefinition,
            int index) throws IOException {
        try (ZipArchiveOutputStream zipArchiveOutputStream =
                new ZipArchiveOutputStream(copyDefinition.getTo().toFile())) {
            ArchiveUtils.readZip(
                    copyDefinition.getFrom(),
                    (zipFile, zipArchiveEntry) -> {
                        String name = zipArchiveEntry.getName();
                        ArchiveCopyDefinition archiveCopyDefinition =
                                findArchiveCopyDefinition(copyDefinition, name);
                        if (archiveCopyDefinition == null) {
                            return;
                        }
                        try (InputStream inputStream = zipFile.getInputStream(zipArchiveEntry)) {
                            toZip(
                                    index,
                                    inputStream::read,
                                    zipArchiveOutputStream,
                                    copyDefinition,
                                    archiveCopyDefinition,
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
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void fromZipToSevenZip(
            CopyDefinition copyDefinition,
            int index) throws IOException {
        try (SevenZOutputFile sevenZOutputFile =
                new SevenZOutputFile(copyDefinition.getTo().toFile())) {
            ArchiveUtils.readZip(
                    copyDefinition.getFrom(),
                    (zipFile, zipArchiveEntry) -> {
                        String name = zipArchiveEntry.getName();
                        ArchiveCopyDefinition archiveCopyDefinition =
                                findArchiveCopyDefinition(copyDefinition, name);
                        if (archiveCopyDefinition == null) {
                            return;
                        }
                        try (InputStream inputStream = zipFile.getInputStream(zipArchiveEntry)) {
                            toSevenZip(
                                    index,
                                    inputStream::read,
                                    sevenZOutputFile,
                                    copyDefinition,
                                    archiveCopyDefinition,
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
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void fromZipToTar(
            ArchiveType archiveType,
            CopyDefinition copyDefinition,
            int index) throws IOException {
        OutputStream outputStream =
                ArchiveUtils.outputStreamForTar(archiveType, copyDefinition.getTo());
        if (outputStream == null) {
            return;
        }
        try (TarArchiveOutputStream tarArchiveOutputStream =
                new TarArchiveOutputStream(outputStream)) {
            ArchiveUtils.readZip(
                    copyDefinition.getFrom(),
                    (zipFile, zipArchiveEntry) -> {
                        String name = zipArchiveEntry.getName();
                        ArchiveCopyDefinition archiveCopyDefinition =
                                findArchiveCopyDefinition(copyDefinition, name);
                        if (archiveCopyDefinition == null) {
                            return;
                        }
                        try (InputStream inputStream = zipFile.getInputStream(zipArchiveEntry)) {
                            toTar(
                                    index,
                                    inputStream::read,
                                    tarArchiveOutputStream,
                                    copyDefinition,
                                    archiveCopyDefinition,
                                    name,
                                    toDate(zipArchiveEntry.getLastModifiedTime()),
                                    zipArchiveEntry.getSize());
                        }
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void fromRarToZip(
            CopyDefinition copyDefinition,
            int index) throws Exception {
        try (ZipArchiveOutputStream zipArchiveOutputStream =
                new ZipArchiveOutputStream(copyDefinition.getTo().toFile())) {
            ArchiveUtils.readRar(
                    copyDefinition.getFrom(),
                    (archive, fileHeader) -> {
                        String name = fileHeader.getFileName();
                        ArchiveCopyDefinition archiveCopyDefinition =
                                findArchiveCopyDefinition(copyDefinition, name);
                        if (archiveCopyDefinition == null) {
                            return;
                        }
                        try (InputStream inputStream = archive.getInputStream(fileHeader)) {
                            toZip(
                                    index,
                                    inputStream::read,
                                    zipArchiveOutputStream,
                                    copyDefinition,
                                    archiveCopyDefinition,
                                    name,
                                    fromDate(fileHeader.getCTime()),
                                    fromDate(fileHeader.getMTime()),
                                    fromDate(fileHeader.getATime()),
                                    fileHeader.getFullUnpackSize());
                        }
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException | RarException e) {
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void fromRarToSevenZip(
            CopyDefinition copyDefinition,
            int index) throws Exception {
        try (SevenZOutputFile sevenZOutputFile =
                new SevenZOutputFile(copyDefinition.getTo().toFile())) {
            ArchiveUtils.readRar(
                    copyDefinition.getFrom(),
                    (archive, fileHeader) -> {
                        String name = fileHeader.getFileName();
                        ArchiveCopyDefinition archiveCopyDefinition =
                                findArchiveCopyDefinition(copyDefinition, name);
                        if (archiveCopyDefinition == null) {
                            return;
                        }
                        try (InputStream inputStream = archive.getInputStream(fileHeader)) {
                            toSevenZip(
                                    index,
                                    inputStream::read,
                                    sevenZOutputFile,
                                    copyDefinition,
                                    archiveCopyDefinition,
                                    name,
                                    fileHeader.getCTime(),
                                    fileHeader.getMTime(),
                                    fileHeader.getATime(),
                                    fileHeader.getFullUnpackSize());
                        }
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException | RarException e) {
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void fromRarToTar(
            ArchiveType archiveType,
            CopyDefinition copyDefinition,
            int index) throws Exception {
        OutputStream outputStream =
                ArchiveUtils.outputStreamForTar(archiveType, copyDefinition.getTo());
        if (outputStream == null) {
            return;
        }
        try (TarArchiveOutputStream tarArchiveOutputStream =
                new TarArchiveOutputStream(outputStream)) {
            ArchiveUtils.readRar(
                    copyDefinition.getFrom(),
                    (archive, fileHeader) -> {
                        String name = fileHeader.getFileName();
                        ArchiveCopyDefinition archiveCopyDefinition =
                                findArchiveCopyDefinition(copyDefinition, name);
                        if (archiveCopyDefinition == null) {
                            return;
                        }
                        try (InputStream inputStream = archive.getInputStream(fileHeader)) {
                            toTar(
                                    index,
                                    inputStream::read,
                                    tarArchiveOutputStream,
                                    copyDefinition,
                                    archiveCopyDefinition,
                                    name,
                                    fileHeader.getMTime(),
                                    fileHeader.getFullUnpackSize());
                        }
                    });
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException | RarException e) {
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void fromSevenZipToZip(
            CopyDefinition copyDefinition,
            int index) throws IOException {
        try (ZipArchiveOutputStream zipArchiveOutputStream =
                new ZipArchiveOutputStream(copyDefinition.getTo().toFile())) {
            ArchiveUtils.readSevenZip(
                    copyDefinition.getFrom(),
                    (sevenZFile, sevenZArchiveEntry) -> toZip(
                            index,
                            sevenZFile::read,
                            zipArchiveOutputStream,
                            copyDefinition,
                            sevenZArchiveEntry.getName(),
                            sevenZArchiveEntry.getHasCreationDate()
                                    ? fromDate(sevenZArchiveEntry.getCreationDate())
                                    : null,
                            sevenZArchiveEntry.getHasLastModifiedDate()
                                    ? fromDate(sevenZArchiveEntry.getLastModifiedDate())
                                    : null,
                            sevenZArchiveEntry.getHasAccessDate()
                                    ? fromDate(sevenZArchiveEntry.getAccessDate())
                                    : null,
                            sevenZArchiveEntry.getSize()));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void fromSevenZipToSevenZip(
            CopyDefinition copyDefinition,
            int index) throws IOException {
        try (SevenZOutputFile sevenZOutputFile =
                new SevenZOutputFile(copyDefinition.getTo().toFile())) {
            ArchiveUtils.readSevenZip(
                    copyDefinition.getFrom(),
                    (sevenZFile, sevenZArchiveEntry) -> toSevenZip(
                            index,
                            sevenZFile::read,
                            sevenZOutputFile,
                            copyDefinition,
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
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void fromSevenZipToTar(
            ArchiveType archiveType,
            CopyDefinition copyDefinition,
            int index) throws IOException {
        OutputStream outputStream =
                ArchiveUtils.outputStreamForTar(archiveType, copyDefinition.getTo());
        if (outputStream == null) {
            return;
        }
        try (TarArchiveOutputStream tarArchiveOutputStream =
                new TarArchiveOutputStream(outputStream)) {
            ArchiveUtils.readSevenZip(
                    copyDefinition.getFrom(),
                    (sevenZFile, sevenZArchiveEntry) -> toTar(
                            index,
                            sevenZFile::read,
                            tarArchiveOutputStream,
                            copyDefinition,
                            sevenZArchiveEntry.getName(),
                            sevenZArchiveEntry.getHasLastModifiedDate()
                                    ? sevenZArchiveEntry.getLastModifiedDate()
                                    : null,
                            sevenZArchiveEntry.getSize()));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void fromTarToZip(ArchiveType archiveType, CopyDefinition copyDefinition, int index)
            throws IOException {
        try (ZipArchiveOutputStream zipArchiveOutputStream =
                new ZipArchiveOutputStream(copyDefinition.getTo().toFile())) {
            ArchiveUtils.readTar(
                    archiveType,
                    copyDefinition.getFrom(),
                    (tarArchiveEntry, tarArchiveInputStream) -> toZip(
                            index,
                            tarArchiveInputStream::read,
                            zipArchiveOutputStream,
                            copyDefinition,
                            tarArchiveEntry.getName(),
                            null,
                            fromDate(tarArchiveEntry.getLastModifiedDate()),
                            null,
                            tarArchiveEntry.getRealSize()));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void fromTarToSevenZip(
            ArchiveType archiveType,
            CopyDefinition copyDefinition,
            int index)
            throws IOException {
        try (SevenZOutputFile sevenZOutputFile =
                new SevenZOutputFile(copyDefinition.getTo().toFile())) {
            ArchiveUtils.readTar(
                    archiveType,
                    copyDefinition.getFrom(),
                    (tarArchiveEntry, tarArchiveInputStream) -> toSevenZip(
                            index,
                            tarArchiveInputStream::read,
                            sevenZOutputFile,
                            copyDefinition,
                            tarArchiveEntry.getName(),
                            null,
                            tarArchiveEntry.getLastModifiedDate(),
                            null,
                            tarArchiveEntry.getRealSize()));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void fromTarToTar(
            ArchiveType fromType,
            ArchiveType toType,
            CopyDefinition copyDefinition,
            int index)
            throws IOException {
        OutputStream outputStream =
                ArchiveUtils.outputStreamForTar(toType, copyDefinition.getTo());
        if (outputStream == null) {
            return;
        }
        try (TarArchiveOutputStream tarArchiveOutputStream =
                new TarArchiveOutputStream(outputStream)) {
            ArchiveUtils.readTar(
                    fromType,
                    copyDefinition.getFrom(),
                    (tarArchiveEntry, tarArchiveInputStream) -> toTar(
                            index,
                            tarArchiveInputStream::read,
                            tarArchiveOutputStream,
                            copyDefinition,
                            tarArchiveEntry.getName(),
                            tarArchiveEntry.getLastModifiedDate(),
                            tarArchiveEntry.getRealSize()));
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e) {
            Files.delete(copyDefinition.getTo());
            throw e;
        }
    }

    private void toZip(
            int index,
            TriFunction<byte[], Integer, Integer, Integer, IOException> readFunction,
            ZipArchiveOutputStream zipArchiveOutputStream,
            CopyDefinition copyDefinition,
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
                copyDefinition,
                findArchiveCopyDefinition(copyDefinition, name),
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
            CopyDefinition copyDefinition,
            @Nullable ArchiveCopyDefinition archiveCopyDefinition,
            String name,
            @Nullable FileTime creationTime,
            @Nullable FileTime lastModifiedTime,
            @Nullable FileTime lastAccessTime,
            long size)
            throws IOException {
        if (archiveCopyDefinition == null) {
            return;
        }
        Path to = copyDefinition.getTo().resolve(archiveCopyDefinition.getDestination());
        ZipArchiveEntry zae = new ZipArchiveEntry(archiveCopyDefinition.getDestination());
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
        Path source = copyDefinition.getFrom().resolve(name);
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
            CopyDefinition copyDefinition,
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
                copyDefinition,
                findArchiveCopyDefinition(copyDefinition, name),
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
            CopyDefinition copyDefinition,
            @Nullable ArchiveCopyDefinition archiveCopyDefinition,
            String name,
            @Nullable Date creationDate,
            @Nullable Date lastModifiedDate,
            @Nullable Date lastAccessDate,
            long size)
            throws IOException {
        if (archiveCopyDefinition == null) {
            return;
        }
        Path to = copyDefinition.getTo().resolve(archiveCopyDefinition.getDestination());
        SevenZArchiveEntry zae = new SevenZArchiveEntry();
        zae.setName(archiveCopyDefinition.getDestination());
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
        Path source = copyDefinition.getFrom().resolve(name);
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
            TarArchiveOutputStream tarArchiveOutputStream,
            CopyDefinition copyDefinition,
            String name,
            @Nullable Date lastModifiedDate,
            long size)
            throws IOException {
        toTar(
                index,
                readFunction,
                tarArchiveOutputStream,
                copyDefinition,
                findArchiveCopyDefinition(copyDefinition, name),
                name,
                lastModifiedDate,
                size);
    }

    private void toTar(
            int index,
            TriFunction<byte[], Integer, Integer, Integer, IOException> readFunction,
            TarArchiveOutputStream tarArchiveOutputStream,
            CopyDefinition copyDefinition,
            @Nullable ArchiveCopyDefinition archiveCopyDefinition,
            String name,
            @Nullable Date lastModifiedDate,
            long size)
            throws IOException {
        if (archiveCopyDefinition == null) {
            return;
        }
        Path to = copyDefinition.getTo().resolve(archiveCopyDefinition.getDestination());
        TarArchiveEntry zae = new TarArchiveEntry(archiveCopyDefinition.getDestination());
        zae.setSize(size);
        if (lastModifiedDate != null) {
            zae.setModTime(lastModifiedDate);
        }
        Path source = copyDefinition.getFrom().resolve(name);
        tarArchiveOutputStream.putArchiveEntry(zae);
        copyWithProgress(
                index,
                size,
                source,
                to,
                readFunction,
                tarArchiveOutputStream::write);
        tarArchiveOutputStream.closeArchiveEntry();
    }

    @Nullable
    private Date toDate(@Nullable FileTime fileTime) {
        return fileTime != null ? new Date(fileTime.toMillis()) : null;
    }

    @Nullable
    private static FileTime fromDate(@Nullable Date date) {
        return date != null ? FileTime.fromMillis(date.getTime()) : null;
    }

    @Nullable
    private ArchiveCopyDefinition findArchiveCopyDefinition(
            CopyDefinition copyDefinition,
            String name) {
        return copyDefinition
                .getArchiveCopyDefinitions()
                .stream()
                .filter(ad -> ad.getSource().equals(name))
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
