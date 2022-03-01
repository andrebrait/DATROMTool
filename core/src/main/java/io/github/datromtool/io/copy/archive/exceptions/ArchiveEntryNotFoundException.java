package io.github.datromtool.io.copy.archive.exceptions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import static java.lang.String.format;

public final class ArchiveEntryNotFoundException extends IOException {

    public ArchiveEntryNotFoundException(Path path, String name) {
        super(format("Could not find an entry with name '%s' in '%s'", name, path));
    }

    public ArchiveEntryNotFoundException(Path path, Collection<String> name) {
        super(format("Could not find an entries with name '[%s]' in '%s'", String.join(",", name), path));
    }
}
