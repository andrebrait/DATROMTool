package io.github.datromtool.io.copy.archive;

import io.github.datromtool.io.Addressable;
import io.github.datromtool.io.copy.SourceSpec;

import java.io.Closeable;
import java.io.IOException;

public interface ArchiveDestinationSpec extends Addressable, Closeable {

    ArchiveDestinationInternalSpec createInternalDestinationSpecFor(String name, SourceSpec sourceSpec) throws IOException;
}
