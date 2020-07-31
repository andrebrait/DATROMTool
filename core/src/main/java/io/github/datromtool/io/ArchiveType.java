package io.github.datromtool.io;

import org.apache.commons.compress.compressors.bzip2.BZip2Utils;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.commons.compress.compressors.lzma.LZMAUtils;
import org.apache.commons.compress.compressors.xz.XZUtils;

import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

public enum ArchiveType {

    NONE(s -> false),
    // Archive formats
    ZIP(s -> Constants.ZIP.matcher(s).find()),
    RAR(s -> Constants.RAR.matcher(s).find()),
    SEVEN_ZIP(s -> Constants.SEVEN_ZIP.matcher(s).find()
            && !Constants.TAR_7Z.matcher(s).find()),
    TAR(s -> Constants.TAR.matcher(s).find()),

    // Compressed archive formats
    TAR_BZ2(s -> BZip2Utils.isCompressedFilename(s)
            && Constants.TAR.matcher(BZip2Utils.getUncompressedFilename(s)).find()),
    TAR_GZ(s -> GzipUtils.isCompressedFilename(s)
            && Constants.TAR.matcher(GzipUtils.getUncompressedFilename(s)).find()),
    TAR_LZ4(s -> Constants.TAR_LZ4.matcher(s).find()),
    TAR_LZMA(s -> LZMAUtils.isLZMACompressionAvailable()
            && LZMAUtils.isCompressedFilename(s)
            && Constants.TAR.matcher(LZMAUtils.getUncompressedFilename(s)).find()),
    TAR_XZ(s -> XZUtils.isXZCompressionAvailable()
            && XZUtils.isCompressedFilename(s)
            && Constants.TAR.matcher(XZUtils.getUncompressedFilename(s)).find());

    private final Predicate<String> predicate;

    ArchiveType(Predicate<String> predicate) {
        this.predicate = predicate;
    }

    public static ArchiveType parse(Path file) {
        String fileName = file.getFileName().toString();
        return parse(fileName);
    }

    public static ArchiveType parse(String fileName) {
        for (ArchiveType value : ArchiveType.values()) {
            if (value.predicate.test(fileName)) {
                return value;
            }
        }
        return NONE;
    }

    private static class Constants {

        private static final Pattern ZIP = compile("\\.zip$", CASE_INSENSITIVE);
        private static final Pattern RAR = compile("\\.rar$", CASE_INSENSITIVE);
        private static final Pattern SEVEN_ZIP = compile("^\\.7z$", CASE_INSENSITIVE);
        private static final Pattern TAR = compile("\\.tar$", CASE_INSENSITIVE);
        private static final Pattern TAR_7Z = compile("\\.tar\\.7z$", CASE_INSENSITIVE);
        private static final Pattern TAR_LZ4 = compile("\\.tar\\.lz4$", CASE_INSENSITIVE);
    }
}
