package io.github.datromtool.domain.serialization;

import com.google.common.base.Splitter;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class CommaSeparatedStringListDeserializer extends ValueDeserializer<List<String>> {

    private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) {
        String valueAsString = p.getValueAsString();
        if (valueAsString != null) {
            return StreamSupport.stream(SPLITTER.split(valueAsString).spliterator(), false)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        }
        return null;
    }
}
