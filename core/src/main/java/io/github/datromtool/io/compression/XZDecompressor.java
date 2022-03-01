package io.github.datromtool.io.compression;

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZUtils;

import java.io.IOException;
import java.io.InputStream;

final class XZDecompressor implements Decompressor {
    @Override
    public InputStream decompress(InputStream compressedInputStream) throws IOException {
        if (XZUtils.isXZCompressionAvailable()) {
            return new XZCompressorInputStream(compressedInputStream);
        }
        throw new UnsupportedCompressionAlgorithm(CompressionAlgorithm.XZ);
    }
}
