package io.github.datromtool.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.IntConsumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the shared invariant of the five performance-primitive wrapper records (issue #14 step
 * 3): construction rejects zero and negative inputs with a message identifying what must be
 * positive, and a valid value round-trips through the accessor unchanged.
 */
class PerformancePrimitiveRecordsTest {

    private record Case(String name, IntConsumer constructor) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Case> nonPositiveCases() {
        return Stream.of(
                new Case("ScanThreads", v -> new ScanThreads(v)),
                new Case("CopyThreads", v -> new CopyThreads(v)),
                new Case("ScanBufferSize", v -> new ScanBufferSize(v)),
                new Case("ScanMaxBufferSize", v -> new ScanMaxBufferSize(v)),
                new Case("CopyBufferSize", v -> new CopyBufferSize(v)));
    }

    @ParameterizedTest(name = "{0} rejects zero")
    @MethodSource("nonPositiveCases")
    void rejectsZero(Case c) {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> c.constructor().accept(0),
                c.name() + "(0) must be rejected as non-positive");
        assertTrue(
                thrown.getMessage().toLowerCase().contains("positive"),
                c.name() + " error must explain the value must be positive, got: " + thrown.getMessage());
    }

    @ParameterizedTest(name = "{0} rejects negative")
    @MethodSource("nonPositiveCases")
    void rejectsNegative(Case c) {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> c.constructor().accept(-1),
                c.name() + "(-1) must be rejected as non-positive");
        assertTrue(
                thrown.getMessage().toLowerCase().contains("positive"),
                c.name() + " error must explain the value must be positive, got: " + thrown.getMessage());
    }

    @Test
    void scanThreadsPreservesExactCliValidationMessage() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> new ScanThreads(0));
        assertEquals("Number of threads should be a positive number", thrown.getMessage());
    }

    @Test
    void copyThreadsPreservesExactCliValidationMessage() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> new CopyThreads(0));
        assertEquals("Number of threads should be a positive number", thrown.getMessage());
    }

    @Test
    void scanThreadsAccessorReturnsConstructedValue() {
        assertEquals(4, new ScanThreads(4).value());
    }

    @Test
    void copyThreadsAccessorReturnsConstructedValue() {
        assertEquals(4, new CopyThreads(4).value());
    }

    @Test
    void scanBufferSizeAccessorReturnsConstructedBytes() {
        assertEquals(1024, new ScanBufferSize(1024).bytes());
    }

    @Test
    void scanMaxBufferSizeAccessorReturnsConstructedBytes() {
        assertEquals(1024, new ScanMaxBufferSize(1024).bytes());
    }

    @Test
    void copyBufferSizeAccessorReturnsConstructedBytes() {
        assertEquals(1024, new CopyBufferSize(1024).bytes());
    }
}
