
package io.github.datromtool.domain.datafile.logiqx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.github.datromtool.domain.datafile.logiqx.enumerations.YesNo;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@Value
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
@JsonRootName("biosset")
public class BiosSet {

    @NonNull
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(required = true)
    String name;

    @NonNull
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(required = true)
    String description;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "default", isAttribute = true)
    @JsonProperty(value = "default", defaultValue = "no")
    YesNo isDefault = YesNo.NO;

}

