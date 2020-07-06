package io.github.datromtool.domain.datafile.enumerations;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum YesNo {
    @JsonProperty("yes")
    YES,
    @JsonEnumDefaultValue
    @JsonProperty("no")
    NO
}
