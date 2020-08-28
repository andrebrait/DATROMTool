package io.github.datromtool.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
public class UnrarArchiveEntry {
    String name;
    Long size;
    LocalDateTime modificationTime;
}
