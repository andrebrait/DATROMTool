package io.github.datromtool.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * I/O buffer size (bytes) for {@link AppConfig.FileCopierConfig}. Kept distinct from
 * {@link ScanBufferSize} and {@link ScanMaxBufferSize} so buffer values from different
 * subsystems cannot be cross-assigned.
 */
public record CopyBufferSize(int bytes) {

    public CopyBufferSize {
        if (bytes <= 0) {
            throw new IllegalArgumentException("Buffer size should be a positive number of bytes");
        }
    }

    @JsonValue
    public int bytes() {
        return bytes;
    }

    @JsonCreator
    public static CopyBufferSize of(int bytes) {
        return new CopyBufferSize(bytes);
    }
}
