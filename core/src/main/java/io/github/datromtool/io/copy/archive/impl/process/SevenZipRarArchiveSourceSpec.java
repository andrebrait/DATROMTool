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

public final class SevenZipRarArchiveSourceSpec extends ProcessArchiveSourceSpec {

    private static final Pattern SEVEN_ZIP_LIST =
            Pattern.compile("^\\s*(\\S+)\\s+(\\S+)\\s+\\S+\\s+([0-9]+)\\s+[0-9]+\\s+(\\S.+\\S)\\s*$");

    public SevenZipRarArchiveSourceSpec(@Nonnull Path executablePath, @Nonnull Path path) {
        super(executablePath, path);
    }

    public SevenZipRarArchiveSourceSpec(@Nonnull Path executablePath, @Nonnull Path path, @Nonnull Iterable<String> names) {
        super(executablePath, path, names);
    }

    @Override
    protected List<String> getListContentsArgs() {
        return ImmutableList.of(getExecutablePath().toString(), "l", "-ba", getPath().toString());
    }

    @Override
    protected List<ProcessArchiveFile> convertToContents(List<String> lines) {
        return lines.stream()
                .map(SEVEN_ZIP_LIST::matcher)
                .filter(Matcher::matches)
                .map(m -> new ProcessArchiveFile(
                        m.group(4),
                        Long.parseLong(m.group(3)),
                        FileTimes.from(
                                FileTime.from(parseForDefaultTimeZone(m).toInstant()),
                                null,
                                null)))
                .filter(f -> f.getSize() > 0)
                .collect(ImmutableList.toImmutableList());
    }

    @Nonnull
    private ZonedDateTime parseForDefaultTimeZone(Matcher m) {
        return LocalDateTime.parse(String.format("%sT%s", m.group(1), m.group(2))).atZone(ZoneId.systemDefault());
    }

    @Override
    protected List<String> getReadContentsArgs(List<ProcessArchiveFile> contents) {
        return ImmutableList.<String>builder()
                .add(getExecutablePath().toString())
                .add("e")
                .add("-so")
                .add("-bd")
                .add("-ba")
                .add(getPath().toString())
                .addAll(contents.stream().map(ProcessArchiveFile::getName).iterator())
                .build();
    }
}
