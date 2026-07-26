package io.github.datromtool.cli.converter;

import io.github.datromtool.config.CopyBufferSize;

public final class CopyBufferSizeConverter extends AbstractBufferSizeConverter<CopyBufferSize> {

    @Override
    protected CopyBufferSize wrap(int bytes) {
        return new CopyBufferSize(bytes);
    }
}
