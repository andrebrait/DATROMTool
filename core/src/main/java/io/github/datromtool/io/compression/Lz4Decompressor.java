package io.github.datromtool.io.compression;

import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;

import java.io.IOException;
import java.io.InputStream;

final class Lz4Decompressor implements Decompressor {
    @Override
    public InputStream decompress(InputStream compressedInputStream) throws IOException {
        return new FramedLZ4CompressorInputStream(compressedInputStream);
    }
}
