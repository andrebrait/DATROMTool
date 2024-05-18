package io.github.datromtool.io.copy.archive.impl.process;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.datromtool.io.copy.FileTimes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class SevenZipRarArchiveSourceSpec extends ZonedTimeProcessArchiveSourceSpec {

    public SevenZipRarArchiveSourceSpec(@Nonnull Path executablePath, @Nonnull Path path) {
        super(executablePath, path);
    }

    public SevenZipRarArchiveSourceSpec(@Nonnull Path executablePath, @Nonnull Path path, @Nonnull Iterable<String> names) {
        super(executablePath, path, names);
    }

    @Override
    protected List<String> getListContentsArgs() {
        return ImmutableList.of(getExecutablePath().toString(), "l", "-ba", "-slt", getPath().toString());
    }

    @Override
    List<ProcessArchiveFile> convertToContents(ImmutableList<ImmutableList<String>> lines) {
        return lines.stream()
                .map(Collection::stream)
                .map(stream -> stream.map(SevenZipRarArchiveSourceSpec::splitKeyValues)
                        .filter(a -> a.length == 2)
                        .collect(ImmutableMap.toImmutableMap(a -> a[0], a -> a[1])))
                .filter(map -> "-".equals(map.get("Folder")))
                .filter(map -> map.containsKey("Size"))
                .map(map -> new ProcessArchiveFile(
                        map.get("Path"),
                        Long.parseLong(requireNonNull(map.get("Size"))),
                        FileTimes.from(
                                parseFileTime(map.get("Modified")),
                                parseFileTime(map.get("Accessed")),
                                parseFileTime(map.get("Created")))))
                .collect(ImmutableList.toImmutableList());
    }

    @Nonnull
    private static String[] splitKeyValues(String s) {
        return Arrays.stream(s.split("=", 2))
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .toArray(String[]::new);
    }

    @Nullable
    private FileTime parseFileTime(String s) {
        OffsetDateTime zonedDateTime = parseZonedDateTimeForDefaultTimeZone(s);
        if (zonedDateTime != null) {
            return FileTime.from(zonedDateTime.toInstant());
        }
        return null;
    }

    @Nullable
    private OffsetDateTime parseZonedDateTimeForDefaultTimeZone(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        // return LocalDateTime.parse(formatDateString(s)).atZone(ZoneId.systemDefault());

        // Workaround for the fact 7z for some reason thinks it should apply the current offset to a date, not the
        // offset of when the date is supposed to be, when launched from Java.
        return LocalDateTime.parse(formatDateString(s)).atOffset(SYSTEM_ZONE_ID.getRules().getOffset(Instant.now()));
    }

    @Override
    List<String> getReadContentsArgs(ImmutableList<ProcessArchiveFile> contents) {
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
