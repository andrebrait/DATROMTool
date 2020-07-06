package io.github.datromtool.domain.detector.enumerations;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum BinaryOperation {
    @JsonProperty("none")
    NONE,
    @JsonProperty("bitswap")
    BIT_SWAP,
    @JsonProperty("byteswap")
    BYTE_SWAP,
    @JsonProperty("wordswap")
    WORD_SWAP,
    @JsonProperty("wordbyteswap")
    WORD_BYTE_SWAP

}
