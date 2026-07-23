package io.github.datromtool.util;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import io.github.datromtool.io.ArchiveType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;

import static java.nio.file.Files.newInputStream;
import static java.nio.file.StandardOpenOption.CREATE;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArchiveUtils {

    public static void deleteFolder(@Nullable Path folder) throws IOException {
        if (folder == null) {
            return;
        }
        Files.walkFileTree(folder, DeleterFileVisitor.getInstance());
        Files.deleteIfExists(folder);
    }

    public static String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    @Nullable
    public static InputStream inputStreamForTar(ArchiveType archiveType, Path file)
            throws IOException {
        return switch (archiveType) {
            case TAR -> newInputStream(file);
            case TAR_BZ2 -> newBz2InputStream(file);
            case TAR_GZ -> newGzipInputStream(file);
            case TAR_LZ4 -> newLz4InputStream(file);
            case TAR_LZMA -> newLzmaInputStream(file);
            case TAR_XZ -> newXzInputStream(file);
            default -> null;
        };
    }

    public static BZip2CompressorInputStream newBz2InputStream(Path file) throws IOException {
        return new BZip2CompressorInputStream(newInputStream(file));
    }

    public static GzipCompressorInputStream newGzipInputStream(Path file) throws IOException {
        return new GzipCompressorInputStream(newInputStream(file));
    }

    public static FramedLZ4CompressorInputStream newLz4InputStream(Path file) throws IOException {
        return new FramedLZ4CompressorInputStream(newInputStream(file));
    }

    public static LZMACompressorInputStream newLzmaInputStream(Path file) throws IOException {
        return new LZMACompressorInputStream(newInputStream(file));
    }

    public static XZCompressorInputStream newXzInputStream(Path file) throws IOException {
        return new XZCompressorInputStream(newInputStream(file));
    }

    @Nullable
    public static OutputStream outputStreamForTar(ArchiveType archiveType, Path file)
            throws IOException {
        return switch (archiveType) {
            case TAR -> Files.newOutputStream(file, CREATE);
            case TAR_BZ2 -> new BZip2CompressorOutputStream(Files.newOutputStream(file, CREATE));
            case TAR_GZ -> new GzipCompressorOutputStream(Files.newOutputStream(file, CREATE));
            case TAR_LZ4 -> new FramedLZ4CompressorOutputStream(Files.newOutputStream(file, CREATE));
            case TAR_LZMA -> new LZMACompressorOutputStream(Files.newOutputStream(file, CREATE));
            case TAR_XZ -> new XZCompressorOutputStream(Files.newOutputStream(file, CREATE));
            default -> null;
        };
    }

    public static <T extends Throwable> void readZip(
            Path file,
            ThrowingBiConsumer<ZipFile, ZipArchiveEntry, T> consumer) throws IOException, T {
        try (ZipFile zipFile = ZipFile.builder().setPath(file).get()) {
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
        try (SevenZFile sevenZFile = SevenZFile.builder().setPath(file).get()) {
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
            try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream)) {
                TarArchiveEntry tarArchiveEntry;
                while ((tarArchiveEntry = tarArchiveInputStream.getNextEntry()) != null) {
                    if (!tarArchiveEntry.isFile()
                            || !tarArchiveInputStream.canReadEntryData(tarArchiveEntry)) {
                        continue;
                    }
                    consumer.accept(tarArchiveEntry, tarArchiveInputStream);
                }
            }
        } else {
            log.warn("Unsupported TAR archive compression for '{}'", file);
        }
    }

    @FunctionalInterface
    public interface ThrowingBiConsumer<T, D, E extends Throwable> {

        void accept(T t, D d) throws E;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class DeleterFileVisitor extends SimpleFileVisitor<Path> {

        private static final DeleterFileVisitor INSTANCE = new DeleterFileVisitor();

        public static DeleterFileVisitor getInstance() {
            return INSTANCE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.deleteIfExists(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.deleteIfExists(dir);
            return FileVisitResult.CONTINUE;
        }
    }
}
