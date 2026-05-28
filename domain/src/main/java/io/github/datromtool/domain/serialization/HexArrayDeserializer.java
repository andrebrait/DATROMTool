package io.github.datromtool.domain.serialization;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

public final class HexArrayDeserializer extends ValueDeserializer<byte[]> {

    @Override
    public byte[] deserialize(JsonParser p, DeserializationContext ctxt) {
        String valueAsString = p.getValueAsString();
        try {
            if (valueAsString != null) {
                return Hex.decodeHex(valueAsString);
            }
        } catch (DecoderException e) {
            InvalidFormatException ife = InvalidFormatException.from(p, "Invalid hex string", valueAsString, byte[].class);
            ife.initCause(e);
            throw ife;
        }
        return null;
    }
}
