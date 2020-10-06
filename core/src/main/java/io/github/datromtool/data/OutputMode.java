package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum OutputMode {
    @JsonProperty("xml")
    XML,
    @JsonProperty("json")
    JSON,
    @JsonProperty("yaml")
    YAML
}
