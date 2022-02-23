package io.github.datromtool.io.spec.compression;

import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.apache.commons.compress.compressors.lzma.LZMAUtils;

import java.io.IOException;
import java.io.OutputStream;

final class LZMACompressor implements Compressor {
    @Override
    public OutputStream compress(OutputStream backingOutputStream) throws IOException {
        if (LZMAUtils.isLZMACompressionAvailable()) {
            return new LZMACompressorOutputStream(backingOutputStream);
        }
        throw new UnsupportedCompressionAlgorithm(CompressionAlgorithm.LZMA);
    }
}
