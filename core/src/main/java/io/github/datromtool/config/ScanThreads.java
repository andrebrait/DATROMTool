package io.github.datromtool.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Thread count dedicated to {@link AppConfig.FileScannerConfig}. Kept distinct from
 * {@link CopyThreads} so a scan value can never be passed where a copy value belongs.
 */
public record ScanThreads(int value) {

    public ScanThreads {
        if (value <= 0) {
            throw new IllegalArgumentException("Number of threads should be a positive number");
        }
    }

    @JsonValue
    public int value() {
        return value;
    }

    @JsonCreator
    public static ScanThreads of(int value) {
        return new ScanThreads(value);
    }
}
