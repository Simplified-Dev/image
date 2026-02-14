package dev.sbs.api.io.stream;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * An extension of {@link java.io.DataOutputStream} for writing to in-memory byte arrays.
 */
public class ByteArrayDataOutput extends DataOutputStream {

    public ByteArrayDataOutput() {
        this(32);
    }

    public ByteArrayDataOutput(int size) {
        this(new java.io.ByteArrayOutputStream(size));
    }

    public ByteArrayDataOutput(@NotNull java.io.ByteArrayOutputStream outputStream) {
        super(outputStream);
    }

    private @NotNull java.io.ByteArrayOutputStream getUnderlying() {
        return (java.io.ByteArrayOutputStream) super.out;
    }

    public void reset() {
        this.getUnderlying().reset();
    }

    /**
     * Returns the contents that have been written to this instance, as a byte array.
     */
    public byte[] toByteArray() {
        return this.getUnderlying().toByteArray();
    }

    @Override
    public String toString() {
        return this.getUnderlying().toString();
    }

    @SneakyThrows
    public String toString(@NotNull String charsetName) {
        return this.getUnderlying().toString(charsetName);
    }

    public String toString(@NotNull Charset charset) {
        return this.getUnderlying().toString(charset);
    }

    public void writeImage(@NotNull RenderedImage renderedImage, @NotNull String formatName) throws IOException {
        ImageIO.write(renderedImage, formatName, this);
    }

    @SneakyThrows
    public void writeTo(@NotNull OutputStream outputStream) {
        this.getUnderlying().writeTo(outputStream);
    }

}