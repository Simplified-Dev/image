package dev.sbs.api.io.image.exception;

import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thrown when image decoding or parsing fails.
 */
public class ImageDecodeException extends ImageException {

    /**
     * Constructs a new {@code ImageDecodeException} with the specified cause.
     *
     * @param cause the underlying throwable that caused this exception
     */
    public ImageDecodeException(@NotNull Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code ImageDecodeException} with the specified detail message.
     *
     * @param message the detail message
     */
    public ImageDecodeException(@NotNull String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ImageDecodeException} with the specified cause and detail message.
     *
     * @param cause the underlying throwable that caused this exception
     * @param message the detail message
     */
    public ImageDecodeException(@NotNull Throwable cause, @NotNull String message) {
        super(cause, message);
    }

    /**
     * Constructs a new {@code ImageDecodeException} with a formatted detail message.
     *
     * @param message the format string
     * @param args the format arguments
     */
    public ImageDecodeException(@NotNull @PrintFormat String message, @Nullable Object... args) {
        super(message, args);
    }

    /**
     * Constructs a new {@code ImageDecodeException} with the specified cause and a formatted detail message.
     *
     * @param cause the underlying throwable that caused this exception
     * @param message the format string
     * @param args the format arguments
     */
    public ImageDecodeException(@NotNull Throwable cause, @NotNull @PrintFormat String message, @Nullable Object... args) {
        super(cause, message, args);
    }

}
