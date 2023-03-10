package io.github.datromtool.domain.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

public final class CommaSeparatedStringListSerializer extends JsonSerializer<List<String>> {

    private static final Joiner JOINER = Joiner.on(", ").skipNulls();

    @Override
    public void serialize(List<String> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(JOINER.join(value.stream().map(StringUtils::capitalize).iterator()));
        }
    }
}
