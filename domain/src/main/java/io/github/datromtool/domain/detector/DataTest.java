
package io.github.datromtool.domain.detector;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@Value
@Jacksonized
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
@EqualsAndHashCode(callSuper = true)
@JacksonXmlRootElement(localName = "data")
public class DataTest extends BinaryTest {

    @Override
    public boolean test(byte[] bytes, int actualLength) {
        actualLength = Math.min(bytes.length, actualLength);
        byte[] value = getValueAsBytes();
        int offset = (int) getOffsetAsLong();
        if (offset < 0) {
            offset = actualLength + offset;
        }
        if (offset < 0 || actualLength - offset < value.length) {
            return false;
        }
        boolean matches = true;
        for (int i = 0; matches && i < value.length; i++) {
            matches = value[i] == bytes[i + offset];
        }
        return matches == getResult();
    }
}
