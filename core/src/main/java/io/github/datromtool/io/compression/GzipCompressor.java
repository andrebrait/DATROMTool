package io.github.datromtool.io.compression;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.IOException;
import java.io.OutputStream;

final class GzipCompressor implements Compressor {
    @Override
    public OutputStream compress(OutputStream backingOutputStream) throws IOException {
        return new GzipCompressorOutputStream(backingOutputStream);
    }
}
