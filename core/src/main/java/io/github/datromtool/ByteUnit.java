package io.github.datromtool;

import lombok.Getter;

@Getter
public enum ByteUnit {
    BYTE(1L, "B"),
    KILOBYTE(BYTE.getSize() * 1024L, "KB"),
    MEGABYTE(KILOBYTE.getSize() * 1024L, "MB"),
    GIGABYTE(MEGABYTE.getSize() * 1024L, "GB"),
    TERABYTE(GIGABYTE.getSize() * 1024L, "TB");

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
        if (speed > TERABYTE.getSize()) {
            return TERABYTE;
        } else if (speed > GIGABYTE.getSize()) {
            return GIGABYTE;
        } else if (speed > MEGABYTE.getSize()) {
            return MEGABYTE;
        } else if (speed > KILOBYTE.getSize()) {
            return KILOBYTE;
        } else {
            return BYTE;
        }
    }

}
