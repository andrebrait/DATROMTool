package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.datafile.Game;
import io.github.datromtool.domain.datafile.Rom;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.domain.detector.Rule;
import org.junit.jupiter.api.Test;

import static io.github.datromtool.io.ArchiveType.RAR;
import static io.github.datromtool.io.ArchiveType.SEVEN_ZIP;
import static io.github.datromtool.io.ArchiveType.ZIP;
import static io.github.datromtool.io.FileScannerParameters.DEFAULT_BUFFER_SIZE;
import static io.github.datromtool.io.FileScannerParameters.forDatWithDetector;
import static io.github.datromtool.io.FileScannerParameters.withDefaults;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileScannerParametersTest {

    @Test
    void testWithDefaults() {
        FileScannerParameters parameters = withDefaults();
        assertNotNull(parameters);
        assertTrue(parameters.getAlsoScanArchives().isEmpty());
        assertEquals(DEFAULT_BUFFER_SIZE, parameters.getBufferSize());
        assertEquals(0L, parameters.getMinRomSize());
        assertEquals(Long.MAX_VALUE, parameters.getMaxRomSize());
        assertEquals("0.00 B", parameters.getMinRomSizeStr());
        assertEquals("8.00 EB", parameters.getMaxRomSizeStr());
        assertFalse(parameters.isUseLazyDetector());
    }

    @Test
    void testForDatWithDetector_nullDetector() {
        Datafile datafile = Datafile.builder()
                .games(ImmutableList.of(Game.builder()
                        .name("Test game 1")
                        .description("Test game 1")
                        .roms(ImmutableList.of(Rom.builder()
                                .name("Test rom 1.ext")
                                .size(8 * 1024L)
                                .build()))
                        .build())).build();
        FileScannerParameters parameters =
                forDatWithDetector(AppConfig.builder().build(), datafile, null);
        assertNotNull(parameters);
        assertTrue(parameters.getAlsoScanArchives().isEmpty());
        assertEquals(DEFAULT_BUFFER_SIZE, parameters.getBufferSize());
        assertEquals(8 * 1024L, parameters.getMinRomSize());
        assertEquals(8 * 1024L, parameters.getMaxRomSize());
        assertEquals("8.00 KB", parameters.getMinRomSizeStr());
        assertEquals("8.00 KB", parameters.getMaxRomSizeStr());
        assertFalse(parameters.isUseLazyDetector());
    }

    @Test
    void testForDatWithDetector_nullDetector_multipleRoms() {
        Datafile datafile = Datafile.builder()
                .games(ImmutableList.of(Game.builder()
                        .name("Test game 1")
                        .description("Test game 1")
                        .roms(ImmutableList.of(
                                Rom.builder()
                                        .name("Test rom 1.ext")
                                        .size(8 * 1024L)
                                        .build(),
                                Rom.builder()
                                        .name("Test rom 2.zip")
                                        .size(16 * 1024L)
                                        .build()))
                        .build())).build();
        FileScannerParameters parameters =
                forDatWithDetector(AppConfig.builder().build(), datafile, null);
        assertNotNull(parameters);
        assertEquals(1, parameters.getAlsoScanArchives().size());
        assertTrue(parameters.getAlsoScanArchives().contains(ZIP));
        assertEquals(DEFAULT_BUFFER_SIZE, parameters.getBufferSize());
        assertEquals(8 * 1024L, parameters.getMinRomSize());
        assertEquals(16 * 1024L, parameters.getMaxRomSize());
        assertEquals("8.00 KB", parameters.getMinRomSizeStr());
        assertEquals("16.00 KB", parameters.getMaxRomSizeStr());
        assertFalse(parameters.isUseLazyDetector());
    }

    @Test
    void testForDatWithDetector_nullDetector_multipleRoms_multipleGames() {
        Datafile datafile = Datafile.builder()
                .games(ImmutableList.of(
                        Game.builder()
                                .name("Test game 1")
                                .description("Test game 1")
                                .roms(ImmutableList.of(
                                        Rom.builder()
                                                .name("Test rom 1.ext")
                                                .size(8 * 1024L)
                                                .build(),
                                        Rom.builder()
                                                .name("Test rom 2.zip")
                                                .size(16 * 1024L)
                                                .build()))
                                .build(),
                        Game.builder()
                                .name("Test game 2")
                                .description("Test game 2")
                                .roms(ImmutableList.of(
                                        Rom.builder()
                                                .name("Test rom 2-1.7z")
                                                .size(4 * 1024L)
                                                .build(),
                                        Rom.builder()
                                                .name("Test rom 2-2.rar")
                                                .size(27 * 1024L * 1024L)
                                                .build()))
                                .build()))
                .build();
        FileScannerParameters parameters =
                forDatWithDetector(AppConfig.builder().build(), datafile, null);
        assertNotNull(parameters);
        assertEquals(3, parameters.getAlsoScanArchives().size());
        assertTrue(parameters.getAlsoScanArchives().contains(ZIP));
        assertTrue(parameters.getAlsoScanArchives().contains(SEVEN_ZIP));
        assertTrue(parameters.getAlsoScanArchives().contains(RAR));
        assertEquals(DEFAULT_BUFFER_SIZE, parameters.getBufferSize());
        assertEquals(4 * 1024L, parameters.getMinRomSize());
        assertEquals(27 * 1024L * 1024L, parameters.getMaxRomSize());
        assertEquals("4.00 KB", parameters.getMinRomSizeStr());
        assertEquals("27.00 MB", parameters.getMaxRomSizeStr());
        assertFalse(parameters.isUseLazyDetector());
    }

    @Test
    void testForDatWithDetector() {
        Detector detector = Detector.builder()
                .name("Test detector")
                .author("Test author")
                .rules(ImmutableList.of(Rule.builder().build()))
                .build();
        Datafile datafile = Datafile.builder()
                .games(ImmutableList.of(Game.builder()
                        .name("Test game 1")
                        .description("Test game 1")
                        .roms(ImmutableList.of(Rom.builder()
                                .name("Test rom 1.ext")
                                .size(8 * 1024L)
                                .build()))
                        .build())).build();
        FileScannerParameters parameters =
                forDatWithDetector(AppConfig.builder().build(), datafile, detector);
        assertNotNull(parameters);
        assertTrue(parameters.getAlsoScanArchives().isEmpty());
        assertEquals(8 * 1024L, parameters.getMinRomSize());
        assertEquals(8 * 1024L, parameters.getMaxRomSize());
        assertEquals("8.00 KB", parameters.getMinRomSizeStr());
        assertEquals("8.00 KB", parameters.getMaxRomSizeStr());
        assertTrue(parameters.isUseLazyDetector());
    }
}