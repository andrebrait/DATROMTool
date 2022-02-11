package io.github.datromtool;

import lombok.Value;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

@Value
public class ByteSize implements Comparable<ByteSize> {

    private static final Pattern PARSE_PATTERN = Pattern.compile(
            "^\\s*(\\d+(?:\\.\\d+)?)\\s*([KMGTPE]?B?)?\\s*$",
            CASE_INSENSITIVE
    );

    double size;
    ByteUnit unit;

    public static ByteSize fromBytes(double bytes) {
        ByteUnit normalizedUnit = ByteUnit.getUnit(bytes);
        return new ByteSize(normalizedUnit.convert(bytes), normalizedUnit);
    }

    public static ByteSize fromString(String str) {
        Matcher matcher = PARSE_PATTERN.matcher(str);
        if (matcher.matches()) {
            try {
                double size = Double.parseDouble(matcher.group(1));
                if (size < 0) {
                    throw new IllegalArgumentException(format("Size must be a positive number. Found: '%f' parsed from '%s'", size, str));
                }
                ByteUnit unit = ByteUnit.fromString(matcher.group(2));
                return new ByteSize(size, unit);
            } catch (Exception e) {
                throw new IllegalArgumentException(format("Could not parse a byte size from '%s'", str), e);
            }
        }
        throw new IllegalArgumentException(format("Could not find a match for a byte size in '%s'", str));
    }

    public long getSizeInBytes() {
        return Math.round(size * unit.getSize());
    }

    public ByteSize normalize() {
        long sizeInBytes = getSizeInBytes();
        ByteUnit newUnit = ByteUnit.getUnit(sizeInBytes);
        if (newUnit != unit) {
            return new ByteSize(newUnit.convert(sizeInBytes), newUnit);
        } else {
            return this;
        }
    }

    public String toFormattedString() {
        return format(Locale.US, "%.2f %s", size, unit.getSymbol());
    }

    /**
     * Only guaranteed to be of a fixed size if normalized
     */
    public String toFixedSizeFormattedString() {
        return format(Locale.US, "%7.2f %2s", size, unit.getSymbol());
    }

    @Override
    public int compareTo(ByteSize o) {
        return Long.compare(this.getSizeInBytes(), o.getSizeInBytes());
    }
}
