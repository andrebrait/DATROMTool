package io.github.datromtool.io;

import java.time.LocalDateTime;

public record UnrarArchiveEntry(String name, Long size, LocalDateTime modificationTime) {
}
