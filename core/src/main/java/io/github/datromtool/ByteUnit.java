package io.github.datromtool;

import lombok.Getter;

import static java.lang.String.format;

@Getter
public enum ByteUnit {
    BYTE(1L, "B"),
    KILOBYTE(BYTE.getSize() * 1024L, "KB"),
    MEGABYTE(KILOBYTE.getSize() * 1024L, "MB"),
    GIGABYTE(MEGABYTE.getSize() * 1024L, "GB"),
    TERABYTE(GIGABYTE.getSize() * 1024L, "TB"),
    PETABYTE(TERABYTE.getSize() * 1024L, "PB"),
    EXABYTE(PETABYTE.getSize() * 1024L, "EB");

    private final long size;
    private final String symbol;

    ByteUnit(long size, String symbol) {
        this.size = size;
        this.symbol = symbol;
    }

    public double convert(double amount) {
        return amount / getSize();
    }

    public static ByteUnit getUnit(double bytes) {
        ByteUnit[] values = values();
        for (int i = values.length - 1; i >= 0; i--) {
            ByteUnit unit = values[i];
            if (bytes >= unit.getSize()) {
                return unit;
            }
        }
        return BYTE;
    }

    public static ByteUnit fromString(String str) {
        String value = str != null
                ? str.trim().toUpperCase()
                : null;
        if (value == null || value.isEmpty()) {
            return BYTE;
        }
        for (ByteUnit unit : values()) {
            if ((value.length() == 1 && unit.getSymbol().startsWith(value)) || unit.getSymbol().equals(value)) {
                return unit;
            }
        }
        throw new IllegalArgumentException(format("Could not parse ByteUnit from '%s'", str));
    }

}
