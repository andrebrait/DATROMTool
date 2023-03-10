package io.github.datromtool.domain.datafile.logiqx.enumerations;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ForceMerging {
    @JsonProperty("none")
    NONE,
    @JsonProperty("split")
    SPLIT,
    @JsonProperty("full")
    FULL
}
