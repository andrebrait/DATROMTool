package io.github.datromtool.domain.serialization;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.List;

public final class CommaSeparatedStringListSerializer extends ValueSerializer<List<String>> {

    private static final Joiner JOINER = Joiner.on(", ").skipNulls();

    @Override
    public void serialize(List<String> value, JsonGenerator gen, SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(JOINER.join(value.stream().map(StringUtils::capitalize).iterator()));
        }
    }
}
