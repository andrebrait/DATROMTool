package io.github.datromtool.io;

import com.fasterxml.jackson.annotation.JsonValue;
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
import static java.util.regex.Pattern.quote;

@AllArgsConstructor
public enum ArchiveType {

    ZIP(Constants.ZIP, true, ArchiveType::isZip),
    RAR(Constants.RAR, false, ArchiveType::isRar),
    SEVEN_ZIP(Constants.SEVEN_ZIP, true, ArchiveType::isSevenZip),
    TAR(Constants.TAR, true, ArchiveType::isTar),
    TAR_BZ2(Constants.TAR_BZ2, true, ArchiveType::isBzip2),
    TAR_GZ(Constants.TAR_GZ, true, ArchiveType::isGzip),
    TAR_LZ4(Constants.TAR_LZ4, true, ArchiveType::isLz4),
    TAR_LZMA(Constants.TAR_LZMA, LZMAUtils.isLZMACompressionAvailable(), ArchiveType::isTarLzma),
    TAR_XZ(Constants.TAR_XZ, XZUtils.isXZCompressionAvailable(), ArchiveType::isTarXz);

    private static boolean isZip(String s) {
        return Constants.ZIP_PATTERN.matcher(s).find();
    }

    private static boolean isRar(String s) {
        return Constants.RAR_PATTERN.matcher(s).find();
    }

    private static boolean isSevenZip(String s) {
        return Constants.SEVEN_ZIP_PATTERN.matcher(s).find()
                && !Constants.TAR_7Z_PATTERN.matcher(s).find();
    }

    private static boolean isTar(String s) {
        return Constants.TAR_PATTERN.matcher(s).find();
    }

    private static boolean isBzip2(String s) {
        return BZip2Utils.isCompressedFileName(s)
                && Constants.TAR_PATTERN.matcher(BZip2Utils.getUncompressedFileName(s)).find();
    }

    private static boolean isGzip(String s) {
        return GzipUtils.isCompressedFileName(s)
                && Constants.TAR_PATTERN.matcher(GzipUtils.getUncompressedFileName(s)).find();
    }

    private static boolean isLz4(String s) {
        return Constants.TAR_LZ4_PATTERN.matcher(s).find();
    }

    private static boolean isTarLzma(String s) {
        return LZMAUtils.isLZMACompressionAvailable()
                && LZMAUtils.isCompressedFileName(s)
                && Constants.TAR_PATTERN.matcher(LZMAUtils.getUncompressedFileName(s)).find();
    }

    private static boolean isTarXz(String s) {
        return XZUtils.isXZCompressionAvailable()
                && XZUtils.isCompressedFileName(s)
                && Constants.TAR_PATTERN.matcher(XZUtils.getUncompressedFileName(s)).find();
    }

    @Getter(onMethod_ = {@JsonValue})
    private final String alias;
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
        for (ArchiveType value : values()) {
            if (value.predicate.test(fileName)) {
                return value;
            }
        }
        return null;
    }

    @Nullable
    public static ArchiveType fromAlias(@Nonnull String alias) {
        for (ArchiveType value : values()) {
            if (value.getAlias().equals(alias)) {
                return value;
            }
        }
        return null;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Constants {

        private static final String ZIP = "zip";
        private static final String RAR = "rar";
        private static final String SEVEN_ZIP = "7z";
        private static final String TAR = "tar";
        private static final String TAR_SEVEN_ZIP = "tar.7z";
        private static final String TAR_BZ2 = "tar.bz2";
        private static final String TAR_GZ = "tar.gz";
        private static final String TAR_LZ4 = "tar.lz4";
        private static final String TAR_LZMA = "tar.lzma";
        private static final String TAR_XZ = "tar.xz";

        private static Pattern toPattern(String alias) {
            return compile("\\." + quote(alias) + "$", CASE_INSENSITIVE);
        }

        private static final Pattern ZIP_PATTERN = toPattern(ZIP);
        private static final Pattern RAR_PATTERN = toPattern(RAR);
        private static final Pattern SEVEN_ZIP_PATTERN = toPattern(SEVEN_ZIP);
        private static final Pattern TAR_PATTERN = toPattern(TAR);
        private static final Pattern TAR_7Z_PATTERN = toPattern(TAR_SEVEN_ZIP);
        private static final Pattern TAR_LZ4_PATTERN = toPattern(TAR_LZ4);
    }
}
