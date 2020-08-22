package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.datafile.Game;
import io.github.datromtool.domain.datafile.Rom;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.domain.detector.Rule;
import org.testng.annotations.Test;

import static io.github.datromtool.io.ArchiveType.RAR;
import static io.github.datromtool.io.ArchiveType.SEVEN_ZIP;
import static io.github.datromtool.io.ArchiveType.ZIP;
import static io.github.datromtool.io.FileScannerParameters.DEFAULT_BUFFER_SIZE;
import static io.github.datromtool.io.FileScannerParameters.forDatWithDetector;
import static io.github.datromtool.io.FileScannerParameters.withDefaults;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class FileScannerParametersTest {

    @Test
    public void testWithDefaults() {
        FileScannerParameters parameters = withDefaults();
        assertNotNull(parameters);
        assertTrue(parameters.getAlsoScanArchives().isEmpty());
        assertEquals(parameters.getBufferSize(), DEFAULT_BUFFER_SIZE);
        assertEquals(parameters.getMinRomSize(), 0L);
        assertEquals(parameters.getMaxRomSize(), Long.MAX_VALUE);
        assertEquals(parameters.getMinRomSizeStr(), "0.00 B");
        assertEquals(parameters.getMaxRomSizeStr(), "8.00 EB");
        assertFalse(parameters.isUseLazyDetector());
    }

    @Test
    public void testForDatWithDetector_nullDetector() {
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
        assertEquals(parameters.getBufferSize(), DEFAULT_BUFFER_SIZE);
        assertEquals(parameters.getMinRomSize(), 8 * 1024L);
        assertEquals(parameters.getMaxRomSize(), 8 * 1024L);
        assertEquals(parameters.getMinRomSizeStr(), "8.00 KB");
        assertEquals(parameters.getMaxRomSizeStr(), "8.00 KB");
        assertFalse(parameters.isUseLazyDetector());
    }

    @Test
    public void testForDatWithDetector_nullDetector_multipleRoms() {
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
        assertEquals(parameters.getAlsoScanArchives().size(), 1);
        assertTrue(parameters.getAlsoScanArchives().contains(ZIP));
        assertEquals(parameters.getBufferSize(), DEFAULT_BUFFER_SIZE);
        assertEquals(parameters.getMinRomSize(), 8 * 1024L);
        assertEquals(parameters.getMaxRomSize(), 16 * 1024L);
        assertEquals(parameters.getMinRomSizeStr(), "8.00 KB");
        assertEquals(parameters.getMaxRomSizeStr(), "16.00 KB");
        assertFalse(parameters.isUseLazyDetector());
    }

    @Test
    public void testForDatWithDetector_nullDetector_multipleRoms_multipleGames() {
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
        assertEquals(parameters.getAlsoScanArchives().size(), 3);
        assertTrue(parameters.getAlsoScanArchives().contains(ZIP));
        assertTrue(parameters.getAlsoScanArchives().contains(SEVEN_ZIP));
        assertTrue(parameters.getAlsoScanArchives().contains(RAR));
        assertEquals(parameters.getBufferSize(), DEFAULT_BUFFER_SIZE);
        assertEquals(parameters.getMinRomSize(), 4 * 1024L);
        assertEquals(parameters.getMaxRomSize(), 27 * 1024L * 1024L);
        assertEquals(parameters.getMinRomSizeStr(), "4.00 KB");
        assertEquals(parameters.getMaxRomSizeStr(), "27.00 MB");
        assertFalse(parameters.isUseLazyDetector());
    }

    @Test
    public void testForDatWithDetector() {
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
        assertEquals(parameters.getMinRomSize(), 8 * 1024L);
        assertEquals(parameters.getMaxRomSize(), 8 * 1024L);
        assertEquals(parameters.getMinRomSizeStr(), "8.00 KB");
        assertEquals(parameters.getMaxRomSizeStr(), "8.00 KB");
        assertTrue(parameters.isUseLazyDetector());
    }
}