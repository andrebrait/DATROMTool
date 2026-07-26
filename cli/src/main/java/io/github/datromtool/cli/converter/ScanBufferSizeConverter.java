package io.github.datromtool.cli.converter;

import io.github.datromtool.config.ScanBufferSize;

public final class ScanBufferSizeConverter extends AbstractBufferSizeConverter<ScanBufferSize> {

    @Override
    protected ScanBufferSize wrap(int bytes) {
        return new ScanBufferSize(bytes);
    }
}
