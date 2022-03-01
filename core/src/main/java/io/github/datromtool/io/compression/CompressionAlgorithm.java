package io.github.datromtool.io.compression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.compress.compressors.lzma.LZMAUtils;
import org.apache.commons.compress.compressors.xz.XZUtils;

@AllArgsConstructor
public enum CompressionAlgorithm {
    BZIP2("bzip2", "bz2", true, new BZip2Compressor(), new BZip2Decompressor()),
    GZIP("gzip", "gz", true, new GzipCompressor(), new GzipDecompressor()),
    LZ4("lz4", "lz4", true, new Lz4Compressor(), new Lz4Decompressor()),
    LZMA("LZMA", "lzma", LZMAUtils.isLZMACompressionAvailable(), new LZMACompressor(), new LZMADecompressor()),
    XZ("xz", "xz", XZUtils.isXZCompressionAvailable(), new XZCompressor(), new XZDecompressor());

    @Getter
    private final String label;

    @Getter
    private final String extension;

    @Getter
    private final boolean enabled;

    @Getter
    private final Compressor compressor;

    @Getter
    private final Decompressor decompressor;
}
