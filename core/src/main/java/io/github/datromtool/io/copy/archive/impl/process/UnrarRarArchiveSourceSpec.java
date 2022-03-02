package io.github.datromtool.io.copy.archive.impl.process;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Matcher;

public final class UnrarRarArchiveSourceSpec extends ProcessArchiveSourceSpec {

    public UnrarRarArchiveSourceSpec(@Nonnull Path executablePath, @Nonnull Path path) {
        super(executablePath, path);
    }

    public UnrarRarArchiveSourceSpec(@Nonnull Path executablePath, @Nonnull Path path, @Nonnull Iterable<String> names) {
        super(executablePath, path, names);
    }

    @Override
    protected List<String> getListContentsArgs() {
        return ImmutableList.of(getExecutablePath().toString(), "lt", getPath().toString());
    }

    @Override
    protected List<ProcessArchiveFile> convertToContents(ImmutableList<ImmutableList<String>> lines) {
//        return lines.stream()
//                .map(RAR_LIST::matcher)
//                .filter(Matcher::matches)
//                .map(m -> new ProcessArchiveFile(
//                        m.group(4),
//                        Long.parseLong(m.group(1)),
//                        FileTimes.from(
//                                FileTime.from(parseForDefaultTimeZone(m).toInstant()),
//                                null,
//                                null)))
//                .filter(f -> f.getSize() > 0)
//                .collect(ImmutableList.toImmutableList());
        return null;
    }

    @Nonnull
    private ZonedDateTime parseForDefaultTimeZone(Matcher m) {
        return LocalDateTime.parse(String.format("%sT%s:00", m.group(2), m.group(3))).atZone(ZoneId.systemDefault());
    }

    @Override
    protected List<String> getReadContentsArgs(ImmutableList<ProcessArchiveFile> contents) {
        return ImmutableList.<String>builder()
                .add(getExecutablePath().toString())
                .add("p")
                .add("-inul")
                .add(getPath().toString())
                .addAll(contents.stream().map(ProcessArchiveFile::getName).iterator())
                .build();
    }
}
