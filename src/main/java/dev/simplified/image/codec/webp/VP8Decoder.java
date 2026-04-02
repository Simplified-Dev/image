package dev.sbs.api.io.image.codec.webp;

import dev.sbs.api.io.image.PixelBuffer;
import dev.sbs.api.io.image.exception.ImageDecodeException;
import org.jetbrains.annotations.NotNull;

/**
 * Pure Java VP8 (WebP lossy) decoder.
 * <p>
 * Decodes a VP8 bitstream into ARGB pixel data using boolean arithmetic
 * decoding, intra-frame prediction, inverse DCT, and loop filtering.
 */
final class VP8Decoder {

    private VP8Decoder() { }

    /**
     * Decodes a VP8 bitstream into pixel data.
     *
     * @param data the raw VP8 payload
     * @return the decoded pixel buffer
     * @throws ImageDecodeException if decoding fails
     */
    static @NotNull PixelBuffer decode(byte @NotNull [] data) {
        if (data.length < 10)
            throw new ImageDecodeException("VP8 data too short");

        // Parse frame tag (3 bytes)
        int frameTag = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8) | ((data[2] & 0xFF) << 16);
        boolean keyFrame = (frameTag & 0x01) == 0;
        int firstPartSize = (frameTag >> 5) & 0x7FFFF;

        if (!keyFrame)
            throw new ImageDecodeException("VP8 inter-frames not supported (only key frames)");

        // Key frame header (7 bytes: 3 sync + 2 width + 2 height)
        int offset = 3;

        if (data.length < offset + 7)
            throw new ImageDecodeException("VP8 key frame header too short");

        // Sync code
        if (data[offset] != (byte) 0x9D || data[offset + 1] != (byte) 0x01 || data[offset + 2] != (byte) 0x2A)
            throw new ImageDecodeException("Invalid VP8 sync code");

        offset += 3;

        int widthField = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
        int heightField = (data[offset + 2] & 0xFF) | ((data[offset + 3] & 0xFF) << 8);
        int width = widthField & 0x3FFF;
        int height = heightField & 0x3FFF;

        offset += 4;

        if (width == 0 || height == 0)
            throw new ImageDecodeException("Invalid VP8 dimensions: %dx%d", width, height);

        // Decode first partition (header + macroblock modes)
        int partitionEnd = Math.min(offset + firstPartSize, data.length);
        BooleanDecoder headerDecoder = new BooleanDecoder(data, offset, partitionEnd - offset);

        // Colorspace and clamping (consumed but not used - VP8 always BT.601)
        headerDecoder.decodeBool();
        headerDecoder.decodeBool();

        // Skip segmentation
        if (headerDecoder.decodeBool() != 0) {
            int updateMap = headerDecoder.decodeBool();
            if (headerDecoder.decodeBool() != 0) {
                headerDecoder.decodeBool(); // abs or delta
                for (int i = 0; i < 4; i++)
                    if (headerDecoder.decodeBool() != 0) headerDecoder.decodeSint(7);
                for (int i = 0; i < 4; i++)
                    if (headerDecoder.decodeBool() != 0) headerDecoder.decodeSint(6);
            }
            if (updateMap != 0)
                for (int i = 0; i < 3; i++)
                    if (headerDecoder.decodeBool() != 0) headerDecoder.decodeUint(8);
        }

        // Filter parameters
        boolean simpleFilter = headerDecoder.decodeBool() == 0;
        int filterLevel = headerDecoder.decodeUint(6);
        int sharpness = headerDecoder.decodeUint(3);

        if (headerDecoder.decodeBool() != 0) { // filter adjust
            if (headerDecoder.decodeBool() != 0)
                for (int i = 0; i < 8; i++)
                    if (headerDecoder.decodeBool() != 0) headerDecoder.decodeSint(6);
        }

        // Token partitions
        int numTokenPartitions = 1 << headerDecoder.decodeUint(2);

        // Quantizer indices
        int yAcQi = headerDecoder.decodeUint(7);
        int yDcDelta = headerDecoder.decodeBool() != 0 ? headerDecoder.decodeSint(4) : 0;
        int y2DcDelta = headerDecoder.decodeBool() != 0 ? headerDecoder.decodeSint(4) : 0;
        int y2AcDelta = headerDecoder.decodeBool() != 0 ? headerDecoder.decodeSint(4) : 0;
        int uvDcDelta = headerDecoder.decodeBool() != 0 ? headerDecoder.decodeSint(4) : 0;
        int uvAcDelta = headerDecoder.decodeBool() != 0 ? headerDecoder.decodeSint(4) : 0;

