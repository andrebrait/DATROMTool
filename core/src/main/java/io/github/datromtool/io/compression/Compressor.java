package io.github.datromtool.io.compression;

import java.io.IOException;
import java.io.OutputStream;

public interface Compressor {

    OutputStream compress(OutputStream backingOutputStream) throws IOException;
}
