
package io.github.datromtool.domain.datafile.logiqx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.domain.datafile.logiqx.enumerations.YesNo;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@JsonPropertyOrder({
        "header",
        "build", // cosmetic for JSON/YAML, XML attribute
        "debug", // cosmetic for JSON/YAML, XML attribute
        "games"
})
@With
@Value
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
@JacksonXmlRootElement(localName = "datafile")
public class Datafile {

    Header header;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "game")
    @JsonProperty(required = true)
    ImmutableList<Game> games = ImmutableList.of();

    @JacksonXmlProperty(isAttribute = true)
    String build;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(isAttribute = true)
    YesNo debug = YesNo.NO;

}
