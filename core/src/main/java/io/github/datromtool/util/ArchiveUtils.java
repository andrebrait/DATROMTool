package io.github.datromtool.util;

import com.github.junrar.Archive;
import com.github.junrar.exception.BadRarArchiveException;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.io.ArchiveType;
import io.github.datromtool.io.UnrarArchiveEntry;
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
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.file.Files.newInputStream;
import static java.nio.file.StandardOpenOption.CREATE;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArchiveUtils {

    public static String normalizePath(String path) {
        return path.indexOf('\\') > 0
                ? path.replace('\\', '/')
                : path;
    }

    @Nullable
    public static InputStream inputStreamForTar(ArchiveType archiveType, Path file)
            throws IOException {
        switch (archiveType) {
            case TAR:
                return newInputStream(file);
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

    private static final Pattern RAR_LIST =
            Pattern.compile("^\\s*\\S+\\s+([0-9]+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S.+\\S)\\s*$");

    private static volatile Boolean isUnrarAvailableCache;

    public static boolean isUnrarAvailable() {
        Boolean value = isUnrarAvailableCache;
        if (value != null) {
            return value;
        }
        synchronized (ArchiveUtils.class) {
            value = isUnrarAvailableCache;
            if (value != null) {
                return value;
            }
            ProcessBuilder pb = new ProcessBuilder("unrar", "-inul");
            try {
                Process process = pb.start();
                value = process.waitFor() == 0;
            } catch (IOException | InterruptedException e) {
                log.error("Could not check if 'unrar' is available", e);
                value = false;
            }
            isUnrarAvailableCache = value;
            return value;
        }
    }

    private static boolean isRarFileOk(Path path) {
        ProcessBuilder pb = new ProcessBuilder("unrar", "t", path.toAbsolutePath().normalize().toString());
        try {
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            log.error("'unrar' could not test file '{}'", path, e);
            return false;
        }
    }

    public static ImmutableList<UnrarArchiveEntry> listRarEntriesWithUnrar(Path path)
            throws IOException, RarException {
        if (!isUnrarAvailable()) {
            throw new UnsupportedOperationException("'unrar' is not available");
        }
        if (!isRarFileOk(path)) {
            throw new BadRarArchiveException();
        }
        String[] arguments = {"unrar", "l", path.toAbsolutePath().normalize().toString()};
        ProcessBuilder pb = new ProcessBuilder(arguments);
        try {
            Process process = pb.start();
            ImmutableList<UnrarArchiveEntry> fileList = readStdout(process)
                    .map(RAR_LIST::matcher)
                    .filter(Matcher::matches)
                    .map(m -> UnrarArchiveEntry.builder()
                            .name(normalizePath(m.group(4)))
                            .size(Long.parseLong(m.group(1)))
                            .modificationTime(LocalDateTime.parse(String.format(
                                    "%sT%s:00",
                                    m.group(2),
                                    m.group(3))))
                            .build())
                    .filter(e -> e.getSize() > 0)
                    .collect(ImmutableList.toImmutableList());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Unexpected error while running '{}'. Exit code: {}", String.join(" ", arguments), exitCode);
            }
            return fileList;
        } catch (InterruptedException e) {
            log.error("'unrar' could not list contents of file '{}'", path, e);
            return ImmutableList.of();
        }
    }

    public static <T extends Throwable> void readRarWithUnrar(
            Path path,
            Set<String> desiredEntryNames,
            ThrowingBiConsumer<UnrarArchiveEntry, InputStream, T> consumer)
            throws IOException, RarException, T {
        ImmutableList<UnrarArchiveEntry> allEntries = listRarEntriesWithUnrar(path);
        ImmutableList<UnrarArchiveEntry> desiredEntries = allEntries.stream()
                .filter(e -> desiredEntryNames.contains(e.getName()))
                .collect(ImmutableList.toImmutableList());
        ImmutableList<String> exclusions = allEntries.stream()
                .map(UnrarArchiveEntry::getName)
                .filter(name -> !desiredEntryNames.contains(name))
                .collect(ImmutableList.toImmutableList());
        String[] arguments = {
                "unrar",
                "p",
                "-inul",
                "-x@",
                path.toAbsolutePath().normalize().toString()
        };
        ProcessBuilder pb = new ProcessBuilder(arguments);
        try {
            Process process = pb.start();
            BufferedWriter writer =
                    new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            for (String exclusion : exclusions) {
                writer.write(exclusion);
                writer.newLine();
            }
            writer.flush();
            writer.close();
            InputStream processInputStream = process.getInputStream();
            for (UnrarArchiveEntry desiredFile : desiredEntries) {
                consumer.accept(desiredFile, processInputStream);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Unexpected error while running '{}'. Exit code: {}", String.join(" ", arguments), exitCode);
            }
        } catch (InterruptedException e) {
            log.error("'unrar' could not read contents of file '{}'", path, e);
        }
    }

    public static Stream<String> readStdout(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Stream.Builder<String> sb = Stream.builder();
        String currLine;
        while ((currLine = reader.readLine()) != null) {
            sb.accept(currLine);
        }
        return sb.build();
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
            log.warn("Unsupported TAR archive compression for '{}'", file);
        }
    }

    @FunctionalInterface
    public interface ThrowingBiConsumer<T, D, E extends Throwable> {

        void accept(T t, D d) throws E;
    }

}
