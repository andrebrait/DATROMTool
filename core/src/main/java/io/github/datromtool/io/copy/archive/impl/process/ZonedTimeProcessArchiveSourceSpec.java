package io.github.datromtool.io.copy.archive.impl.process;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.regex.Pattern;

public abstract class ZonedTimeProcessArchiveSourceSpec extends ProcessArchiveSourceSpec {

    protected static final Pattern SPACE_REGEX = Pattern.compile("\\s+");

    /**
     * Cached system zone ID that must be constant during the execution so conversion does not change.
     */
    protected static final ZoneId SYSTEM_ZONE_ID = ZoneId.systemDefault();

    public ZonedTimeProcessArchiveSourceSpec(@Nonnull Path executablePath, @Nonnull Path path) {
        super(executablePath, path);
    }

    public ZonedTimeProcessArchiveSourceSpec(@Nonnull Path executablePath, @Nonnull Path path, @Nonnull Iterable<String> names) {
        super(executablePath, path, names);
    }

    protected String formatDateString(String date) {
        return SPACE_REGEX.matcher(date).replaceFirst("T").replace(',', '.');
    }
}
