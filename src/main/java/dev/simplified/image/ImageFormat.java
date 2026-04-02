package dev.sbs.api.io.image;

import dev.sbs.api.io.image.exception.UnsupportedFormatException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Supported image formats with magic byte detection and animation capability metadata.
 */
@Getter
@RequiredArgsConstructor
public enum ImageFormat {

    JPEG("jpeg", false),
    PNG("png", false),
    BMP("bmp", false),
    GIF("gif", true),
    WEBP("webp", true);

    private final @NotNull String formatName;
    private final boolean supportsAnimation;

    /**
     * Detects the image format of the given byte array by examining magic bytes.
     *
     * @param data the raw image bytes to examine
     * @return the detected image format
     * @throws UnsupportedFormatException if no known format matches the data
     */
    public static @NotNull ImageFormat detect(byte @NotNull [] data) {
        return Arrays.stream(values())
            .filter(format -> format.matches(data))
            .findFirst()
            .orElseThrow(() -> new UnsupportedFormatException("Unable to detect image format from magic bytes"));
    }

    /**
     * Returns whether the given byte array matches this format's magic bytes.
     *
     * @param data the raw image bytes to check
     * @return true if the data starts with this format's magic bytes
     */
    public boolean matches(byte @NotNull [] data) {
        return switch (this) {
            case JPEG -> data.length >= 3
                && (data[0] & 0xFF) == 0xFF
                && (data[1] & 0xFF) == 0xD8
                && (data[2] & 0xFF) == 0xFF;
            case PNG -> data.length >= 4
                && (data[0] & 0xFF) == 0x89
                && data[1] == 0x50
                && data[2] == 0x4E
                && data[3] == 0x47;
            case BMP -> data.length >= 2
                && data[0] == 0x42
                && data[1] == 0x4D;
            case GIF -> data.length >= 3
                && data[0] == 0x47
                && data[1] == 0x49
                && data[2] == 0x46;
            case WEBP -> data.length >= 12
                && data[0] == 0x52   // R
                && data[1] == 0x49   // I
                && data[2] == 0x46   // F
                && data[3] == 0x46   // F
                && data[8] == 0x57   // W
                && data[9] == 0x45   // E
                && data[10] == 0x42  // B
                && data[11] == 0x50; // P
        };
    }

}
