package io.github.datromtool.io.spec;

import lombok.Getter;

import javax.annotation.Nonnull;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public abstract class AbstractArchiveDestinationSpec implements ArchiveDestinationSpec {

    @Getter
    private final Path path;

    protected AbstractArchiveDestinationSpec(@Nonnull Path path) {
        this.path = requireNonNull(path, "'path' must not be null").toAbsolutePath().normalize();
    }
}
