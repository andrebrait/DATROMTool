package io.github.datromtool.io.compression;

import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZUtils;

import java.io.IOException;
import java.io.OutputStream;

final class XZCompressor implements Compressor {
    @Override
    public OutputStream compress(OutputStream backingOutputStream) throws IOException {
        if (XZUtils.isXZCompressionAvailable()) {
            return new XZCompressorOutputStream(backingOutputStream);
        }
        throw new UnsupportedCompressionAlgorithm(CompressionAlgorithm.XZ);
    }
}
