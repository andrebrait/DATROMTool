package io.github.datromtool.io.copy;

import io.github.datromtool.display.Displayable;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public interface DestinationSpec extends Displayable, Closeable {

    String getName();

    OutputStream getOutputStream() throws IOException;
}
