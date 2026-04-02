package dev.sbs.api.io.image.codec.png;

import dev.sbs.api.io.image.ImageData;
import dev.sbs.api.io.image.ImageFormat;
import dev.sbs.api.io.image.StaticImageData;
import dev.sbs.api.io.image.codec.ImageReadOptions;
import dev.sbs.api.io.image.codec.ImageReader;
import dev.sbs.api.io.image.exception.ImageDecodeException;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * Reads PNG images via {@link ImageIO}.
 */
public class PngImageReader implements ImageReader {

    @Override
    public @NotNull ImageFormat getFormat() {
        return ImageFormat.PNG;
    }

    @Override
    public boolean canRead(byte @NotNull [] data) {
        return ImageFormat.PNG.matches(data);
    }

    @Override
    @SneakyThrows
    public @NotNull ImageData read(byte @NotNull [] data, @Nullable ImageReadOptions options) {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));

        if (image == null)
            throw new ImageDecodeException("Failed to decode PNG image");

        return StaticImageData.of(image);
    }

}
