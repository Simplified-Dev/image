package dev.sbs.api.io.image.codec.webp;

import org.jetbrains.annotations.NotNull;

/**
 * VP8 boolean arithmetic (range) encoder.
 * <p>
 * Encodes bits into a VP8 bitstream where each bit has an associated
 * probability. Dual to {@link BooleanDecoder}.
 */
final class BooleanEncoder {

    private byte[] data;
    private int bytePos;
    private int range;
    private long bottom;
    private int bitCount;

    /**
     * Creates a boolean encoder with the specified initial capacity.
     *
     * @param initialCapacity the initial byte array capacity
     */
    BooleanEncoder(int initialCapacity) {
        this.data = new byte[Math.max(16, initialCapacity)];
        this.bytePos = 0;
        this.range = 255;
        this.bottom = 0;
        this.bitCount = -24; // VP8 convention
    }

    /**
     * Encodes a single bit with the given probability.
     *
     * @param probability the probability of the bit being 0 (0-255)
     * @param bit 0 or 1
     */
    void encodeBit(int probability, int bit) {
        int split = 1 + (((range - 1) * probability) >> 8);

        if (bit != 0) {
            bottom += split;
            range -= split;
        } else {
            range = split;
        }

        // Renormalize
        int shift = normShift(range);
        range <<= shift;
        bitCount += shift;
        bottom <<= shift;

        if (bottom > 0xFFFFFFL) {
            // Carry propagation
            carry();
        }

        while (bitCount >= 0) {
            ensureCapacity(1);
            data[bytePos++] = (byte) ((bottom >> (24 + bitCount)) & 0xFF);
            bottom &= (1L << (24 + bitCount)) - 1;
            bitCount -= 8;
        }
    }

    /**
     * Encodes a boolean (equal probability).
     *
     * @param bit 0 or 1
     */
    void encodeBool(int bit) {
        encodeBit(128, bit);
    }

    /**
     * Encodes an unsigned integer of the given bit width.
     *
     * @param value the value to encode
     * @param bits the number of bits
     */
    void encodeUint(int value, int bits) {
        for (int i = bits - 1; i >= 0; i--)
            encodeBool((value >> i) & 1);
    }

    /**
     * Encodes a signed integer of the given bit width.
     *
     * @param value the signed value to encode
     * @param bits the number of bits (excluding sign)
     */
    void encodeSint(int value, int bits) {
        int abs = Math.abs(value);
        encodeUint(abs, bits);
        encodeBool(value < 0 ? 1 : 0);
    }

    /**
     * Flushes remaining state and returns the encoded bytes.
     *
     * @return the encoded byte array
     */
    byte @NotNull [] toByteArray() {
        // Flush remaining bits
        for (int i = 0; i < 32; i++)
            encodeBool(0);

        byte[] result = new byte[bytePos];
        System.arraycopy(data, 0, result, 0, bytePos);
        return result;
    }

    private void carry() {
        // Propagate carry through output
        int pos = bytePos - 1;
        while (pos >= 0 && data[pos] == (byte) 0xFF) {
            data[pos] = 0;
            pos--;
        }
        if (pos >= 0) data[pos]++;
        bottom &= 0xFFFFFFL;
    }

    private static int normShift(int range) {
        int shift = 0;
        while (range < 128) {
            range <<= 1;
            shift++;
        }
        return shift;
    }

    private void ensureCapacity(int additional) {
        if (bytePos + additional > data.length) {
            byte[] newData = new byte[Math.max(data.length * 2, bytePos + additional)];
            System.arraycopy(data, 0, newData, 0, bytePos);
            data = newData;
        }
    }

}
