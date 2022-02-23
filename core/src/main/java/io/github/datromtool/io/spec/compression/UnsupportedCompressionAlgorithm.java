package io.github.datromtool.io.spec.compression;

import java.io.IOException;

import static java.lang.String.format;

public final class UnsupportedCompressionAlgorithm extends IOException {

    public UnsupportedCompressionAlgorithm(CompressionAlgorithm algorithm) {
        super(format("Compression algorithm '%s' is not supported", algorithm.getLabel()));
    }
}
