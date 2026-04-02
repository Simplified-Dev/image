package dev.sbs.api.io.image.codec.webp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * WebP RIFF chunk types identified by their four-character code (FourCC).
 */
@Getter
@RequiredArgsConstructor
enum WebPChunkType {

    VP8("VP8 "),
    VP8L("VP8L"),
    VP8X("VP8X"),
    ANIM("ANIM"),
    ANMF("ANMF"),
    ALPH("ALPH"),
    ICCP("ICCP"),
    EXIF("EXIF"),
    XMP("XMP ");

    private final @NotNull String fourCC;

    /**
     * Returns the chunk type matching the given FourCC string.
     *
     * @param fourCC the four-character code
     * @return the matching chunk type, or null if unrecognized
     */
    static @Nullable WebPChunkType of(@NotNull String fourCC) {
        return Arrays.stream(values())
            .filter(type -> type.getFourCC().equals(fourCC))
            .findFirst()
            .orElse(null);
    }

}
