package io.github.datromtool.io.compression;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.IOException;
import java.io.InputStream;

final class BZip2Decompressor implements Decompressor {
    @Override
    public InputStream decompress(InputStream compressedInputStream) throws IOException {
        return new BZip2CompressorInputStream(compressedInputStream);
    }
}
