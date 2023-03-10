package io.github.datromtool.domain.datafile.logiqx.enumerations;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SampleMode {
    @JsonProperty("merged")
    MERGED,
    @JsonProperty("unmerged")
    UNMERGED

}
