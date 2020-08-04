
package io.github.datromtool.domain.detector;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.datromtool.domain.detector.exception.TestException;
import io.github.datromtool.domain.detector.util.NumberUtils;
import io.github.datromtool.domain.serialization.HexArrayDeserializer;
import io.github.datromtool.domain.serialization.HexArraySerializer;
import lombok.AllArgsConstructor;
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
public abstract class LogicalTest extends BinaryTest {

    @NonNull
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(required = true)
    @JsonSerialize(using = HexArraySerializer.class)
    @JsonDeserialize(using = HexArrayDeserializer.class)
    byte[] mask;

    protected abstract byte operate(byte a, byte b);

    @Override
    public final boolean test(byte[] bytes, int actualLength, long fileSize) {
        actualLength = Math.min(bytes.length, actualLength);
        if (getValue().length != mask.length) {
            throw new TestException(this, "Mask and value lengths do not match");
        }
        int offset = NumberUtils.asInt(getOffset());
        if (offset < 0) {
            offset += actualLength;
        }
        if (offset < 0 || actualLength - offset < mask.length) {
            return false;
        }
        boolean matches = true;
        for (int i = 0; matches && i < mask.length; i++) {
            matches = operate(bytes[i], mask[i]) == getValue()[i];
        }
        return matches == getResult();
    }

}
