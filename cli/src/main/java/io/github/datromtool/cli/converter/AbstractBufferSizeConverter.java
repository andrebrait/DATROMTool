package io.github.datromtool.cli.converter;

import io.github.datromtool.ByteSize;
import picocli.CommandLine;

import static java.lang.Math.toIntExact;
import static java.lang.String.format;

/**
 * Shared parsing for the buffer-size converters: parses a {@link ByteSize} string and rejects
 * values over {@link Integer#MAX_VALUE} bytes before handing the byte count to the concrete
 * wrapper record, so the "Maximum byte size is %d bytes" message stays in one place.
 */
abstract class AbstractBufferSizeConverter<T> implements CommandLine.ITypeConverter<T> {

    private static final ByteSize MAX_BUFFER_SIZE = ByteSize.fromBytes(Integer.MAX_VALUE);

    @Override
    public final T convert(String value) {
        ByteSize byteSize = ByteSize.fromString(value);
        if (byteSize.compareTo(MAX_BUFFER_SIZE) > 0) {
            throw new IllegalArgumentException(format("Maximum byte size is %d bytes", Integer.MAX_VALUE));
        }
        return wrap(toIntExact(byteSize.getSizeInBytes()));
    }

    protected abstract T wrap(int bytes);
}
