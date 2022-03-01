package io.github.datromtool.io.copy.archive.impl.process;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.io.copy.archive.ArchiveSourceInternalSpec;
import io.github.datromtool.io.copy.archive.ArchiveSourceSpec;
import io.github.datromtool.io.copy.archive.exceptions.ArchiveEntryNotFoundException;
import io.github.datromtool.util.ArchiveUtils;
import lombok.AccessLevel;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public abstract class ProcessArchiveSourceSpec implements ArchiveSourceSpec {

    @Getter(AccessLevel.PROTECTED)
    private final Path executablePath;
    @Getter
    private final Path path;
    @Getter(AccessLevel.PROTECTED)
    private final ImmutableSet<String> names;

    // Stateful part
    private transient ImmutableList<ProcessArchiveFile> contents;
    private transient Iterator<ProcessArchiveFile> contentsIterator;
    private transient List<String> args;
    private transient Process process;
    private transient InputStream processInputStream;

    public ProcessArchiveSourceSpec(@Nonnull Path executablePath, @Nonnull Path path) {
        this(executablePath, path, ImmutableSet.of());
    }

    public ProcessArchiveSourceSpec(@Nonnull Path executablePath, @Nonnull Path path, @Nonnull Iterable<String> names) {
        this.executablePath = requireNonNull(executablePath, "'executablePath' must not be null").toAbsolutePath().normalize();
        this.path = requireNonNull(path, "'path' must not be null").toAbsolutePath().normalize();
        this.names = ImmutableSet.copyOf(requireNonNull(names, "'names' must not be null"));
    }

    protected abstract List<String> getListContentsArgs();

    /**
     * Converts the output of the command into instances of {@link ProcessArchiveFile}.
     */
    protected abstract List<ProcessArchiveFile> convertToContents(List<String> lines);

    protected abstract List<String> getReadContentsArgs(List<ProcessArchiveFile> contents);

    @Nullable
    @Override
    public final ArchiveSourceInternalSpec getNextInternalSpec() throws IOException {
        if (contents == null) {
            List<String> args = getListContentsArgs();
            ProcessBuilder pb = new ProcessBuilder(args);
            Process process = pb.start();
            List<ProcessArchiveFile> archiveContents = convertToContents(readStdout(process));
            waitProcess(args, process);
            if (names.isEmpty()) {
                contents = ImmutableList.copyOf(archiveContents);
            } else {
                contents = filterContents(archiveContents);
            }
        }
        if (contentsIterator == null) {
            contentsIterator = contents.iterator();
            args = ImmutableList.copyOf(getReadContentsArgs(contents));
            ProcessBuilder pb = new ProcessBuilder(args);
            process = pb.start();
            processInputStream = process.getInputStream();
        }
        if (contentsIterator.hasNext()) {
            return new ProcessArchiveSourceInternalSpec(this, processInputStream, contentsIterator.next());
        }
        return null;
    }

    private ImmutableList<ProcessArchiveFile> filterContents(List<ProcessArchiveFile> archiveContents) throws ArchiveEntryNotFoundException {
        ImmutableList.Builder<ProcessArchiveFile> result = ImmutableList.builder();
        HashSet<String> mutableNames = new LinkedHashSet<>(names);
        for (ProcessArchiveFile file : archiveContents) {
            if (mutableNames.remove(ArchiveUtils.normalizePath(file.getName()))) {
                result.add(file);
            }
        }
        if (!mutableNames.isEmpty()) {
            throw new ArchiveEntryNotFoundException(path, mutableNames);
        }
        return result.build();
    }

    private static void waitProcess(List<String> args, Process process) throws IOException {
        try {
            int statusCode = process.waitFor();
            if (statusCode != 0) {
                throw new IOException(format("Process [%s] exited with status code %d", String.join(",", args), statusCode));
            }
        } catch (InterruptedException e) {
            throw new IOException(format("Process [%s] interrupted", String.join(",", args)), e);
        }
    }

    @Override
    public final void close() throws IOException {
        List<String> currentArgs = args;
        Process currentProcess = process;
        contents = null;
        contentsIterator = null;
        args = null;
        process = null;
        if (processInputStream != null) {
            processInputStream.close();
            processInputStream = null;
        }
        if (currentProcess != null) {
            waitProcess(currentArgs, currentProcess);
        }
    }

    private static ImmutableList<String> readStdout(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        String currLine;
        while ((currLine = reader.readLine()) != null) {
            builder.add(currLine);
        }
        return builder.build();
    }

}
