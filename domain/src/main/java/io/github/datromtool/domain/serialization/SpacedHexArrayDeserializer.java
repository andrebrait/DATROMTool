package io.github.datromtool.domain.serialization;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Bytes;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

public final class SpacedHexArrayDeserializer extends ValueDeserializer<ImmutableList<Byte>> {

    @Override
    public ImmutableList<Byte> deserialize(JsonParser p, DeserializationContext ctxt) {
        String valueAsString = p.getValueAsString();
        try {
            if (valueAsString != null) {
                return ImmutableList.copyOf(
                        Bytes.asList(Hex.decodeHex(StringUtils.deleteWhitespace(valueAsString))));
            }
        } catch (DecoderException e) {
            InvalidFormatException ife = InvalidFormatException.from(p, "Invalid spaced hex string", valueAsString, ImmutableList.class);
            ife.initCause(e);
            throw ife;
        }
        return null;
    }
}
