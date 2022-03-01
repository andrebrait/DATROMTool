package io.github.datromtool.io.copy.archive.impl;

import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrapper for OutputStream which prevents it from being closed
 */
@RequiredArgsConstructor
final class NonCloseableOutputStream extends OutputStream {

    private final OutputStream delegate;

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(@Nonnull byte[] b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(@Nonnull byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }
}
