package io.github.datromtool.domain.datafile.enumerations;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Status {
    @JsonProperty("baddump")
    BAD_DUMP,
    @JsonProperty("nodump")
    NO_DUMP,
    @JsonProperty("good")
    GOOD,
    @JsonProperty("verified")
    VERIFIED
}
