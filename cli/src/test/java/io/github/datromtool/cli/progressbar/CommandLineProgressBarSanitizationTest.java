package io.github.datromtool.cli.progressbar;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 * A filename is attacker-controlled data — getting a file onto the user's disk is all it takes.
 * Rendering one into the progress bar unfiltered lets it carry terminal escape sequences, which
 * rewrite the window title, move the cursor, or hide what is really being scanned (CWE-150).
 * The bar must show what the file is called, not obey it.
 */
class CommandLineProgressBarSanitizationTest {

    /** A carriage return, then an OSC "set window title" sequence, in a filename. */
    private static final String EVIL_NAME = "evil\r\u001B]0;pwned\u0007.rom";

    private static Path evilPath() {
        try {
            return Path.of(EVIL_NAME);
        } catch (InvalidPathException e) {
            return abort("this filesystem cannot represent a control character in a path");
        }
    }

    private static String render(Path path) throws Exception {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .streams(new ByteArrayInputStream(new byte[0]), captured)
                .build()) {
            CommandLineProgressBar bar =
                    new CommandLineProgressBar(terminal, "Scanning", "Scanning input...");
            bar.reportListing(path);
            bar.init(1);
            bar.reportTotalItems(1);
            bar.reportStart(1, path, 1024L);
            bar.reportFinish(1, path);
            terminal.flush();
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

    @Test
    void controlCharactersInAFilenameAreRenderedInertly() throws Exception {
        String rendered = render(evilPath());

        assertTrue(
                rendered.contains("evil"),
                () -> "test premise: the filename must actually reach the output, got:\n"
                        + rendered);
        assertFalse(
                rendered.contains("\u001B]0;"),
                () -> "an OSC sequence from a filename must not reach the terminal, got:\n"
                        + rendered);
        assertTrue(
                rendered.contains("\\u001B"),
                () -> "the escape character must be rendered visibly, got:\n" + rendered);
        assertTrue(
                rendered.contains("\\u000D"),
                () -> "the carriage return must be rendered visibly, got:\n" + rendered);
    }
}
