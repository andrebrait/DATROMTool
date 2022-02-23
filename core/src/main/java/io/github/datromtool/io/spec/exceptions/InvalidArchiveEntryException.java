package io.github.datromtool.io.spec.exceptions;

import java.io.IOException;
import java.nio.file.Path;

import static java.lang.String.format;

public final class InvalidArchiveEntryException extends IOException {

    public InvalidArchiveEntryException(Path path, String name) {
        super(format("Entry '%s' in '%s' is not a file", name, path));
    }
}
