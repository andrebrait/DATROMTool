
package io.github.datromtool.domain.datafile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;


@Value
@Jacksonized
@Builder(toBuilder = true)
@JacksonXmlRootElement(localName = "archive")
public class Archive {

    @NonNull
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(required = true)
    String name;

}
