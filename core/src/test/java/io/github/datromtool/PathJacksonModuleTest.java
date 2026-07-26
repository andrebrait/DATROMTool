package io.github.datromtool;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link PathJacksonModule}'s override of jackson-databind's built-in {@link Path}
 * handling: a relative path must serialize as a plain path string and round-trip back to an
 * equal <em>relative</em> path, not get absolutized into a {@code file://} URI against the JVM's
 * working directory (issue #15 step 2 — profile files carrying relative
 * {@code dats}/{@code dirs}/{@code outputDir}/{@code outputFile} paths must stay portable).
 */
class PathJacksonModuleTest {

    private static final JsonMapper JSON = SerializationHelper.getInstance().getJsonMapper();
    private static final YAMLMapper YAML = SerializationHelper.getInstance().getYamlMapper();

    @Test
    void relativePathSerializesAsPlainStringNotUri() {
        Path path = Paths.get("roms/game.zip");
        String json = JSON.writeValueAsString(path);
        assertFalse(json.contains("file:"), "relative Path must not serialize as a file:// URI, got: " + json);
        assertEquals("\"roms/game.zip\"", json.trim());
    }

    @Test
    void relativePathRoundTripsThroughJsonStayingRelative() {
        Path path = Paths.get("roms/game.zip");
        String json = JSON.writeValueAsString(path);
        Path roundTripped = JSON.readValue(json, Path.class);
        assertEquals(path, roundTripped);
        assertFalse(roundTripped.isAbsolute(), "round-tripped path must stay relative, got: " + roundTripped);
    }

    @Test
    void relativePathRoundTripsThroughYamlStayingRelative() {
        Path path = Paths.get("roms/game.zip");
        String yaml = YAML.writeValueAsString(path);
        Path roundTripped = YAML.readValue(yaml, Path.class);
        assertEquals(path, roundTripped);
        assertFalse(roundTripped.isAbsolute(), "round-tripped path must stay relative, got: " + roundTripped);
    }

    @Test
    void absolutePathRoundTripsThroughJson() {
        Path path = Paths.get("/tmp/roms/game.zip");
        String json = JSON.writeValueAsString(path);
        assertFalse(json.contains("file:"), "absolute Path must not serialize as a file:// URI, got: " + json);
        Path roundTripped = JSON.readValue(json, Path.class);
        assertEquals(path, roundTripped);
        assertTrue(roundTripped.isAbsolute());
    }
}
