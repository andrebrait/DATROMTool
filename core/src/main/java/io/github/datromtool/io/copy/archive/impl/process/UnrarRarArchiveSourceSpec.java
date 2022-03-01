package io.github.datromtool.io.copy.archive.impl.process;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.io.copy.FileTimes;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UnrarRarArchiveSourceSpec extends ProcessArchiveSourceSpec {

    private static final Pattern RAR_LIST =
            Pattern.compile("^\\s*\\S+\\s+([0-9]+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S.+\\S)\\s*$");

    public UnrarRarArchiveSourceSpec(@Nonnull Path executablePath, @Nonnull Path path) {
        super(executablePath, path);
    }

    public UnrarRarArchiveSourceSpec(@Nonnull Path executablePath, @Nonnull Path path, @Nonnull Iterable<String> names) {
        super(executablePath, path, names);
    }

    @Override
    protected List<String> getListContentsArgs() {
        return ImmutableList.of(getExecutablePath().toString(), "l", getPath().toString());
    }

    @Override
    protected List<ProcessArchiveFile> convertToContents(List<String> lines) {
        return lines.stream()
                .map(RAR_LIST::matcher)
                .filter(Matcher::matches)
                .map(m -> new ProcessArchiveFile(
                        m.group(4),
                        Long.parseLong(m.group(1)),
                        FileTimes.from(
                                FileTime.from(parseForDefaultTimeZone(m).toInstant()),
                                null,
                                null)))
                .filter(f -> f.getSize() > 0)
                .collect(ImmutableList.toImmutableList());
    }

    @Nonnull
    private ZonedDateTime parseForDefaultTimeZone(Matcher m) {
        return LocalDateTime.parse(String.format("%sT%s:00", m.group(2), m.group(3))).atZone(ZoneId.systemDefault());
    }

    @Override
    protected List<String> getReadContentsArgs(List<ProcessArchiveFile> contents) {
        return ImmutableList.<String>builder()
                .add(getExecutablePath().toString())
                .add("p")
                .add("-inul")
                .add(getPath().toString())
                .addAll(contents.stream().map(ProcessArchiveFile::getName).iterator())
                .build();
    }
}
