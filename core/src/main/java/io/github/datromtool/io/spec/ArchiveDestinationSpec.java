package io.github.datromtool.io.spec;

import io.github.datromtool.io.Addressable;

import java.io.Closeable;
import java.io.IOException;

public interface ArchiveDestinationSpec extends Addressable, Closeable {

    ArchiveDestinationInternalSpec createInternalDestinationSpecFor(String name, SourceSpec sourceSpec) throws IOException;
}
