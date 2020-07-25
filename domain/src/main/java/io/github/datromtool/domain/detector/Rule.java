
package io.github.datromtool.domain.detector;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.domain.detector.enumerations.BinaryOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@JsonPropertyOrder({
        "dataTests",
        "orTests",
        "xorTests",
        "andTests",
        "fileTests"
})
@Value
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
@JacksonXmlRootElement(localName = "rule")
public class Rule {

    public static final String END_OF_FILE = "EOF";

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "data")
    ImmutableList<DataTest> dataTests = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "or")
    ImmutableList<OrTest> orTests = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "xor")
    ImmutableList<XorTest> xorTests = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "and")
    ImmutableList<AndTest> andTests = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "file")
    ImmutableList<FileTest> fileTests = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "start_offset", isAttribute = true)
    @JsonProperty(defaultValue = "0")
    String startOffset = "0";

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "end_offset", isAttribute = true)
    @JsonProperty(defaultValue = END_OF_FILE)
    String endOffset = END_OF_FILE;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(defaultValue = "none")
    BinaryOperation operation = BinaryOperation.NONE;

}
