package dev.sbs.api.io.image.exception;

import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thrown when image encoding or writing fails.
 */
public class ImageEncodeException extends ImageException {

    /**
     * Constructs a new {@code ImageEncodeException} with the specified cause.
     *
     * @param cause the underlying throwable that caused this exception
     */
    public ImageEncodeException(@NotNull Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code ImageEncodeException} with the specified detail message.
     *
     * @param message the detail message
     */
    public ImageEncodeException(@NotNull String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ImageEncodeException} with the specified cause and detail message.
     *
     * @param cause the underlying throwable that caused this exception
     * @param message the detail message
     */
    public ImageEncodeException(@NotNull Throwable cause, @NotNull String message) {
        super(cause, message);
    }

    /**
     * Constructs a new {@code ImageEncodeException} with a formatted detail message.
     *
     * @param message the format string
     * @param args the format arguments
     */
    public ImageEncodeException(@NotNull @PrintFormat String message, @Nullable Object... args) {
        super(message, args);
    }

    /**
     * Constructs a new {@code ImageEncodeException} with the specified cause and a formatted detail message.
     *
     * @param cause the underlying throwable that caused this exception
     * @param message the format string
     * @param args the format arguments
     */
    public ImageEncodeException(@NotNull Throwable cause, @NotNull @PrintFormat String message, @Nullable Object... args) {
        super(cause, message, args);
    }

}
