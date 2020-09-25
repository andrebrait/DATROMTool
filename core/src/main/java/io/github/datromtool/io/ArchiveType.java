package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.compress.compressors.bzip2.BZip2Utils;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.commons.compress.compressors.lzma.LZMAUtils;
import org.apache.commons.compress.compressors.xz.XZUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

@AllArgsConstructor
public enum ArchiveType {

    ZIP(ImmutableList.of("zip"), true, s -> Constants.ZIP.matcher(s).find()),
    RAR(ImmutableList.of("rar"), false, s -> Constants.RAR.matcher(s).find()),
    SEVEN_ZIP(
            ImmutableList.of("7z"),
            true,
            s -> Constants.SEVEN_ZIP.matcher(s).find()
                    && !Constants.TAR_7Z.matcher(s).find()),
    TAR(ImmutableList.of("tar"), true, s -> Constants.TAR.matcher(s).find()),
    TAR_BZ2(
            ImmutableList.of("tar.bz2"),
            true,
            s -> BZip2Utils.isCompressedFilename(s)
                    && Constants.TAR.matcher(BZip2Utils.getUncompressedFilename(s)).find()),
    TAR_GZ(
            ImmutableList.of("tar.gz"),
            true,
            s -> GzipUtils.isCompressedFilename(s)
                    && Constants.TAR.matcher(GzipUtils.getUncompressedFilename(s)).find()),
    TAR_LZ4(ImmutableList.of("tar.lz4"), true, s -> Constants.TAR_LZ4.matcher(s).find()),
    TAR_LZMA(
            ImmutableList.of("tar.lzma"), LZMAUtils.isLZMACompressionAvailable(),
            s -> LZMAUtils.isLZMACompressionAvailable()
                    && LZMAUtils.isCompressedFilename(s)
                    && Constants.TAR.matcher(LZMAUtils.getUncompressedFilename(s)).find()),
    TAR_XZ(
            ImmutableList.of("tar.xz"), XZUtils.isXZCompressionAvailable(),
            s -> XZUtils.isXZCompressionAvailable()
                    && XZUtils.isCompressedFilename(s)
                    && Constants.TAR.matcher(XZUtils.getUncompressedFilename(s)).find());

    @Getter
    private final ImmutableList<String> aliases;
    @Getter
    private final boolean availableAsOutput;
    private final Predicate<String> predicate;

    @Nullable
    public static ArchiveType parse(Path file) {
        if (Files.isDirectory(file)) {
            return null;
        }
        String fileName = file.getFileName().toString();
        return parse(fileName);
    }

    @Nullable
    public static ArchiveType parse(String fileName) {
        for (ArchiveType value : ArchiveType.values()) {
            if (value.predicate.test(fileName)) {
                return value;
            }
        }
        return null;
    }

    @Nonnull
    public static ArchiveType fromNameOrAlias(String s) {
        for (ArchiveType value : ArchiveType.values()) {
            if (value.name().equals(s)) {
                return value;
            }
            if (value.aliases.contains(s)) {
                return value;
            }
        }
        throw new IllegalArgumentException(String.format(
                "No enum constant or alias %s.%s",
                ArchiveType.class.getName(),
                s));
    }

    public String getFileExtension() {
        return aliases.iterator().next();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Constants {

        private static final Pattern ZIP = compile("\\.zip$", CASE_INSENSITIVE);
        private static final Pattern RAR = compile("\\.rar$", CASE_INSENSITIVE);
        private static final Pattern SEVEN_ZIP = compile("\\.7z$", CASE_INSENSITIVE);
        private static final Pattern TAR = compile("\\.tar$", CASE_INSENSITIVE);
        private static final Pattern TAR_7Z = compile("\\.tar\\.7z$", CASE_INSENSITIVE);
        private static final Pattern TAR_LZ4 = compile("\\.tar\\.lz4$", CASE_INSENSITIVE);
    }
}
