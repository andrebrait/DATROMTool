package io.github.datromtool.domain.serialization;

import org.apache.commons.codec.binary.Hex;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public final class SpacedHexArraySerializer extends ValueSerializer<byte[]> {

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
        } else {
            String hexString = Hex.encodeHexString(value).toUpperCase();
            if (hexString.length() == 2) {
                gen.writeString(hexString);
            } else {
                StringBuilder output = new StringBuilder(hexString.length() + hexString.length() / 2 - 1);
                for (int i = 0; i < hexString.length() - 2; i += 2) {
                    if (i > 0) {
                        output.append(' ');
                    }
                    output.append(hexString.charAt(i));
                    output.append(hexString.charAt(i + 1));
                }
                gen.writeString(output.toString());
            }
        }
    }
}
