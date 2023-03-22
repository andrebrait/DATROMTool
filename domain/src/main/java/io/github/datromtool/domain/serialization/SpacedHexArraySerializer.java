package io.github.datromtool.domain.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;

public final class SpacedHexArraySerializer extends JsonSerializer<byte[]> {

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
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
