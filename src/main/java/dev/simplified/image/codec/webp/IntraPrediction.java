package dev.sbs.api.io.image.codec.webp;

import org.jetbrains.annotations.NotNull;

/**
 * VP8 intra-frame spatial prediction for luma and chroma blocks.
 * <p>
 * Supports 4 luma 16x16 modes, 10 luma 4x4 sub-block modes,
 * and 4 chroma 8x8 modes.
 */
final class IntraPrediction {

    /** 16x16 luma prediction modes. */
    static final int DC_PRED = 0;
    static final int V_PRED = 1;
    static final int H_PRED = 2;
    static final int TM_PRED = 3;

    /** Number of 16x16 luma modes. */
    static final int NUM_16x16_MODES = 4;
    /** Number of 4x4 sub-block modes. */
    static final int NUM_4x4_MODES = 10;
    /** Number of 8x8 chroma modes. */
    static final int NUM_CHROMA_MODES = 4;

    private IntraPrediction() { }

    /**
     * Fills a 4x4 block with predicted values using the given mode.
     *
     * @param predicted output 16-element predicted block
     * @param above 4-element row of pixels above the block (null if top row)
     * @param left 4-element column of pixels to the left (null if left column)
     * @param aboveLeft the pixel above-left of the block (0 if unavailable)
     * @param mode the prediction mode (0-9 for 4x4, 0-3 for 16x16)
     */
    static void predict4x4(
        short @NotNull [] predicted,
        short[] above,
        short[] left,
        short aboveLeft,
        int mode
    ) {
        switch (mode) {
            case DC_PRED -> {
                int sum = 0;
                int count = 0;
                if (above != null) { for (short v : above) sum += v; count += 4; }
                if (left != null) { for (short v : left) sum += v; count += 4; }
                short dc = count > 0 ? (short) ((sum + count / 2) / count) : (short) 128;
                java.util.Arrays.fill(predicted, dc);
            }
            case V_PRED -> {
                if (above == null) { java.util.Arrays.fill(predicted, (short) 127); return; }
                for (int y = 0; y < 4; y++)
                    System.arraycopy(above, 0, predicted, y * 4, 4);
            }
            case H_PRED -> {
                if (left == null) { java.util.Arrays.fill(predicted, (short) 129); return; }
                for (int y = 0; y < 4; y++)
                    for (int x = 0; x < 4; x++)
                        predicted[y * 4 + x] = left[y];
            }
            case TM_PRED -> {
                for (int y = 0; y < 4; y++)
                    for (int x = 0; x < 4; x++) {
                        int a = above != null ? above[x] : 127;
                        int l = left != null ? left[y] : 129;
                        predicted[y * 4 + x] = (short) clamp(a + l - aboveLeft);
                    }
            }
            default -> java.util.Arrays.fill(predicted, (short) 128);
        }
    }

    /**
     * Computes the sum of squared differences between two blocks.
     *
     * @param original the original pixel block
     * @param predicted the predicted pixel block
     * @return the sum of squared differences
     */
    static long computeSSD(short @NotNull [] original, short @NotNull [] predicted) {
        long ssd = 0;
        for (int i = 0; i < original.length; i++) {
            int diff = original[i] - predicted[i];
            ssd += (long) diff * diff;
        }
        return ssd;
    }

    private static int clamp(int value) {
        return Math.clamp(value, 0, 255);
    }

}
