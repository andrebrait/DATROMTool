package io.github.datromtool.io.copy.archive.impl.process;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.datromtool.io.copy.FileTimes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class UnrarRarArchiveSourceSpec extends ZonedTimeProcessArchiveSourceSpec {

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
        return lines.stream()
                .map(Collection::stream)
                .map(stream -> stream.map(UnrarRarArchiveSourceSpec::splitKeyValues)
                        .filter(a -> a.length == 2)
                        .collect(ImmutableMap.toImmutableMap(a -> a[0], a -> a[1])))
                .filter(map -> "File".equals(map.get("Type")))
                .filter(map -> map.containsKey("Size"))
                .map(map -> new ProcessArchiveFile(
                        map.get("Name"),
                        Long.parseLong(requireNonNull(map.get("Size"))),
                        FileTimes.from(
                                parseFileTime(map.get("Modified")),
                                parseFileTime(map.get("Accessed")),
                                parseFileTime(map.get("Created")))))
                .filter(f -> f.getSize() > 0)
                .collect(ImmutableList.toImmutableList());
    }

    @Nonnull
    private static String[] splitKeyValues(String s) {
        return Arrays.stream(s.split(":", 2))
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .toArray(String[]::new);
    }

    @Nullable
    private FileTime parseFileTime(String s) {
        ZonedDateTime zonedDateTime = parseZonedDateTimeForDefaultTimeZone(s);
        if (zonedDateTime != null) {
            return FileTime.from(zonedDateTime.toInstant());
        }
        return null;
    }

    @Nullable
    private ZonedDateTime parseZonedDateTimeForDefaultTimeZone(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(formatDateString(s)).atZone(SYSTEM_ZONE_ID);
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
