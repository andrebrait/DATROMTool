package io.github.datromtool.io.compression;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import java.io.IOException;
import java.io.OutputStream;

final class BZip2Compressor implements Compressor {
    @Override
    public OutputStream compress(OutputStream backingOutputStream) throws IOException {
        return new BZip2CompressorOutputStream(backingOutputStream);
    }
}
