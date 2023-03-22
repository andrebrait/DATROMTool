package io.github.datromtool.domain.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public final class SpacedHexArrayDeserializer extends JsonDeserializer<byte[]> {

    @Override
    public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            String valueAsString = p.getValueAsString();
            if (valueAsString != null) {
                return Hex.decodeHex(StringUtils.deleteWhitespace(valueAsString));
            }
        } catch (DecoderException e) {
            throw InvalidFormatException.from(p, "Invalid spaced hex string", e);
        }
        return null;
    }
}
