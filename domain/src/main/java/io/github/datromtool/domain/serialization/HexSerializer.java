package io.github.datromtool.domain.serialization;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public final class HexSerializer extends ValueSerializer<Long> {

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(Long.toHexString(value));
        }
    }
}
