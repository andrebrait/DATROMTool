package io.github.datromtool.io.spec;

import io.github.datromtool.display.Displayable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public interface SourceSpec extends Displayable, Closeable {

    String getName();

    long getSize();

    FileTimes getFileTimes();

    InputStream getInputStream() throws IOException;
}
