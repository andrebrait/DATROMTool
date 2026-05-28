package io.github.datromtool.domain.serialization;

import io.github.datromtool.domain.detector.Rule;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

public final class HexDeserializer extends ValueDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) {
        String val = p.getValueAsString();
        try {
            if (Rule.END_OF_FILE.equals(val)) {
                return Long.MAX_VALUE;
            }
            if (val != null) {
                return Long.parseLong(val, 16);
            }
        } catch (NumberFormatException e) {
            InvalidFormatException ife = InvalidFormatException.from(p, "Invalid hex string", val, Long.class);
            ife.initCause(e);
            throw ife;
        }
        return null;
    }
}
