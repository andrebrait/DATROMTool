package io.github.datromtool.domain.retool;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.SerializationHelper;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Domain model test for {@link RetoolMetadata} (issue #19 step 1: models only). The fixture
 * {@code retool/metadata/atari-2600-no-intro-trimmed.json} is a hand-trimmed <em>subset</em> of a
 * real upstream file - {@code metadata/Atari - Atari 2600 (No-Intro).json} from
 * <a href="https://github.com/unexpectedpanda/retool-clonelists-metadata">
 * retool-clonelists-metadata</a> (commit as of 2026-07-26, {@code main} branch), which is 911
 * entries / ~53KB in full. The 10 entries kept below are copied byte-for-byte (values, not
 * formatting) from that file: the three Atari 2600 "Air Raiders" group titles referenced by
 * {@link CloneListTest}, plus two multi-language titles ("Donkey Kong Junior (Australia)
 * (En,De)" and "RealSports Baseball (USA, Europe) (En,Fr,De,Es,It)") that don't appear in the
 * untrimmed Air Raiders group but demonstrate the real file's multi-language and non-English
 * shapes.
 */
class RetoolMetadataTest {

    private static final SerializationHelper HELPER = SerializationHelper.getInstance();

    private static Path resource(String name) throws URISyntaxException {
        return Paths.get(ClassLoader.getSystemResource(name).toURI());
    }

    // Matrix row 2: real metadata fixture parses; assert real title -> languages entries.
    @Test
    void parsesRealMetadataFixture() throws Exception {
        RetoolMetadata metadata = HELPER.loadRetoolMetadata(
                resource("retool/metadata/atari-2600-no-intro-trimmed.json"));

        assertEquals(10, metadata.entries().size());
        assertEquals(ImmutableList.of("En"), metadata.entries().get("Air Raiders (USA)").languages());
        assertEquals(ImmutableList.of("En"), metadata.entries().get("Bogey Blaster (Europe)").languages());
        // Single non-English-only entry, real value from the upstream file.
        assertEquals(ImmutableList.of("De"), metadata.entries().get("Top Gun (Germany)").languages());
        // Multi-language entries, real values from the upstream file.
        assertEquals(
                ImmutableList.of("De", "En"),
                metadata.entries().get("Donkey Kong Junior (Australia) (En,De)").languages());
        assertEquals(
                ImmutableList.of("De", "En", "Es", "Fr", "It"),
                metadata.entries().get("RealSports Baseball (USA, Europe) (En,Fr,De,Es,It)").languages());
        assertNull(metadata.entries().get("Not In The File"));
    }
}
