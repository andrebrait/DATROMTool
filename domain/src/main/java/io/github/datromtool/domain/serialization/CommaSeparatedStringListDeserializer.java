package io.github.datromtool.domain.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.base.Splitter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class CommaSeparatedStringListDeserializer extends JsonDeserializer<List<String>> {

    private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String valueAsString = p.getValueAsString();
        if (valueAsString != null) {
            return StreamSupport.stream(SPLITTER.split(valueAsString).spliterator(), false)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        }
        return null;
    }
}
