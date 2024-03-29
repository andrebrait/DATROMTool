
package io.github.datromtool.domain.datafile.logiqx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.datromtool.domain.datafile.logiqx.enumerations.Status;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@Value
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
@JacksonXmlRootElement(localName = "disk")
public class Disk {

    @NonNull
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(required = true)
    String name;

    @JacksonXmlProperty(isAttribute = true)
    String sha1;

    @JacksonXmlProperty(isAttribute = true)
    String sha256;

    @JacksonXmlProperty(isAttribute = true)
    String md5;

    @JacksonXmlProperty(isAttribute = true)
    String merge;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(defaultValue = "good")
    Status status = Status.GOOD;

}
