package io.github.datromtool.domain.datafile.logiqx;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.domain.datafile.logiqx.enumerations.YesNo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A ROM's header is part of its value: two ROMs that describe the same dumped file must
 * compare, hash, and round-trip as the same ROM regardless of which array instance holds
 * the header bytes.
 */
class RomTest {

    /** Four bytes so the last one is distinguishable from a truncated rendering. */
    private static final ImmutableList<Byte> NES_HEADER =
            ImmutableList.of((byte) 0x4E, (byte) 0x45, (byte) 0x53, (byte) 0x1A);

    private static Rom romWithHeader(ImmutableList<Byte> header) {
        return new Rom(
                "Some Game (USA).nes",
                1024L,
                header,
                YesNo.NO,
                "12345678",
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void givenByteEqualHeadersInDistinctInstances_whenCompared_thenRomsAreEqual() {
        Rom one = romWithHeader(copyOf(NES_HEADER));
        Rom other = romWithHeader(copyOf(NES_HEADER));

        assertNotEquals(
                System.identityHashCode(one.header()),
                System.identityHashCode(other.header()),
                "test premise: the two headers must be distinct instances");
        assertEquals(one, other, "ROMs with byte-equal headers must be equal");
        assertEquals(
                one.hashCode(),
                other.hashCode(),
                "ROMs with byte-equal headers must share a hash code");
    }

    @Test
    void givenDifferentHeaderContent_whenCompared_thenRomsAreNotEqual() {
        Rom one = romWithHeader(NES_HEADER);
        Rom other = romWithHeader(
                ImmutableList.of((byte) 0x4E, (byte) 0x45, (byte) 0x53, (byte) 0x1B));

        assertNotEquals(one, other, "ROMs whose header bytes differ must not be equal");
    }

    @Test
    void givenAHeaderedRom_whenRenderedAsText_thenTheHeaderContentIsVisible() {
        String rendered = romWithHeader(NES_HEADER).toString();

        assertTrue(
                rendered.contains(NES_HEADER.toString()),
                () -> "toString must render header content, got: " + rendered);
    }

    @Test
    void givenAHeaderedRomInADatafile_whenRoundTrippedThroughXml_thenTheHeaderSurvives(
            @TempDir Path tempDir) throws Exception {
        SerializationHelper helper = SerializationHelper.getInstance(tempDir);
        Datafile datafile = Datafile.builder()
                .games(ImmutableList.of(Game.builder()
                        .name("Some Game (USA)")
                        .description("Some Game (USA)")
                        .roms(ImmutableList.of(romWithHeader(NES_HEADER)))
                        .build()))
                .build();

        byte[] xml = helper.getXmlMapper().writeValueAsBytes(datafile);
        Datafile parsed = helper.loadXml(new ByteArrayInputStream(xml), Datafile.class);

        ImmutableList<Byte> parsedHeader = parsed.getGames().get(0).getRoms().get(0).header();
        assertEquals(
                NES_HEADER,
                parsedHeader,
                () -> "header bytes must survive the round trip, wrote: " + new String(xml));
        assertEquals(datafile, parsed, "a round-tripped DAT must equal the one written");
    }

    /** A distinct instance holding the same bytes. */
    private static ImmutableList<Byte> copyOf(ImmutableList<Byte> header) {
        return ImmutableList.copyOf(new ArrayList<>(header));
    }
}
