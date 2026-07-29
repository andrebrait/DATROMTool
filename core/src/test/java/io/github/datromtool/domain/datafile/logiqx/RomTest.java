package io.github.datromtool.domain.datafile.logiqx;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.domain.datafile.logiqx.enumerations.YesNo;
import io.github.datromtool.util.XMLValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

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

        assertNotSame(
                one.header(),
                other.header(),
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

    static Stream<Arguments> headerShapes() {
        return Stream.of(
                argumentSet("no header at all", null, null),
                argumentSet("an empty header", ImmutableList.of(), ""),
                argumentSet("a single byte", ImmutableList.of((byte) 0x1A), "1A"),
                argumentSet("several bytes", NES_HEADER, "4E 45 53 1A"),
                argumentSet(
                        "a byte a text format would have to escape",
                        ImmutableList.of((byte) 0x0A, (byte) 0x0D),
                        "0A 0D"));
    }

    @ParameterizedTest
    @MethodSource("headerShapes")
    void givenARomInADatafile_whenRoundTrippedThroughXml_thenItsHeaderSurvives(
            ImmutableList<Byte> header,
            String expectedAttributeText,
            @TempDir Path tempDir) throws Exception {
        SerializationHelper helper = SerializationHelper.getInstance(tempDir);
        Datafile datafile = Datafile.builder()
                .games(ImmutableList.of(Game.builder()
                        .name("Some Game (USA)")
                        .description("Some Game (USA)")
                        .roms(ImmutableList.of(romWithHeader(header)))
                        .build()))
                .build();

        byte[] xml = helper.getXmlMapper().writeValueAsBytes(datafile);
        String written = new String(xml, UTF_8);
        Datafile parsed = helper.loadXml(new ByteArrayInputStream(xml), Datafile.class);

        if (expectedAttributeText == null) {
            assertFalse(
                    written.contains("header="),
                    () -> "a ROM without a header must not emit the attribute, wrote: " + written);
        } else {
            assertTrue(
                    written.contains("header=\"" + expectedAttributeText + "\""),
                    () -> "the header attribute must be written as spaced upper-case hex, wrote: "
                            + written);
        }
        assertEquals(
                header,
                parsed.getGames().get(0).getRoms().get(0).header(),
                () -> "header bytes must survive the round trip, wrote: " + written);
        assertEquals(datafile, parsed, "a round-tripped DAT must equal the one written");
        // The same check every other DAT round-trip test in the repo makes: what we emit must
        // still be a valid Logiqx document.
        XMLValidator.validateLogiqxDat(xml);
    }

    /** A distinct instance holding the same bytes. */
    private static ImmutableList<Byte> copyOf(ImmutableList<Byte> header) {
        return ImmutableList.copyOf(new ArrayList<>(header));
    }
}
