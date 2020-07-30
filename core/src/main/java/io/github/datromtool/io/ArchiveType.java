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

    // TODO: support plain GZ, BZ2, LZ4, LZMA, XZ and ZSTD

    NONE(s -> false),
    ZIP(s -> Constants.ZIP.matcher(s).find()),
    RAR(s -> Constants.RAR.matcher(s).find()),
    SEVEN_ZIP(s -> !Constants.TAR_7Z.matcher(s).find()
            && Constants.SEVEN_ZIP.matcher(s).find()),
    TAR(s -> Constants.TAR.matcher(s).find()),
    TAR_BZ2(s -> BZip2Utils.isCompressedFilename(s)
            && Constants.COMPRESSED_TAR.matcher(s).find()),
    TAR_GZ(s -> !s.endsWith(".Z")
            && GzipUtils.isCompressedFilename(s)
            && Constants.COMPRESSED_TAR.matcher(s).find()),
    TAR_LZ4(s -> Constants.TAR_LZ4.matcher(s).find()),
    TAR_LZMA(s -> LZMAUtils.isLZMACompressionAvailable()
            && LZMAUtils.isCompressedFilename(s)
            && Constants.COMPRESSED_TAR.matcher(s).find()),
    TAR_XZ(s -> XZUtils.isXZCompressionAvailable()
            && XZUtils.isCompressedFilename(s)
            && Constants.COMPRESSED_TAR.matcher(s).find()),
    TAR_ZSTD(s -> Constants.TAR_ZSTD.matcher(s).find());


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

        // TODO: split this for every type of tar because we also have short tar names!
        // TODO: support tgz, tbz2, tz, etc.
        private static final Pattern COMPRESSED_TAR = compile("\\.tar\\.[^.]+$", CASE_INSENSITIVE);
        private static final Pattern TAR_7Z = compile("\\.tar\\.7z$", CASE_INSENSITIVE);
        private static final Pattern ZIP = compile("\\.zip$", CASE_INSENSITIVE);
        private static final Pattern RAR = compile("\\.rar$", CASE_INSENSITIVE);
        private static final Pattern SEVEN_ZIP = compile("^\\.7z$", CASE_INSENSITIVE);
        private static final Pattern TAR = compile("\\.tar$", CASE_INSENSITIVE);
        private static final Pattern TAR_LZ4 = compile("\\.tar\\.lz4$", CASE_INSENSITIVE);
        private static final Pattern TAR_ZSTD = compile("\\.tar\\.zst$|\\.tzst$", CASE_INSENSITIVE);
    }
}
