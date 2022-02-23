package io.github.datromtool.io.spec;

import io.github.datromtool.io.Addressable;
import io.github.datromtool.io.ArchiveType;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;

public interface ArchiveSourceSpec extends Addressable, Closeable {

    ArchiveType getType();

    @Nullable
    ArchiveSourceInternalSpec getNextInternalSpec() throws IOException;
}