        // Build dequantization steps from QI + deltas
        int yDcQ = lookupDc(Math.clamp(yAcQi + yDcDelta, 0, 127));
        int yAcQ = lookupAc(yAcQi);
        int uvDcQ = lookupDc(Math.clamp(yAcQi + uvDcDelta, 0, 127));
        int uvAcQ = lookupAc(Math.clamp(yAcQi + uvAcDelta, 0, 127));

        // Macroblock grid
        int mbCols = (width + 15) / 16;
        int mbRows = (height + 15) / 16;
        int lumaWidth = mbCols * 16;
        int chromaWidth = mbCols * 8;

        // Reconstructed planes
        short[] reconLuma = new short[lumaWidth * mbRows * 16];
        short[] reconCb = new short[chromaWidth * mbRows * 8];
        short[] reconCr = new short[chromaWidth * mbRows * 8];

        // Token partition(s) start after header partition
        int tokenOffset = partitionEnd;
        // Skip partition size fields for multi-partition (3 bytes each for partitions 1..N-1)
        if (numTokenPartitions > 1)
            tokenOffset += (numTokenPartitions - 1) * 3;

        BooleanDecoder tokenDecoder = tokenOffset < data.length
            ? new BooleanDecoder(data, tokenOffset, data.length - tokenOffset)
            : null;

        // Decode each macroblock
        for (int mbY = 0; mbY < mbRows; mbY++) {
            for (int mbX = 0; mbX < mbCols; mbX++) {
                // Read prediction mode from header partition
                int yMode = headerDecoder.decodeUint(2);
                int uvMode = headerDecoder.decodeUint(2);

                // Get neighbor samples
                short[] aboveY = mbY > 0 ? extractRow(reconLuma, mbX * 16, (mbY * 16 - 1) * lumaWidth, 16) : null;
                short[] leftY = mbX > 0 ? extractCol(reconLuma, mbX * 16 - 1, mbY * 16, lumaWidth, 16) : null;
                short aboveLeftY = (mbX > 0 && mbY > 0) ? reconLuma[(mbY * 16 - 1) * lumaWidth + mbX * 16 - 1] : (short) 128;

                // Predict + decode residual for luma 4x4 sub-blocks
                for (int by = 0; by < 4; by++) {
                    for (int bx = 0; bx < 4; bx++) {
                        short[] predicted = new short[16];
                        short[] above4 = getAbove4(reconLuma, mbX * 16 + bx * 4, mbY * 16 + by * 4, lumaWidth);
                        short[] left4 = getLeft4(reconLuma, mbX * 16 + bx * 4, mbY * 16 + by * 4, lumaWidth);
                        short al = getAboveLeft(reconLuma, mbX * 16 + bx * 4, mbY * 16 + by * 4, lumaWidth);

                        IntraPrediction.predict4x4(predicted, above4, left4, al, mapTo4x4Mode(yMode));

                        short[] coefficients = new short[16];
                        if (tokenDecoder != null)
                            for (int c = 0; c < 16; c++)
                                coefficients[c] = (short) tokenDecoder.decodeSint(11);

                        // Dequantize
                        coefficients[0] = (short) (coefficients[0] * yDcQ);
                        for (int c = 1; c < 16; c++)
                            coefficients[c] = (short) (coefficients[c] * yAcQ);

                        // Inverse DCT
                        short[] residual = new short[16];
                        DCT.inverseDCT(coefficients, residual);

                        // Reconstruct and store
                        int baseX = mbX * 16 + bx * 4;
                        int baseY = mbY * 16 + by * 4;
                        for (int y = 0; y < 4; y++)
                            for (int x = 0; x < 4; x++)
                                reconLuma[(baseY + y) * lumaWidth + baseX + x] =
                                    (short) Math.clamp(predicted[y * 4 + x] + residual[y * 4 + x], 0, 255);
                    }
                }

                // Predict + decode residual for chroma 4x4 sub-blocks
                decodeChromaPlane(reconCb, mbX, mbY, chromaWidth, uvMode, tokenDecoder, uvDcQ, uvAcQ);
                decodeChromaPlane(reconCr, mbX, mbY, chromaWidth, uvMode, tokenDecoder, uvDcQ, uvAcQ);
            }
        }

        // Loop filter
        if (filterLevel > 0)
            LoopFilter.filterSimple(reconLuma, lumaWidth, mbRows * 16, filterLevel, sharpness);

