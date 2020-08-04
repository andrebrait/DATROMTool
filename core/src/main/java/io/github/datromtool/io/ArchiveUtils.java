package io.github.datromtool.io;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;

import static java.nio.file.StandardOpenOption.CREATE;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArchiveUtils {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveUtils.class);

    @Nullable
    public static InputStream inputStreamForTar(ArchiveType archiveType, Path file)
            throws IOException {
        switch (archiveType) {
            case TAR:
                return Files.newInputStream(file);
            case TAR_BZ2:
                return newBz2InputStream(file);
            case TAR_GZ:
                return newGzipInputStream(file);
            case TAR_LZ4:
                return newLz4InputStream(file);
            case TAR_LZMA:
                return newLzmaInputStream(file);
            case TAR_XZ:
                return newXzInputStream(file);
            default:
                return null;
        }
    }

    public static BZip2CompressorInputStream newBz2InputStream(Path file) throws IOException {
        return new BZip2CompressorInputStream(Files.newInputStream(file));
    }

    public static GzipCompressorInputStream newGzipInputStream(Path file) throws IOException {
        return new GzipCompressorInputStream(Files.newInputStream(file));
    }

    public static FramedLZ4CompressorInputStream newLz4InputStream(Path file) throws IOException {
        return new FramedLZ4CompressorInputStream(Files.newInputStream(file));
    }

    public static LZMACompressorInputStream newLzmaInputStream(Path file) throws IOException {
        return new LZMACompressorInputStream(Files.newInputStream(file));
    }

    public static XZCompressorInputStream newXzInputStream(Path file) throws IOException {
        return new XZCompressorInputStream(Files.newInputStream(file));
    }

    @Nullable
    public static OutputStream outputStreamForTar(ArchiveType archiveType, Path file)
            throws IOException {
        switch (archiveType) {
            case TAR:
                return Files.newOutputStream(file, CREATE);
            case TAR_BZ2:
                return new BZip2CompressorOutputStream(Files.newOutputStream(file, CREATE));
            case TAR_GZ:
                return new GzipCompressorOutputStream(Files.newOutputStream(file, CREATE));
            case TAR_LZ4:
                return new FramedLZ4CompressorOutputStream(Files.newOutputStream(file, CREATE));
            case TAR_LZMA:
                return new LZMACompressorOutputStream(Files.newOutputStream(file, CREATE));
            case TAR_XZ:
                return new XZCompressorOutputStream(Files.newOutputStream(file, CREATE));
            default:
                return null;
        }
    }

    public static <T extends Throwable> void readZip(
            Path file,
            ThrowingBiConsumer<ZipFile, ZipArchiveEntry, T> consumer) throws IOException, T {
        try (ZipFile zipFile = new ZipFile(file.toFile())) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntriesInPhysicalOrder();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry zipArchiveEntry = entries.nextElement();
                if (zipArchiveEntry.isDirectory() || zipArchiveEntry.isUnixSymlink()) {
                    continue;
                }
                consumer.accept(zipFile, zipArchiveEntry);
            }
        }
    }

    public static <T extends Throwable> void readRar(
            Path file,
            ThrowingBiConsumer<Archive, FileHeader, T> consumer)
            throws IOException, RarException, T {
        try (Archive archive = new Archive(file.toFile())) {
            for (FileHeader fileHeader : archive) {
                if (!fileHeader.isFileHeader() || fileHeader.isDirectory()) {
                    continue;
                }
                consumer.accept(archive, fileHeader);
            }
        }
    }

    public static <T extends Throwable> void readSevenZip(
            Path file,
            ThrowingBiConsumer<SevenZFile, SevenZArchiveEntry, T> consumer)
            throws IOException, T {
        try (SevenZFile sevenZFile = new SevenZFile(file.toFile())) {
            SevenZArchiveEntry sevenZArchiveEntry;
            while ((sevenZArchiveEntry = sevenZFile.getNextEntry()) != null) {
                if (sevenZArchiveEntry.isDirectory() || sevenZArchiveEntry.isAntiItem()) {
                    continue;
                }
                consumer.accept(sevenZFile, sevenZArchiveEntry);
            }
        }
    }

    public static <T extends Throwable> void readTar(
            ArchiveType archiveType,
            Path file,
            ThrowingBiConsumer<TarArchiveEntry, TarArchiveInputStream, T> consumer)
            throws IOException, T {
        InputStream inputStream = inputStreamForTar(archiveType, file);
        if (inputStream != null) {
            try (TarArchiveInputStream tarArchiveInputStream =
                    new TarArchiveInputStream(inputStream)) {
                TarArchiveEntry tarArchiveEntry;
                while ((tarArchiveEntry = tarArchiveInputStream.getNextTarEntry()) != null) {
                    if (!tarArchiveEntry.isFile()
                            || !tarArchiveInputStream.canReadEntryData(tarArchiveEntry)) {
                        continue;
                    }
                    consumer.accept(tarArchiveEntry, tarArchiveInputStream);
                }
            }
        } else {
            logger.warn("Unsupported TAR archive compression for '{}'", file);
        }
    }

    @FunctionalInterface
    public interface ThrowingBiConsumer<T, D, E extends Throwable> {

        void accept(T t, D d) throws E;
    }

}
