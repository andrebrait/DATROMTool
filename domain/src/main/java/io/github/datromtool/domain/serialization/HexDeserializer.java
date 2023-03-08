package io.github.datromtool.domain.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.github.datromtool.domain.detector.Rule;

import java.io.IOException;

public final class HexDeserializer extends JsonDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            String val = p.getValueAsString();
            if (Rule.END_OF_FILE.equals(val)) {
                return Long.MAX_VALUE;
            }
            if (val != null) {
                return Long.parseLong(val, 16);
            }
        } catch (NumberFormatException e) {
            throw InvalidFormatException.from(p, "Invalid hex string", e);
        }
        return null;
    }
}
