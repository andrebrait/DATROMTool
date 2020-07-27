
package io.github.datromtool.domain.detector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.datromtool.domain.detector.exception.TestWrappedException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

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
    String offset = "0";

    @NonNull
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(required = true)
    String value;

    @JsonIgnore
    public long getOffsetAsLong() {
        return Long.parseLong(getOffset(), 16);
    }

    @JsonIgnore
    public byte[] getValueAsBytes() {
        try {
            return Hex.decodeHex(getValue());
        } catch (DecoderException e) {
            throw new TestWrappedException(this, e);
        }
    }

}
