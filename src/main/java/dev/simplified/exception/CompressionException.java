package dev.sbs.api.io.exception;

import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompressionException extends RuntimeException {

    public CompressionException(@NotNull Throwable cause) {
        super(cause);
    }

    public CompressionException(@NotNull String message) {
        super(message);
    }

    public CompressionException(@NotNull Throwable cause, @NotNull String message) {
        super(message, cause);
    }

    public CompressionException(@NotNull @PrintFormat String message, @Nullable Object... args) {
        super(String.format(message, args));
    }

    public CompressionException(@NotNull Throwable cause, @NotNull @PrintFormat String message, @Nullable Object... args) {
        super(String.format(message, args), cause);
    }

}
