
package io.github.datromtool.domain.datafile.logiqx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@JsonPropertyOrder({
        "name",
        "description",
        "category",
        "version",
        "date",
        "author",
        "email",
        "homepage",
        "url",
        "comment",
        "clrmamepro",
        "romCenter"
})
@Value
@With
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
@JsonRootName("header")
public class Header {

    @NonNull
    @JsonProperty(required = true)
    String name;

    @NonNull
    @JsonProperty(required = true)
    String description;
    String category;

    @NonNull
    @JsonProperty(required = true)
    String version;
    String date;

    @NonNull
    @JsonProperty(required = true)
    String author;
    String email;
    String homepage;
    String url;
    String comment;
    Clrmamepro clrmamepro;

    @JacksonXmlProperty(localName = "romcenter")
    RomCenter romCenter;

}
