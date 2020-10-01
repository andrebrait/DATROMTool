package io.github.datromtool.io;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;

@AllArgsConstructor
public enum ArchiveType {

    @JsonProperty(Constants.ZIP)
    ZIP(Constants.ZIP, true, s -> Constants.ZIP_PATTERN.matcher(s).find()),
    @JsonProperty(Constants.RAR)
    RAR(Constants.RAR, false, s -> Constants.RAR_PATTERN.matcher(s).find()),
    @JsonProperty(Constants.SEVEN_ZIP)
    SEVEN_ZIP(Constants.SEVEN_ZIP, true, ArchiveType::isSevenZip),
    @JsonProperty(Constants.TAR)
    TAR(Constants.TAR, true, s -> Constants.TAR_PATTERN.matcher(s).find()),
    @JsonProperty(Constants.TAR_BZ2)
    TAR_BZ2(Constants.TAR_BZ2, true, ArchiveType::isBzip2),
    @JsonProperty(Constants.TAR_GZ)
    TAR_GZ(Constants.TAR_GZ, true, ArchiveType::isGzip),
    @JsonProperty(Constants.TAR_LZ4)
    TAR_LZ4(Constants.TAR_LZ4, true, s -> Constants.TAR_LZ4_PATTERN.matcher(s).find()),
    @JsonProperty(Constants.TAR_LZMA)
    TAR_LZMA(Constants.TAR_LZMA, LZMAUtils.isLZMACompressionAvailable(), ArchiveType::isTarLzma),
    @JsonProperty(Constants.TAR_XZ)
    TAR_XZ(Constants.TAR_XZ, XZUtils.isXZCompressionAvailable(), ArchiveType::isTarXz);

    private static boolean isSevenZip(String s) {
        return Constants.SEVEN_ZIP_PATTERN.matcher(s).find()
                && !Constants.TAR_7Z_PATTERN.matcher(s).find();
    }

    private static boolean isBzip2(String s) {
        return BZip2Utils.isCompressedFilename(s)
                && Constants.TAR_PATTERN.matcher(BZip2Utils.getUncompressedFilename(s)).find();
    }

    private static boolean isGzip(String s) {
        return GzipUtils.isCompressedFilename(s)
                && Constants.TAR_PATTERN.matcher(GzipUtils.getUncompressedFilename(s)).find();
    }

    private static boolean isTarLzma(String s) {
        return LZMAUtils.isLZMACompressionAvailable()
                && LZMAUtils.isCompressedFilename(s)
                && Constants.TAR_PATTERN.matcher(LZMAUtils.getUncompressedFilename(s)).find();
    }

    private static boolean isTarXz(String s) {
        return XZUtils.isXZCompressionAvailable()
                && XZUtils.isCompressedFilename(s)
                && Constants.TAR_PATTERN.matcher(XZUtils.getUncompressedFilename(s)).find();
    }

    @Getter
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
        for (ArchiveType value : ArchiveType.values()) {
            if (value.predicate.test(fileName)) {
                return value;
            }
        }
        return null;
    }

    @Nullable
    public static ArchiveType fromAlias(@Nonnull String alias) {
        return Arrays.stream(values())
                .filter(v -> v.getAlias().equals(alias))
                .findFirst()
                .orElse(null);
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
