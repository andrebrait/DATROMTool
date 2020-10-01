package io.github.datromtool.io;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum OutputMode {
    @JsonProperty("xml")
    XML,
    @JsonProperty("json")
    JSON,
    @JsonProperty("yaml")
    YAML
}
