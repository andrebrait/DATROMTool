package io.github.datromtool;

import lombok.Getter;

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

    public double convert(long amount) {
        return ((double) amount) / getSize();
    }

    public static ByteUnit getUnit(long speed) {
        ByteUnit[] values = values();
        for (int i = values.length - 1; i >= 0; i--) {
            ByteUnit unit = values[i];
            if (speed >= unit.getSize()) {
                return unit;
            }
        }
        return BYTE;
    }

}
