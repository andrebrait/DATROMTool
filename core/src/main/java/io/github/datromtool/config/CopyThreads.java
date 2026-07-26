package io.github.datromtool.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Thread count dedicated to {@link AppConfig.FileCopierConfig}. Kept distinct from
 * {@link ScanThreads} so a copy value can never be passed where a scan value belongs.
 */
public record CopyThreads(int value) {

    public CopyThreads {
        if (value <= 0) {
            throw new IllegalArgumentException("Number of threads should be a positive number");
        }
    }

    @JsonValue
    public int value() {
        return value;
    }

    @JsonCreator
    public static CopyThreads of(int value) {
        return new CopyThreads(value);
    }
}
