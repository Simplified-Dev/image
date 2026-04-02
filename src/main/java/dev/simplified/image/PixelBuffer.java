package dev.sbs.api.io.image;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * A zero-copy wrapper over {@code int[]} ARGB pixel data.
 * <p>
 * For {@link BufferedImage#TYPE_INT_ARGB} images, the backing array is referenced
 * directly without copying. For other image types, pixel data is extracted once
 * into a new array, then accessed without further copies.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PixelBuffer {

    private final int @NotNull [] pixels;
    private final int width;
    private final int height;

    /**
     * Wraps the pixel data of a {@link BufferedImage}.
     * <p>
     * If the image is {@link BufferedImage#TYPE_INT_ARGB}, the underlying data buffer
     * is referenced directly (zero copy). Otherwise, pixel data is extracted via
     * {@link BufferedImage#getRGB(int, int, int, int, int[], int, int)}.
     *
     * @param image the source image
     * @return a pixel buffer wrapping the image data
     */
    public static @NotNull PixelBuffer wrap(@NotNull BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            int[] data = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            return new PixelBuffer(data, w, h);
        }

        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
        return new PixelBuffer(pixels, w, h);
    }

    /**
     * Creates a pixel buffer from an existing ARGB pixel array.
     *
     * @param pixels the ARGB pixel data (not copied)
     * @param width the image width
     * @param height the image height
     * @return a pixel buffer wrapping the array
     */
    public static @NotNull PixelBuffer of(int @NotNull [] pixels, int width, int height) {
        return new PixelBuffer(pixels, width, height);
    }

    /**
     * Creates a new {@link BufferedImage} from this buffer's pixel data.
     * <p>
     * The returned image is {@link BufferedImage#TYPE_INT_ARGB} with the pixel array
     * set directly on its raster.
     *
     * @return a new buffered image containing this buffer's pixels
     */
    public @NotNull BufferedImage toBufferedImage() {
        BufferedImage image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, this.width, this.height, this.pixels, 0, this.width);
        return image;
    }

    /**
     * Returns the ARGB value at the given coordinates.
     *
     * @param x the column index
     * @param y the row index
     * @return the packed ARGB pixel value
     */
    public int getPixel(int x, int y) {
        return this.pixels[y * this.width + x];
    }

    /**
     * Sets the ARGB value at the given coordinates.
     *
     * @param x the column index
     * @param y the row index
     * @param argb the packed ARGB pixel value
     */
    public void setPixel(int x, int y, int argb) {
        this.pixels[y * this.width + x] = argb;
    }

}
