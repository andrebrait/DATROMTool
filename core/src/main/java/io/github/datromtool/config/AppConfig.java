package io.github.datromtool.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@With
@Value
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
public class AppConfig {

    @With
    @Value
    @Jacksonized
    @Builder(toBuilder = true)
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PRIVATE, force = true)
    @JsonInclude(NON_DEFAULT)
    public static class FileScannerConfig {

        @Builder.Default
        @NonNull
        Integer defaultBufferSize = 32 * 1024; // 32KB

        @Builder.Default
        @NonNull
        Integer maxBufferSize = 256 * 1024 * 1024; // 256MB

        @Builder.Default
        @NonNull
        Integer threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    }

    @With
    @Value
    @Jacksonized
    @Builder(toBuilder = true)
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PRIVATE, force = true)
    @JsonInclude(NON_DEFAULT)
    public static class FileCopierConfig {

        @Builder.Default
        @NonNull
        Integer bufferSize = 32 * 1024; // 32KB

        @Builder.Default
        @NonNull
        Integer threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

        @Builder.Default
        boolean allowRawZipCopy = false;
    }

    @NonNull
    @Builder.Default
    AppConfig.FileScannerConfig scanner = FileScannerConfig.builder().build();

    @NonNull
    @Builder.Default
    AppConfig.FileCopierConfig copier = FileCopierConfig.builder().build();

}
