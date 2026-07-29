package io.github.datromtool.domain.serialization;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Bytes;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.HexFormat;

public final class SpacedHexArraySerializer extends ValueSerializer<ImmutableList<Byte>> {

    private static final HexFormat SPACED_UPPER_CASE = HexFormat.ofDelimiter(" ").withUpperCase();

    @Override
    public void serialize(
            ImmutableList<Byte> value,
            JsonGenerator gen,
            SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(SPACED_UPPER_CASE.formatHex(Bytes.toArray(value)));
        }
    }
}
