package io.github.datromtool.domain.datafile.enumerations;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ForcePacking {
    @JsonProperty("zip")
    ZIP,
    @JsonProperty("unzip")
    UNZIP
}
