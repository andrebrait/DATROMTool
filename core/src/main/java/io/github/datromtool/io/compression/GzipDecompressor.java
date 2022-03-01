package io.github.datromtool.io.compression;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.IOException;
import java.io.InputStream;

final class GzipDecompressor implements Decompressor {
    @Override
    public InputStream decompress(InputStream compressedInputStream) throws IOException {
        return new GzipCompressorInputStream(compressedInputStream);
    }
}
