package io.github.datromtool.io.spec;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.io.spec.exceptions.ArchiveEntryNotFoundException;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public abstract class AbstractArchiveSourceSpec implements ArchiveSourceSpec {

    @Getter
    private final Path path;
    private final Set<String> names;

    // Stateful part
    private transient Set<String> mutableNames;

    public AbstractArchiveSourceSpec(@Nonnull Path path) {
        this(path, ImmutableSet.of());
    }

    public AbstractArchiveSourceSpec(@Nonnull Path path, @Nonnull Iterable<String> names) {
        this.path = requireNonNull(path, "'path' must not be null").toAbsolutePath().normalize();
        this.names = ImmutableSet.copyOf(requireNonNull(names, "'names' must not be null"));
    }

    protected abstract void initArchive() throws IOException;

    @Nullable
    protected abstract ArchiveSourceInternalSpec getNextEntry() throws IOException;

    protected abstract void closeArchive() throws IOException;

    @Nullable
    @Override
    public final ArchiveSourceInternalSpec getNextInternalSpec() throws IOException {
        initArchive();
        if (names.isEmpty()) {
            return getNextEntry();
        } else {
            if (mutableNames == null) {
                mutableNames = new HashSet<>(names);
            }
            ArchiveSourceInternalSpec entry;
            while ((entry = getNextEntry()) != null) {
                if (mutableNames.remove(entry.getName())) {
                    return entry;
                }
            }
            if (!mutableNames.isEmpty()) {
                throw new ArchiveEntryNotFoundException(path, mutableNames);
            }
            return null;
        }
    }

    @Override
    public final void close() throws IOException {
        mutableNames = null;
        closeArchive();
    }
}


