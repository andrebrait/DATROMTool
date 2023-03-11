
package io.github.datromtool.domain.datafile.logiqx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.datromtool.domain.datafile.logiqx.enumerations.Status;
import io.github.datromtool.domain.datafile.logiqx.enumerations.YesNo;
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
@JacksonXmlRootElement(localName = "rom")
public class Rom {

    @NonNull
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(required = true)
    String name;

    /**
     * This is optional for parsing purposes only due to No-Intro using nodumps without settings a size
     * This should never be empty for real ROMs or anything we will output
     */
    @JacksonXmlProperty(isAttribute = true)
    Long size;

    @JacksonXmlProperty(isAttribute = true)
    String header;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(defaultValue = "no")
    YesNo mia = YesNo.NO;

    @JacksonXmlProperty(isAttribute = true)
    String crc;

    @JacksonXmlProperty(isAttribute = true)
    String md5;

    @JacksonXmlProperty(isAttribute = true)
    String sha1;

    @JacksonXmlProperty(isAttribute = true)
    String sha256;

    @JacksonXmlProperty(isAttribute = true)
    String merge;

    @JacksonXmlProperty(isAttribute = true)
    Status status;

    @JacksonXmlProperty(isAttribute = true)
    String date;

    @JacksonXmlProperty(isAttribute = true)
    String serial;
}
