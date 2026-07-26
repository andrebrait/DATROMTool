package io.github.datromtool.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Default dynamic I/O buffer size (bytes) for {@link AppConfig.FileScannerConfig}. Kept
 * distinct from {@link ScanMaxBufferSize} and {@link CopyBufferSize} so buffer values from
 * different subsystems cannot be cross-assigned.
 */
public record ScanBufferSize(int bytes) {

    public ScanBufferSize {
        if (bytes <= 0) {
            throw new IllegalArgumentException("Buffer size should be a positive number of bytes");
        }
    }

    @JsonValue
    public int bytes() {
        return bytes;
    }

    @JsonCreator
    public static ScanBufferSize of(int bytes) {
        return new ScanBufferSize(bytes);
    }
}
