package dev.sbs.api.io.image.codec.webp;

import org.jetbrains.annotations.NotNull;

/**
 * Forward and inverse 4x4 Discrete Cosine Transform (DCT) and Walsh-Hadamard
 * Transform (WHT) for VP8 lossy codec.
 */
final class DCT {

    private DCT() { }

    /**
     * Applies forward 4x4 DCT to a block of pixel residuals.
     *
     * @param input 16-element residual block (row-major)
     * @param output 16-element output for DCT coefficients
     */
    static void forwardDCT(short @NotNull [] input, short @NotNull [] output) {
        int[] tmp = new int[16];

        // Horizontal pass
        for (int i = 0; i < 4; i++) {
            int a0 = input[i * 4] + input[i * 4 + 3];
            int a1 = input[i * 4 + 1] + input[i * 4 + 2];
            int a2 = input[i * 4 + 1] - input[i * 4 + 2];
            int a3 = input[i * 4] - input[i * 4 + 3];

            tmp[i * 4] = a0 + a1;
            tmp[i * 4 + 2] = a0 - a1;
            tmp[i * 4 + 1] = (a3 * 2217 + a2 * 5352 + 14500) >> 12;
            tmp[i * 4 + 3] = (a3 * 5352 - a2 * 2217 + 7500) >> 12;
        }

        // Vertical pass
        for (int i = 0; i < 4; i++) {
            int a0 = tmp[i] + tmp[12 + i];
            int a1 = tmp[4 + i] + tmp[8 + i];
            int a2 = tmp[4 + i] - tmp[8 + i];
            int a3 = tmp[i] - tmp[12 + i];

            output[i] = (short) ((a0 + a1 + 7) >> 4);
            output[8 + i] = (short) ((a0 - a1 + 7) >> 4);
            output[4 + i] = (short) ((a3 * 2217 + a2 * 5352 + 12000) >> 16);
            output[12 + i] = (short) ((a3 * 5352 - a2 * 2217 + 51000) >> 16);
        }
    }

    /**
     * Applies inverse 4x4 DCT to reconstruct pixel residuals.
     *
     * @param input 16-element DCT coefficients
     * @param output 16-element output for reconstructed residuals
     */
    static void inverseDCT(short @NotNull [] input, short @NotNull [] output) {
        int[] tmp = new int[16];

        // Horizontal pass
        for (int i = 0; i < 4; i++) {
            int a = input[i * 4] + input[i * 4 + 2];
            int b = input[i * 4] - input[i * 4 + 2];
            int c = ((input[i * 4 + 1] * 35468) >> 16) - ((input[i * 4 + 3] * 85627) >> 16);
            int d = ((input[i * 4 + 1] * 85627) >> 16) + ((input[i * 4 + 3] * 35468) >> 16);

            tmp[i * 4] = a + d;
            tmp[i * 4 + 1] = b + c;
            tmp[i * 4 + 2] = b - c;
            tmp[i * 4 + 3] = a - d;
        }

        // Vertical pass
        for (int i = 0; i < 4; i++) {
            int a = tmp[i] + tmp[8 + i];
            int b = tmp[i] - tmp[8 + i];
            int c = ((tmp[4 + i] * 35468) >> 16) - ((tmp[12 + i] * 85627) >> 16);
            int d = ((tmp[4 + i] * 85627) >> 16) + ((tmp[12 + i] * 35468) >> 16);

            output[i] = (short) ((a + d + 4) >> 3);
            output[4 + i] = (short) ((b + c + 4) >> 3);
            output[8 + i] = (short) ((b - c + 4) >> 3);
            output[12 + i] = (short) ((a - d + 4) >> 3);
        }
    }

    /**
     * Applies forward 4x4 Walsh-Hadamard Transform for luma DC coefficients.
     *
     * @param input 16-element DC values from each 4x4 sub-block
     * @param output 16-element WHT coefficients
     */
    static void forwardWHT(short @NotNull [] input, short @NotNull [] output) {
        int[] tmp = new int[16];

        for (int i = 0; i < 4; i++) {
            int a0 = input[i * 4] + input[i * 4 + 3];
            int a1 = input[i * 4 + 1] + input[i * 4 + 2];
            int a2 = input[i * 4 + 1] - input[i * 4 + 2];
            int a3 = input[i * 4] - input[i * 4 + 3];

            tmp[i * 4] = a0 + a1;
            tmp[i * 4 + 1] = a3 + a2;
            tmp[i * 4 + 2] = a0 - a1;
            tmp[i * 4 + 3] = a3 - a2;
        }

        for (int i = 0; i < 4; i++) {
            int a0 = tmp[i] + tmp[12 + i];
            int a1 = tmp[4 + i] + tmp[8 + i];
            int a2 = tmp[4 + i] - tmp[8 + i];
            int a3 = tmp[i] - tmp[12 + i];

            output[i] = (short) ((a0 + a1 + 3) >> 3);
            output[4 + i] = (short) ((a3 + a2 + 3) >> 3);
            output[8 + i] = (short) ((a0 - a1 + 3) >> 3);
            output[12 + i] = (short) ((a3 - a2 + 3) >> 3);
        }
    }

    /**
     * Applies inverse 4x4 Walsh-Hadamard Transform.
     *
     * @param input 16-element WHT coefficients
     * @param output 16-element DC values
     */
    static void inverseWHT(short @NotNull [] input, short @NotNull [] output) {
        int[] tmp = new int[16];

        for (int i = 0; i < 4; i++) {
            int a = input[i * 4] + input[i * 4 + 2];
            int b = input[i * 4] - input[i * 4 + 2];
            int c = input[i * 4 + 1] - input[i * 4 + 3];
            int d = input[i * 4 + 1] + input[i * 4 + 3];

            tmp[i * 4] = a + d;
            tmp[i * 4 + 1] = b + c;
            tmp[i * 4 + 2] = b - c;
            tmp[i * 4 + 3] = a - d;
        }

        for (int i = 0; i < 4; i++) {
            int a = tmp[i] + tmp[8 + i];
            int b = tmp[i] - tmp[8 + i];
            int c = tmp[4 + i] - tmp[12 + i];
            int d = tmp[4 + i] + tmp[12 + i];

            output[i] = (short) ((a + d + 3) >> 3);
            output[4 + i] = (short) ((b + c + 3) >> 3);
            output[8 + i] = (short) ((b - c + 3) >> 3);
            output[12 + i] = (short) ((a - d + 3) >> 3);
        }
    }

}
