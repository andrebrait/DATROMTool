package io.github.datromtool.io.copy.archive.impl.process;

import io.github.datromtool.io.copy.FileTimes;

record ProcessArchiveFile(String name, long size, FileTimes fileTimes) {
}
