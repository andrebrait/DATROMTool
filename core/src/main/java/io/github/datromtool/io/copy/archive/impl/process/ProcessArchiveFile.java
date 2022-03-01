package io.github.datromtool.io.copy.archive.impl.process;

import io.github.datromtool.io.copy.FileTimes;
import lombok.Value;

@Value
class ProcessArchiveFile {
    String name;
    long size;
    FileTimes fileTimes;
}
