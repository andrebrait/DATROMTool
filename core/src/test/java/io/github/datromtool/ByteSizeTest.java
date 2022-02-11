package io.github.datromtool;

import org.junit.jupiter.api.Test;

import static io.github.datromtool.ByteUnit.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteSizeTest {

    @Test
    void testFromBytes_noScaling() {
        ByteSize size = ByteSize.fromBytes(1000);
        assertEquals(1000.0, size.getSize());
        assertEquals(ByteUnit.BYTE, size.getUnit());
    }

    @Test
    void testFromBytes_scaleToKiloBytes() {
        // 1 KB
        ByteSize size = ByteSize.fromBytes(KILOBYTE.getSize());
        assertEquals(1.0, size.getSize());
        assertEquals(KILOBYTE, size.getUnit());
    }

    @Test
    void testFromBytes_scaleToKiloBytes_fraction() {
        // 1 KB + 512 B
        ByteSize size = ByteSize.fromBytes(KILOBYTE.getSize() + 512);
        assertEquals(1.5, size.getSize());
        assertEquals(KILOBYTE, size.getUnit());
    }

    @Test
    void testFromBytes_scaleToMegaBytes() {
        // 456 MB + 256 KB
        ByteSize size = ByteSize.fromBytes(MEGABYTE.getSize() * 456 + KILOBYTE.getSize() * 256);
        assertEquals(456.25, size.getSize());
        assertEquals(MEGABYTE, size.getUnit());
    }

    @Test
    void testFromString_denormalized() {
        assertEquals(new ByteSize(200, BYTE), ByteSize.fromString("200"));
        assertEquals(new ByteSize(200, BYTE), ByteSize.fromString("200b"));
        assertEquals(new ByteSize(200, BYTE), ByteSize.fromString("200B"));
        assertEquals(new ByteSize(200, BYTE), ByteSize.fromString(" 200 b "));
        assertEquals(new ByteSize(200, BYTE), ByteSize.fromString(" 200 B "));
        assertEquals(new ByteSize(1, KILOBYTE), ByteSize.fromString("1k"));
        assertEquals(new ByteSize(1, KILOBYTE), ByteSize.fromString("1K"));
        assertEquals(new ByteSize(1, KILOBYTE), ByteSize.fromString("1kb"));
        assertEquals(new ByteSize(1, KILOBYTE), ByteSize.fromString("1KB"));
        assertEquals(new ByteSize(1, KILOBYTE), ByteSize.fromString("1 k"));
        assertEquals(new ByteSize(1, KILOBYTE), ByteSize.fromString("1 K"));
        assertEquals(new ByteSize(1, KILOBYTE), ByteSize.fromString("1 kb"));
        assertEquals(new ByteSize(1, KILOBYTE), ByteSize.fromString("1 KB"));
        assertEquals(new ByteSize(1, KILOBYTE), ByteSize.fromString(" 1 KB "));
        assertEquals(new ByteSize(1.5, KILOBYTE), ByteSize.fromString("1.5k"));
        assertEquals(new ByteSize(12.6, MEGABYTE), ByteSize.fromString("12.6MB"));
    }

    @Test
    void testFromString_normalized() {
        assertEquals(new ByteSize(1024, BYTE), ByteSize.fromString("1024 B"));
        assertEquals(new ByteSize(1, KILOBYTE), ByteSize.fromString("1024 B").normalize());
        assertEquals(new ByteSize(0.5, KILOBYTE), ByteSize.fromString("0.5k"));
        assertEquals(new ByteSize(512, BYTE), ByteSize.fromString("0.5k").normalize());
    }

    @Test
    void testNormalize() {
        assertEquals(new ByteSize(512, BYTE), new ByteSize(0.5, KILOBYTE).normalize());
        assertEquals(new ByteSize(1, KILOBYTE), new ByteSize(1024, BYTE).normalize());
        assertEquals(new ByteSize(3, KILOBYTE), new ByteSize(3, KILOBYTE).normalize());
        assertEquals(new ByteSize(256, KILOBYTE), new ByteSize(0.25, MEGABYTE).normalize());
    }

    @Test
    void testToFormattedString() {
        assertEquals("1.00 B", new ByteSize(1, BYTE).toFormattedString());
        assertEquals("1.00 KB", new ByteSize(1, KILOBYTE).toFormattedString());
        assertEquals("34.50 EB", new ByteSize(34.5, EXABYTE).toFormattedString());
        assertEquals("4608.50 TB", new ByteSize(4608.5, TERABYTE).toFormattedString());
        assertEquals("4.50 PB", new ByteSize(4608.5, TERABYTE).normalize().toFormattedString());
    }

    @Test
    void toFixedSizeFormattedString() {
        assertEquals("   1.00  B", new ByteSize(1, BYTE).toFixedSizeFormattedString());
        assertEquals("   1.00 KB", new ByteSize(1, KILOBYTE).toFixedSizeFormattedString());
        assertEquals("  34.50 EB", new ByteSize(34.5, EXABYTE).toFixedSizeFormattedString());
        assertEquals("4608.50 TB", new ByteSize(4608.5, TERABYTE).toFixedSizeFormattedString());
        assertEquals("14608.50 TB", new ByteSize(14608.5, TERABYTE).toFixedSizeFormattedString());
        assertEquals("   4.50 PB", new ByteSize(4608.5, TERABYTE).normalize().toFixedSizeFormattedString());
    }
}