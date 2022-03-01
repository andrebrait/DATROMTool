package io.github.datromtool.io.compression;

import java.io.IOException;
import java.io.InputStream;

public interface Decompressor {

    InputStream decompress(InputStream compressedInputStream) throws IOException;
}