        // Convert YCbCr to ARGB
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int yVal = reconLuma[y * lumaWidth + x] - 16;
                int cbVal = reconCb[(y / 2) * chromaWidth + (x / 2)] - 128;
                int crVal = reconCr[(y / 2) * chromaWidth + (x / 2)] - 128;

                int r = Math.clamp((298 * yVal + 409 * crVal + 128) >> 8, 0, 255);
                int g = Math.clamp((298 * yVal - 100 * cbVal - 208 * crVal + 128) >> 8, 0, 255);
                int b = Math.clamp((298 * yVal + 516 * cbVal + 128) >> 8, 0, 255);

                pixels[y * width + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }

        return PixelBuffer.of(pixels, width, height);
    }

    private static void decodeChromaPlane(
        short @NotNull [] recon, int mbX, int mbY, int chromaWidth,
        int uvMode, BooleanDecoder tokenDecoder, int dcQ, int acQ
    ) {
        for (int by = 0; by < 2; by++) {
            for (int bx = 0; bx < 2; bx++) {
                short[] predicted = new short[16];
                short[] above4 = getAbove4(recon, mbX * 8 + bx * 4, mbY * 8 + by * 4, chromaWidth);
                short[] left4 = getLeft4(recon, mbX * 8 + bx * 4, mbY * 8 + by * 4, chromaWidth);
                short al = getAboveLeft(recon, mbX * 8 + bx * 4, mbY * 8 + by * 4, chromaWidth);

                IntraPrediction.predict4x4(predicted, above4, left4, al, mapTo4x4Mode(uvMode));

                short[] coefficients = new short[16];
                if (tokenDecoder != null)
                    for (int c = 0; c < 16; c++)
                        coefficients[c] = (short) tokenDecoder.decodeSint(11);

                coefficients[0] = (short) (coefficients[0] * dcQ);
                for (int c = 1; c < 16; c++)
                    coefficients[c] = (short) (coefficients[c] * acQ);

                short[] residual = new short[16];
                DCT.inverseDCT(coefficients, residual);

                int baseX = mbX * 8 + bx * 4;
                int baseY = mbY * 8 + by * 4;
                for (int y = 0; y < 4; y++)
                    for (int x = 0; x < 4; x++)
                        recon[(baseY + y) * chromaWidth + baseX + x] =
                            (short) Math.clamp(predicted[y * 4 + x] + residual[y * 4 + x], 0, 255);
            }
        }
    }

    private static int mapTo4x4Mode(int mode16x16) {
        // Map 16x16 modes to compatible 4x4 modes (DC, V, H, TM are the same indices)
        return Math.clamp(mode16x16, 0, 3);
    }

    private static short[] extractRow(short[] plane, int startX, int rowOffset, int count) {
        short[] row = new short[count];
        System.arraycopy(plane, rowOffset + startX, row, 0, count);
        return row;
    }

    private static short[] extractCol(short[] plane, int x, int startY, int stride, int count) {
        short[] col = new short[count];
        for (int i = 0; i < count; i++)
            col[i] = plane[(startY + i) * stride + x];
        return col;
    }

    private static short[] getAbove4(short[] plane, int blockX, int blockY, int stride) {
        if (blockY == 0) return null;
        short[] above = new short[4];
        int rowOffset = (blockY - 1) * stride + blockX;
        if (rowOffset + 4 <= plane.length)
            System.arraycopy(plane, rowOffset, above, 0, 4);
        return above;
    }

    private static short[] getLeft4(short[] plane, int blockX, int blockY, int stride) {
        if (blockX == 0) return null;
        short[] left = new short[4];
        for (int i = 0; i < 4; i++) {
            int idx = (blockY + i) * stride + blockX - 1;
            if (idx >= 0 && idx < plane.length) left[i] = plane[idx];
        }
        return left;
    }

    private static short getAboveLeft(short[] plane, int blockX, int blockY, int stride) {
        if (blockX == 0 || blockY == 0) return 128;
        int idx = (blockY - 1) * stride + blockX - 1;
        return (idx >= 0 && idx < plane.length) ? plane[idx] : 128;
    }

    private static int lookupDc(int qi) {
        return Math.max(1, (qi < 8) ? qi + 2 : (qi < 25) ? (qi * 2 - 4) : (qi * 3 - 26));
    }

    private static int lookupAc(int qi) {
        return Math.max(1, (qi < 4) ? qi + 2 : (qi < 24) ? (qi + qi / 2) : (qi * 2 - 16));
    }

}
