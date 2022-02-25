package io.github.datromtool.io.spec.implementations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.io.ArchiveType;
import io.github.datromtool.io.spec.ArchiveSourceInternalSpec;
import io.github.datromtool.io.spec.ArchiveSourceSpec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SevenZipArchiveSourceSpec implements ArchiveSourceSpec {

    @NonNull
    @Getter
    private final Path path;
    @NonNull
    private final ImmutableSet<String> names;

    // Stateful part
    private transient SevenZFile sevenZFile;
    private transient Set<String> mutableNames;

    @Nonnull
    public static SevenZipArchiveSourceSpec from(@Nonnull Path path) {
        return from(path, ImmutableList.of());
    }

    @Nonnull
    public static SevenZipArchiveSourceSpec from(@Nonnull Path path, @Nonnull Iterable<String> names) {
        return new SevenZipArchiveSourceSpec(path.toAbsolutePath().normalize(), ImmutableSet.copyOf(names));
    }

    @Override
    public ArchiveType getType() {
        return ArchiveType.SEVEN_ZIP;
    }

    @Nullable
    @Override
    public ArchiveSourceInternalSpec getNextInternalSpec() throws IOException {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
