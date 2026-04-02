package dev.sbs.api.io.image.codec.bmp;

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
 * Reads BMP images via {@link ImageIO}.
 */
public class BmpImageReader implements ImageReader {

    @Override
    public @NotNull ImageFormat getFormat() {
        return ImageFormat.BMP;
    }

    @Override
    public boolean canRead(byte @NotNull [] data) {
        return ImageFormat.BMP.matches(data);
    }

    @Override
    @SneakyThrows
    public @NotNull ImageData read(byte @NotNull [] data, @Nullable ImageReadOptions options) {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));

        if (image == null)
            throw new ImageDecodeException("Failed to decode BMP image");

        return StaticImageData.of(image);
    }

}
