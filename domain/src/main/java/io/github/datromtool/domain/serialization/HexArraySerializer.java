package io.github.datromtool.domain.serialization;

import org.apache.commons.codec.binary.Hex;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public final class HexArraySerializer extends ValueSerializer<byte[]> {

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(Hex.encodeHexString(value).toUpperCase());
        }
    }
}
