package io.github.datromtool.io.spec.exceptions;

import java.io.IOException;
import java.nio.file.Path;

import static java.lang.String.format;

public final class ArchiveEntryNotFoundException extends IOException {

    public ArchiveEntryNotFoundException(Path path, String name) {
        super(format("Could not find an entry with name '%s' in '%s'", name, path));
    }
}
