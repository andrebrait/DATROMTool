package io.github.datromtool.io.spec.compression;

import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMAUtils;

import java.io.IOException;
import java.io.InputStream;

final class LZMADecompressor implements Decompressor {
    @Override
    public InputStream decompress(InputStream compressedInputStream) throws IOException {
        if (LZMAUtils.isLZMACompressionAvailable()) {
            return new LZMACompressorInputStream(compressedInputStream);
        }
        throw new UnsupportedCompressionAlgorithm(CompressionAlgorithm.LZMA);
    }
}
