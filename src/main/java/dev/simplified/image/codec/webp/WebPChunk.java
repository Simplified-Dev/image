package dev.simplified.image.codec.webp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A parsed RIFF chunk consisting of a type identifier and payload reference.
 * <p>
 * To minimize data copying, the payload is stored as an offset and length
 * into the original byte array rather than as a copied sub-array.
 */
@Getter
@RequiredArgsConstructor
class WebPChunk {

    private final @Nullable WebPChunkType type;
    private final @NotNull String fourCC;
    private final byte @NotNull [] source;
    private final int payloadOffset;
    private final int payloadLength;

    /**
     * Returns a copy of this chunk's payload bytes.
     *
     * @return a new byte array containing the payload
     */
    byte @NotNull [] getPayload() {
        byte[] payload = new byte[this.payloadLength];
        System.arraycopy(this.source, this.payloadOffset, payload, 0, this.payloadLength);
        return payload;
    }

}
