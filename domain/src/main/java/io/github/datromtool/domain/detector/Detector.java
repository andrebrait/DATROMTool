
package io.github.datromtool.domain.detector;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;


@JsonPropertyOrder({
        "name",
        "author",
        "version",
        "rules"
})
@Value
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
@JacksonXmlRootElement(localName = "detector")
public class Detector {

    @NonNull
    @JsonProperty(required = true)
    String name;
    String author;
    String version;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "rule")
    @JsonProperty(required = true)
    ImmutableList<Rule> rules = ImmutableList.of();

}
