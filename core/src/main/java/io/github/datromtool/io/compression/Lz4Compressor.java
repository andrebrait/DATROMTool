package io.github.datromtool.io.compression;

import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;

import java.io.IOException;
import java.io.OutputStream;

final class Lz4Compressor implements Compressor {
    @Override
    public OutputStream compress(OutputStream backingOutputStream) throws IOException, UnsupportedCompressionAlgorithm {
        return new FramedLZ4CompressorOutputStream(backingOutputStream);
    }
}
