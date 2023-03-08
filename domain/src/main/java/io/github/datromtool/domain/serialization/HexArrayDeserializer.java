package io.github.datromtool.domain.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;

public final class HexArrayDeserializer extends JsonDeserializer<byte[]> {

    @Override
    public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            String valueAsString = p.getValueAsString();
            if (valueAsString != null) {
                return Hex.decodeHex(valueAsString);
            }
        } catch (DecoderException e) {
            throw InvalidFormatException.from(p, "Invalid hex string", e);
        }
        return null;
    }
}
