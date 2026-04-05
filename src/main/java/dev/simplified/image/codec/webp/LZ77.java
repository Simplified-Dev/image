package dev.simplified.image.codec.webp;

import org.jetbrains.annotations.NotNull;

/**
 * LZ77 backward reference encoding and decoding for VP8L.
 * <p>
 * Uses hash-chain based matching for the encoder with a configurable
 * maximum distance window. The decoder interprets length-distance pairs
 * and copies pixel data from the already-decoded buffer.
 */
final class LZ77 {

    /** VP8L length code prefix table (RFC 6386 / VP8L spec Table 1). */
    static final int[] LENGTH_CODES = {
         1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 13, 15, 17, 19, 23,
        27, 31, 35, 43, 51, 59, 67, 83, 99,115,131,163,195,227,258
    };

    /** Extra bits for each length code. */
    static final int[] LENGTH_EXTRA_BITS = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2,
        2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 0
    };

    /** VP8L distance code prefix table. */
    static final int[] DISTANCE_CODES = {
         1,  2,  3,  4,  5,  7,  9, 13, 17, 25, 33, 49, 65, 97,129,193,
       257,385,513,769,1025,1537,2049,3073,4097,6145,8193,12289,16385,24577,32769
    };

    /** Extra bits for each distance code. */
    static final int[] DISTANCE_EXTRA_BITS = {
        0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
        7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 0
    };

    private static final int HASH_BITS = 16;
    private static final int HASH_SIZE = 1 << HASH_BITS;
    private static final int HASH_MASK = HASH_SIZE - 1;
    private static final int MAX_CHAIN_LENGTH = 64;
    private static final int MIN_MATCH = 3;

    private LZ77() { }

    /**
     * Decodes a length from a VP8L length symbol and extra bits.
     *
     * @param code the length code index (0-23, after subtracting 256 from the green symbol)
     * @param reader the bit reader for extra bits
     * @return the decoded length
     */
    static int decodeLength(int code, @NotNull BitReader reader) {
        if (code < LENGTH_CODES.length) {
            int base = LENGTH_CODES[code];
            int extraBits = code < LENGTH_EXTRA_BITS.length ? LENGTH_EXTRA_BITS[code] : 0;
            return base + (extraBits > 0 ? reader.readBits(extraBits) : 0);
        }

        return 1;
    }

    /**
     * Decodes a 2D distance from a VP8L distance code, applying the
     * VP8L distance mapping for image-width-aware backward references.
     *
     * @param distCode the raw distance code from the Huffman symbol
     * @param reader the bit reader for extra bits
     * @param imageWidth the image width (used for 2D distance mapping)
     * @return the decoded linear distance
     */
    static int decodeDistance(int distCode, @NotNull BitReader reader, int imageWidth) {
        // VP8L uses special 2D distance codes for the first 120 codes
        if (distCode < 120)
            return planeCodeToDistance(distCode, imageWidth);

        int code = distCode - 120;
        if (code < DISTANCE_CODES.length) {
            int base = DISTANCE_CODES[code];
            int extraBits = code < DISTANCE_EXTRA_BITS.length ? DISTANCE_EXTRA_BITS[code] : 0;
            return base + (extraBits > 0 ? reader.readBits(extraBits) : 0);
        }

        return 1;
    }

    /**
     * Converts a VP8L 2D plane code to a linear distance.
     * <p>
     * The first 120 distance codes map to 2D offsets relative to the
     * current pixel position, enabling efficient backward references
     * to pixels in nearby rows.
     *
     * @param code the 2D distance code (0-119)
     * @param xSize the image width
     * @return the linear distance in pixels
     */
    static int planeCodeToDistance(int code, int xSize) {
        // Lookup table: (dy, dx) pairs for codes 0-119
        // Based on VP8L spec Section 4.1
        int dy = code / 8;
        int dx = code % 8;

        // Map to signed deltas
        int[][] offsets = {
            {0, 1}, {1, 0}, {1, 1}, {-1, 1},
            {0, 2}, {2, 0}, {1, 2}, {-1, 2}
        };

        if (code < 8) {
            int dxOff = offsets[code][1];
            int dyOff = offsets[code][0];
            int dist = dyOff * xSize + dxOff;
            return Math.max(1, dist);
        }

        // For codes >= 8, use formula from spec
        dy = (code >> 3);
        dx = code & 7;

        int signedDx;
        if (dx < 4)
            signedDx = dx + 1;
        else
            signedDx = -(dx - 3);

        int dist = dy * xSize + signedDx;
        return Math.max(1, dist);
    }

    /**
     * Finds the best backward reference match at the current position
     * using hash-chain matching.
     *
     * @param pixels the pixel array (ARGB values)
     * @param pos the current position
     * @param maxLen the maximum match length
     * @param hashHead the hash chain head table
     * @param hashPrev the hash chain previous-link table
     * @return the match as {length, distance}, or {0, 0} if no match
     */
    static int @NotNull [] findMatch(int @NotNull [] pixels, int pos, int maxLen, int @NotNull [] hashHead, int @NotNull [] hashPrev) {
        if (pos == 0 || maxLen < MIN_MATCH) return new int[]{0, 0};

        int hash = hashPixel(pixels, pos);
        int bestLen = 0;
        int bestDist = 0;
        int chainLen = 0;
        int candidate = hashHead[hash];

        while (candidate >= 0 && chainLen < MAX_CHAIN_LENGTH) {
            int dist = pos - candidate;
            if (dist > 0) {
                int matchLen = matchLength(pixels, candidate, pos, Math.min(maxLen, pixels.length - pos));
                if (matchLen > bestLen) {
                    bestLen = matchLen;
                    bestDist = dist;
                    if (matchLen >= maxLen) break;
                }
            }
            candidate = hashPrev[candidate];
            chainLen++;
        }

        if (bestLen < MIN_MATCH) return new int[]{0, 0};

        return new int[]{bestLen, bestDist};
    }

    /**
     * Updates the hash chain with the pixel at the given position.
     *
     * @param pixels the pixel array
     * @param pos the position to insert
     * @param hashHead the hash chain head table
     * @param hashPrev the hash chain previous-link table
     */
    static void updateHash(int @NotNull [] pixels, int pos, int @NotNull [] hashHead, int @NotNull [] hashPrev) {
        if (pos >= pixels.length) return;

        int hash = hashPixel(pixels, pos);
        hashPrev[pos] = hashHead[hash];
        hashHead[hash] = pos;
    }

    /**
     * Creates a new hash head table initialized to -1.
     * <p>
     * Indexed by hash value, maps to the most recent pixel position with that hash.
     *
     * @return the hash head table
     */
    static int @NotNull [] newHashHead() {
        int[] table = new int[HASH_SIZE];
        java.util.Arrays.fill(table, -1);
        return table;
    }

    /**
     * Creates a new hash previous-link table initialized to -1.
     * <p>
     * Indexed by pixel position, maps to the previous position in the same
     * hash chain. Sized to the pixel array length for collision-free chaining.
     *
     * @param pixelCount the total number of pixels in the image
     * @return the hash prev table
     */
    static int @NotNull [] newHashPrev(int pixelCount) {
        int[] table = new int[pixelCount];
        java.util.Arrays.fill(table, -1);
        return table;
    }

    private static int hashPixel(int[] pixels, int pos) {
        return (pixels[pos] * 0x1E35A7BD) >>> (32 - HASH_BITS);
    }

    private static int matchLength(int[] pixels, int a, int b, int maxLen) {
        int len = 0;
        while (len < maxLen && pixels[a + len] == pixels[b + len])
            len++;
        return len;
    }

}
