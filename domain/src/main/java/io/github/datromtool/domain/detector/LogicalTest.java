
package io.github.datromtool.domain.detector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.datromtool.domain.detector.exception.TestException;
import io.github.datromtool.domain.detector.exception.TestWrappedException;
import lombok.AllArgsConstructor;
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
public abstract class LogicalTest extends BinaryTest {

    @NonNull
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(required = true)
    String mask;

    @JsonIgnore
    private byte[] getMaskAsBytes() {
        try {
            return Hex.decodeHex(getMask());
        } catch (DecoderException e) {
            throw new TestWrappedException(this, e);
        }
    }

    protected abstract byte operate(byte a, byte b);

    @Override
    public final boolean test(byte[] bytes, int actualLength) {
        actualLength = Math.min(bytes.length, actualLength);
        byte[] mask = getMaskAsBytes();
        byte[] value = getValueAsBytes();
        if (value.length != mask.length) {
            throw new TestException(this, "Mask and value lengths do not match");
        }
        if (value.length % 2 != 0) {
            throw new TestException(this, "Mask and value lengths are not multiples of 2");
        }
        int offset = (int) getOffsetAsLong();
        if (offset < 0) {
            offset = actualLength + offset;
        }
        if (offset < 0 || actualLength - offset < mask.length) {
            return false;
        }
        boolean matches = true;
        for (int i = 0; matches && i < mask.length; i++) {
            matches = operate(bytes[i], mask[i]) == value[i];
        }
        return matches == getResult();
    }

}
