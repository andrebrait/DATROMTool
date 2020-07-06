package io.github.datromtool.domain.detector.enumerations;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ComparisonOperator {
    @JsonProperty("equal")
    EQUAL,
    @JsonProperty("less")
    LESS,
    @JsonProperty("greater")
    GREATER

}
