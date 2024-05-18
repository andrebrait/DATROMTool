package io.github.datromtool.util;

import com.github.junrar.Archive;
import com.github.junrar.exception.BadRarArchiveException;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.SystemUtils;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.github.datromtool.SystemUtils.OperatingSystem.WINDOWS;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArchiveUtils {

    public static volatile Path tempBinDir;

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

    private static final Pattern RAR_LIST =
            Pattern.compile("^\\s*\\S+\\s+([0-9]+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S.+\\S)\\s*$");

    private static final Pattern COMMAND_NOT_FOUND =
            Pattern.compile("error=2, (No such file or directory|The system cannot find the file specified)", CASE_INSENSITIVE);

    private static volatile String unrarPath;

    public static String findExecutableOnPath(String name) {
        if (SystemUtils.OPERATING_SYSTEM == WINDOWS) {
            name = name + ".exe";
        }
        for (String dirname : System.getenv("PATH").split(File.pathSeparator)) {
            File file = new File(dirname, name);
            if (file.isFile() && file.canExecute()) {
                String absolutePath = file.getAbsolutePath();
                log.info("Found '{}' in '{}'", name, absolutePath);
                return absolutePath;
            } else {
                log.debug("Could not find '{}' in '{}'", name, dirname);
            }
        }
        log.warn("Could not find '{}' in the PATH", name);
        return null;
    }

    @Nullable
    public static Path getUnrarPath() {
        if (isUnrarAvailable()) {
            return Paths.get(unrarPath);
        }
        return null;
    }

    private static volatile Boolean isUnrarAvailableCache;

    public static boolean isUnrarAvailable() {
        return isUnrarAvailable(null);
    }

    public static boolean isUnrarAvailable(@Nullable Path customPath) {
        Boolean value = isUnrarAvailableCache;
        if (value != null) {
            return value;
        }
        synchronized (ArchiveUtils.class) {
            value = isUnrarAvailableCache;
            if (value != null) {
                return value;
            }
            if (customPath != null) {
                String customPathStr = customPath.toAbsolutePath().normalize().toString();
                value = checkUnrarPath(customPathStr);
                if (value) {
                    unrarPath = customPathStr;
                }
            } else {
                unrarPath = findExecutableOnPath("unrar");
            }
            if (value == null || !value) {
                value = checkUnrarPath(unrarPath);
                if (!value) {
                    unrarPath = getEmbeddedUnrarPath();
                    value = checkUnrarPath(ArchiveUtils.unrarPath);
                }
            }
            isUnrarAvailableCache = value;
            return value;
        }
    }

    private static String getEmbeddedUnrarPath() {
        String embeddedUnrarPath = null;
        createTempBinDir();
        if (tempBinDir != null) {
            switch (SystemUtils.OPERATING_SYSTEM) {
                case WINDOWS -> {
                    switch (SystemUtils.ARCHITECTURE) {
                        case X86_32 -> embeddedUnrarPath = copyToTempBinDir(
                                "bin/unrar/windows/unrar-x32.exe",
                                "unrar.exe");
                        case X86_64 -> embeddedUnrarPath = copyToTempBinDir(
                                "bin/unrar/windows/unrar-x64.exe",
                                "unrar.exe");
                    }
                }
                case LINUX -> embeddedUnrarPath = switch (SystemUtils.ARCHITECTURE) {
                    case X86_32 -> copyToTempBinDir("bin/unrar/linux/unrar-x32", "unrar");
                    case X86_64 -> copyToTempBinDir("bin/unrar/linux/unrar-x64", "unrar");
                    case ARM_32, ARM_64 -> copyToTempBinDir("bin/unrar/linux/unrar-arm", "unrar");
                    case POWER_PC_32, POWER_PC_64 -> copyToTempBinDir("bin/unrar/linux/unrar-ppc", "unrar");
                    default -> null;
                };
                case BSD -> embeddedUnrarPath = switch (SystemUtils.ARCHITECTURE) {
                    case X86_32 -> copyToTempBinDir("bin/unrar/bsd/unrar-x32", "unrar");
                    case X86_64 -> copyToTempBinDir("bin/unrar/bsd/unrar-x64", "unrar");
                    default -> null;
                };
                case OSX -> embeddedUnrarPath = switch (SystemUtils.ARCHITECTURE) {
                    case X86_64 -> copyToTempBinDir("bin/unrar/macos/unrar-x64", "unrar");
                    case ARM_64 -> copyToTempBinDir("bin/unrar/macos/unrar-arm64", "unrar");
                    default -> null;
                };
            }
        }
        if (embeddedUnrarPath == null) {
            log.warn("Could not find a suitable version of unrar for this combination of OS and architecture");
        }
        return embeddedUnrarPath;
    }

    private static boolean checkUnrarPath(String unrarPath) {
        return unrarPath != null && checkProcess(unrarPath, "-inul");
    }

    private static final Pattern SEVEN_ZIP_LIST =
            Pattern.compile("^\\s*(\\S+)\\s+(\\S+)\\s+\\S+\\s+([0-9]+)\\s+[0-9]+\\s+(\\S.+\\S)\\s*$");

    private static volatile String sevenZipPath;

    @Nullable
    public static Path getSevenZipPath() {
        if (isSevenZipAvailable()) {
            return Paths.get(sevenZipPath);
        }
        return null;
    }

    private static volatile Boolean isSevenZipAvailableCache;

    public static boolean isSevenZipAvailable() {
        return isSevenZipAvailable(null);
    }

    public static boolean isSevenZipAvailable(@Nullable Path customPath) {
        Boolean value = isSevenZipAvailableCache;
        if (value != null) {
            return value;
        }
        synchronized (ArchiveUtils.class) {
            value = isSevenZipAvailableCache;
            if (value != null) {
                return value;
            }
            if (customPath != null) {
                String customPathStr = customPath.toAbsolutePath().normalize().toString();
                value = checkSevenZipPath(customPathStr);
                if (value) {
                    sevenZipPath = customPathStr;
                }
            } else {
                sevenZipPath = findExecutableOnPath("7z");
            }
            if (value == null || !value) {
                sevenZipPath = getEmbeddedSevenZipPath();
                value = checkSevenZipPath(sevenZipPath);
            }
            isSevenZipAvailableCache = value;
            return value;
        }
    }

    private static String getEmbeddedSevenZipPath() {
        String embeddedSevenZipPath = null;
        createTempBinDir();
        if (tempBinDir != null) {
            switch (SystemUtils.OPERATING_SYSTEM) {
                case WINDOWS -> {
                    switch (SystemUtils.ARCHITECTURE) {
                        case X86_32 -> {
                            embeddedSevenZipPath = copyToTempBinDir("bin/7zip/windows/x32/7z.exe", "7z.exe");
                            copyToTempBinDir("bin/7zip/windows/x32/7z.dll", "7z.dll");
                        }
                        case X86_64 -> {
                            embeddedSevenZipPath = copyToTempBinDir("bin/7zip/windows/x64/7z.exe", "7z.exe");
                            copyToTempBinDir("bin/7zip/windows/x64/7z.dll", "7z.dll");
                        }
                        case ARM_64 -> {
                            embeddedSevenZipPath = copyToTempBinDir("bin/7zip/windows/arm64/7z.exe", "7z.exe");
                            copyToTempBinDir("bin/7zip/windows/arm64/7z.dll", "7z.dll");
                        }
                    }
                }
                case LINUX -> embeddedSevenZipPath = switch (SystemUtils.ARCHITECTURE) {
                    case X86_32 -> copyToTempBinDir("bin/7zip/linux/x32/7zzs", "7zzs");
                    case X86_64 -> copyToTempBinDir("bin/7zip/linux/x64/7zzs", "7zzs");
                    case ARM_32 -> copyToTempBinDir("bin/7zip/linux/arm/7zzs", "7zzs");
                    case ARM_64 -> copyToTempBinDir("bin/7zip/linux/arm64/7zzs", "7zzs");
                    default -> null;
                };
                case OSX -> embeddedSevenZipPath = switch (SystemUtils.ARCHITECTURE) {
                    case X86_64, ARM_64 -> copyToTempBinDir("bin/7zip/macos/7zz", "7zz");
                    default -> null;
                };
            }
        }
        if (embeddedSevenZipPath == null) {
            log.warn("Could not find a suitable version of 7-Zip for this combination of OS and architecture");
        }
        return embeddedSevenZipPath;
    }

    private static boolean checkSevenZipPath(String sevenZipPath) {
        return sevenZipPath != null && checkProcess(sevenZipPath);
    }

    private static boolean checkProcess(String... args) {
        ProcessBuilder pb = new ProcessBuilder(args);
        try {
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (IOException e) {
            if (!isFileNotFoundError(e)) {
                log.error("Failed to run '{}'", String.join(" ", args), e);
            }
            return false;
        } catch (InterruptedException e) {
            log.error("Failed to run '{}'", String.join(" ", args), e);
            return false;
        }
    }

    private static String copyToTempBinDir(String path, String destination) {
        Path finalDestination = tempBinDir.resolve(destination);
        try (InputStream stream = ClassLoader.getSystemResource(path).openStream()) {
            Files.copy(stream, finalDestination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Copied '{}' to '{}'", path, finalDestination);
            File file = finalDestination.toFile();
            if (!file.canExecute() && !file.setExecutable(true)) {
                if (SystemUtils.OPERATING_SYSTEM != WINDOWS) {
                    log.warn("Failed to make '{}' executable", finalDestination);
                }
            }
            return finalDestination.toString();
        } catch (IOException e) {
            log.error("Could not copy file '{}' to '{}'", path, finalDestination, e);
            return null;
        }
    }

    private static void createTempBinDir() {
        if (tempBinDir != null) {
            return;
        }
        synchronized (ArchiveUtils.class) {
            if (tempBinDir != null) {
                return;
            }
            try {
                tempBinDir = Files.createTempDirectory("datromtool_bin_");
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        deleteFolder(tempBinDir);
                    } catch (Exception e) {
                        log.error("Could not delete temporary folder '{}'", tempBinDir, e);
                    }
                }));
            } catch (IOException e) {
                log.error("Could not create temporary directory", e);
            }
        }
    }

    private static boolean isFileNotFoundError(IOException e) {
        while (e.getCause() instanceof IOException) {
            e = (IOException) e.getCause();
        }
        return COMMAND_NOT_FOUND.matcher(e.getMessage()).find();
    }

    private static boolean isBadRarFile(Path path) {
        if (isUnrarAvailable()) {
            ProcessBuilder pb = new ProcessBuilder(unrarPath, "t", path.toAbsolutePath().normalize().toString());
            try {
                Process process = pb.start();
                return process.waitFor() != 0;
            } catch (IOException | InterruptedException e) {
                log.error("'unrar' could not test file '{}'", path, e);
                return false;
            }
        } else if (isSevenZipAvailable()) {
            ProcessBuilder pb = new ProcessBuilder(sevenZipPath, "t", path.toAbsolutePath().normalize().toString());
            try {
                Process process = pb.start();
                return process.waitFor() != 0;
            } catch (IOException | InterruptedException e) {
                log.error("'7z' could not test file '{}'", path, e);
                return false;
            }
        } else {
            return false;
        }
    }

    public static ImmutableList<UnrarArchiveEntry> listRarEntriesWithUnrar(Path path)
            throws IOException, RarException {
        if (!isUnrarAvailable()) {
            throw new UnsupportedOperationException("'unrar' is not available");
        }
        if (isBadRarFile(path)) {
            throw new BadRarArchiveException();
        }
        String[] arguments = {unrarPath, "l", path.toAbsolutePath().normalize().toString()};
        ProcessBuilder pb = new ProcessBuilder(arguments);
        try {
            Process process = pb.start();
            ImmutableList<UnrarArchiveEntry> fileList = readStdout(process)
                    .map(RAR_LIST::matcher)
                    .filter(Matcher::matches)
                    .map(m -> UnrarArchiveEntry.builder()
                            .name(m.group(4))
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
        List<String> arguments = ImmutableList.<String>builder()
                .add(unrarPath)
                .add("p")
                .add("-inul")
                .add(path.toAbsolutePath().normalize().toString())
                .addAll(desiredEntries.stream().map(UnrarArchiveEntry::getName).iterator())
                .build();
        ProcessBuilder pb = new ProcessBuilder(arguments);
        try {
            Process process = pb.start();
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

    public static ImmutableList<UnrarArchiveEntry> listRarEntriesWithSevenZip(Path path)
            throws IOException, RarException {
        if (!isSevenZipAvailable()) {
            throw new UnsupportedOperationException("'7z' is not available");
        }
        if (isBadRarFile(path)) {
            throw new BadRarArchiveException();
        }
        String[] arguments = {sevenZipPath, "l", "-ba", path.toAbsolutePath().normalize().toString()};
        ProcessBuilder pb = new ProcessBuilder(arguments);
        try {
            Process process = pb.start();
            ImmutableList<UnrarArchiveEntry> fileList = readStdout(process)
                    .map(SEVEN_ZIP_LIST::matcher)
                    .filter(Matcher::matches)
                    .map(m -> UnrarArchiveEntry.builder()
                            .name(m.group(4))
                            .size(Long.parseLong(m.group(3)))
                            .modificationTime(LocalDateTime.parse(String.format(
                                    "%sT%s",
                                    m.group(1),
                                    m.group(2))))
                            .build())
                    .filter(e -> e.getSize() > 0)
                    .collect(ImmutableList.toImmutableList());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Unexpected error while running '{}'. Exit code: {}", String.join(" ", arguments), exitCode);
            }
            return fileList;
        } catch (InterruptedException e) {
            log.error("'7z' could not list contents of file '{}'", path, e);
            return ImmutableList.of();
        }
    }

    public static <T extends Throwable> void readRarWithSevenZip(
            Path path,
            Set<String> desiredEntryNames,
            ThrowingBiConsumer<UnrarArchiveEntry, InputStream, T> consumer)
            throws IOException, RarException, T {
        ImmutableList<UnrarArchiveEntry> allEntries = listRarEntriesWithSevenZip(path);
        ImmutableList<UnrarArchiveEntry> desiredEntries = allEntries.stream()
                .filter(e -> desiredEntryNames.contains(e.getName()))
                .collect(ImmutableList.toImmutableList());
        List<String> arguments = ImmutableList.<String>builder()
                .add(sevenZipPath)
                .add("e")
                .add("-so")
                .add("-bd")
                .add("-ba")
                .add(path.toAbsolutePath().normalize().toString())
                .addAll(desiredEntries.stream().map(UnrarArchiveEntry::getName).iterator())
                .build();
        ProcessBuilder pb = new ProcessBuilder(arguments);
        try {
            Process process = pb.start();
            InputStream processInputStream = process.getInputStream();
            for (UnrarArchiveEntry desiredFile : desiredEntries) {
                consumer.accept(desiredFile, processInputStream);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Unexpected error while running '{}'. Exit code: {}", String.join(" ", arguments), exitCode);
            }
        } catch (InterruptedException e) {
            log.error("'7z' could not read contents of file '{}'", path, e);
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
