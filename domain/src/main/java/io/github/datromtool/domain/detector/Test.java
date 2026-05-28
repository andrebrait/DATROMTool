
package io.github.datromtool.domain.detector;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Data
@FieldDefaults(level = PRIVATE, makeFinal = true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor(access = PROTECTED)
@NoArgsConstructor(access = PROTECTED, force = true)
@JsonInclude(NON_DEFAULT)
public abstract class Test {

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(defaultValue = "true")
    Boolean result = Boolean.TRUE;

    public abstract boolean test(byte[] bytes, int actualLength, long fileSize);

}
