package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.Patterns;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.zip.CRC32;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileScanner {

    private static final Logger logger = LoggerFactory.getLogger(FileScanner.class);
    private static final int STREAM_BUFFER_LENGTH = 1024;

    // TODO add callback to report progress in scanning
    // TODO make it multithreaded (first list files, then iterate list, don't scan at once)

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
        Long size;
        Digest digest;
        String archivePath;
    }


    public static ImmutableList<Result> scan(Path directory) {
        ImmutableList.Builder<Result> builder = ImmutableList.builder();
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(
                        Path file, BasicFileAttributes attrs) throws IOException {
                    FileVisitResult result = super.visitFile(file, attrs);
                    if (Files.isRegularFile(file)) {
                        builder.addAll(scanFile(file));
                    }
                    return result;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    logger.warn("Failed to scan '{}'", file, exc);
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });
            return builder.build();
        } catch (Exception e) {
            logger.error("Could not scan '{}'", directory, e);
            return ImmutableList.of();
        }
    }

    private static ImmutableList<Result> scanFile(Path file) {
        try {
            ImmutableList.Builder<Result> builder = ImmutableList.builder();
            if (Patterns.ZIP.matcher(file.getFileName().toString()).find()) {
                try (ZipFile zipFile = new ZipFile(file.toFile())) {
                    Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
                    while (entries.hasMoreElements()) {
                        ZipArchiveEntry entry = entries.nextElement();
                        try (InputStream entryInputStream = zipFile.getInputStream(entry)) {
                            builder.add(new Result(
                                    file,
                                    entry.getSize(),
                                    processDigest(entryInputStream),
                                    entry.getName()));
                        }
                    }
                }
            } else {
                try (InputStream inputStream = Files.newInputStream(file)) {
                    builder.add(new Result(
                            file,
                            Files.size(file),
                            processDigest(inputStream),
                            null));
                }
            }
            return builder.build();
        } catch (Exception e) {
            logger.error("Could not read file '{}'", file, e);
            return ImmutableList.of();
        }
    }

    private static Result.Digest processDigest(InputStream inputStream) throws IOException {
        Result.Digest digest;
        MessageDigest md5 = DigestUtils.getMd5Digest();
        MessageDigest sha1 = DigestUtils.getSha1Digest();
        CRC32 crc32 = new CRC32();
        final byte[] buffer = new byte[STREAM_BUFFER_LENGTH];
        int read = inputStream.read(buffer, 0, STREAM_BUFFER_LENGTH);
        while (read > -1) {
            crc32.update(buffer, 0, read);
            md5.update(buffer, 0, read);
            sha1.update(buffer, 0, read);
            read = inputStream.read(buffer, 0, STREAM_BUFFER_LENGTH);
        }
        digest = new Result.Digest(
                Long.toHexString(crc32.getValue()),
                Hex.encodeHexString(md5.digest()),
                Hex.encodeHexString(sha1.digest()));
        return digest;
    }

}
