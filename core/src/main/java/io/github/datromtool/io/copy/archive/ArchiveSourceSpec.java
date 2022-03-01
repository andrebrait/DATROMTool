package io.github.datromtool.io.copy.archive;

import io.github.datromtool.io.Addressable;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;

public interface ArchiveSourceSpec extends Addressable, Closeable {

    @Nullable
    ArchiveSourceInternalSpec getNextInternalSpec() throws IOException;
}
