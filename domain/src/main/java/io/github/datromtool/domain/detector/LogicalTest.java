
package io.github.datromtool.domain.detector;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.datromtool.domain.detector.exception.TestException;
import io.github.datromtool.domain.detector.util.NumberUtils;
import io.github.datromtool.domain.serialization.HexArrayDeserializer;
import io.github.datromtool.domain.serialization.HexArraySerializer;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

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

    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(required = true)
    @JsonSerialize(using = HexArraySerializer.class)
    @JsonDeserialize(using = HexArrayDeserializer.class)
    byte @NonNull [] mask;

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
