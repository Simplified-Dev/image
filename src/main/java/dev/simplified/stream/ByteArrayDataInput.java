package dev.sbs.api.io.stream;

import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataInputStream;

/**
 * An extension of {@code DataInput} for reading from in-memory byte arrays.
 *
 * <p><b>Warning:</b> The caller is responsible for not attempting to read past the end of the
 * array. If any method encounters the end of the array prematurely, it throws {@link
 * IllegalStateException} to signify <i>programmer error</i>. This behavior is a technical violation
 * of the supertype's contract, which specifies a checked exception.
 */
public class ByteArrayDataInput extends DataInputStream implements DataInput {

    public ByteArrayDataInput(byte[] bytes) {
        this(new java.io.ByteArrayInputStream(bytes));
    }

    public ByteArrayDataInput(byte[] bytes, int start) {
        this(new java.io.ByteArrayInputStream(bytes, start, bytes.length - start));
    }

    public ByteArrayDataInput(@NotNull java.io.ByteArrayInputStream inputStream) {
        super(inputStream);
    }

}
