package io.github.datromtool.domain.serialization;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.HexFormat;

public final class SpacedHexArraySerializer extends ValueSerializer<byte[]> {

    private static final HexFormat SPACED_UPPER_CASE = HexFormat.ofDelimiter(" ").withUpperCase();

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(SPACED_UPPER_CASE.formatHex(value));
        }
    }
}
