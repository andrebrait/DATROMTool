
package io.github.datromtool.domain.datafile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

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
@JacksonXmlRootElement(localName = "header")
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
