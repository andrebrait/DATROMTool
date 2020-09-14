package io.github.datromtool.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@Value
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
public class AppConfig {

    @Value
    @Jacksonized
    @Builder(toBuilder = true)
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PRIVATE, force = true)
    @JsonInclude(NON_DEFAULT)
    public static class FileScanner {

        @Builder.Default
        @NonNull
        Integer maxBufferSize = 256 * 1024 * 1024; // 256MB

        @Builder.Default
        @NonNull
        Integer threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

    }

    @Value
    @Jacksonized
    @Builder(toBuilder = true)
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PRIVATE, force = true)
    @JsonInclude(NON_DEFAULT)
    public static class FileCopier {

        @Builder.Default
        @NonNull
        Integer threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

    }

    @NonNull
    @Builder.Default
    FileScanner scanner = FileScanner.builder().build();

    @NonNull
    @Builder.Default
    FileCopier copier = FileCopier.builder().build();

}
