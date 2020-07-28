
package io.github.datromtool.domain.detector;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.datromtool.domain.serialization.HexArrayDeserializer;
import io.github.datromtool.domain.serialization.HexArraySerializer;
import io.github.datromtool.domain.serialization.HexDeserializer;
import io.github.datromtool.domain.serialization.HexSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;


@Data
@FieldDefaults(level = PRIVATE, makeFinal = true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor(access = PROTECTED)
@NoArgsConstructor(access = PROTECTED, force = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(NON_DEFAULT)
public abstract class BinaryTest extends Test {

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(defaultValue = "0")
    @JsonSerialize(using = HexSerializer.class)
    @JsonDeserialize(using = HexDeserializer.class)
    Long offset = 0L;

    @NonNull
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(required = true)
    @JsonSerialize(using = HexArraySerializer.class)
    @JsonDeserialize(using = HexArrayDeserializer.class)
    byte[] value;

}
