package io.github.datromtool;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Overrides jackson-databind's built-in {@link Path} handling, which serializes via
 * {@link Path#toUri()} — turning a relative path into an absolute {@code file:///...} URI
 * (resolved against the JVM's working directory) on write, and back into an absolute path on
 * read. That is unsuitable for a portable profile file (issue #15): a profile checked out on
 * another machine, or simply run from a different working directory, must keep a relative
 * {@code dats}/{@code dirs}/{@code outputDir}/{@code outputFile} path relative, and the
 * serialized form should read as a plain path string, not a URI.
 *
 * <p>Registered on the JSON and YAML mappers only (profile files and, transitively,
 * {@link io.github.datromtool.data.FileOutputOptions}/{@link io.github.datromtool.data.TextOutputOptions},
 * are only ever bound through those formats); the XML mapper is untouched since none of the
 * XML-bound types ({@code Datafile}, {@code Detector}) have {@link Path} fields.
 */
final class PathJacksonModule extends SimpleModule {

    static final PathJacksonModule INSTANCE = new PathJacksonModule();

    private PathJacksonModule() {
        super("NioPathAsPlainString");
        addSerializer(Path.class, new ValueSerializer<Path>() {
            @Override
            public void serialize(Path value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
                gen.writeString(value.toString());
            }
        });
        addDeserializer(Path.class, new ValueDeserializer<Path>() {
            @Override
            public Path deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
                return Paths.get(p.getString());
            }
        });
    }
}
