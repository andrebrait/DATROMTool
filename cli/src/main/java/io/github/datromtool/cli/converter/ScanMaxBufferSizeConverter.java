package io.github.datromtool.cli.converter;

import io.github.datromtool.config.ScanMaxBufferSize;

public final class ScanMaxBufferSizeConverter extends AbstractBufferSizeConverter<ScanMaxBufferSize> {

    @Override
    protected ScanMaxBufferSize wrap(int bytes) {
        return new ScanMaxBufferSize(bytes);
    }
}
