package dev.sbs.api.io.image.codec.bmp;

import dev.sbs.api.io.image.ImageData;
import dev.sbs.api.io.image.ImageFormat;
import dev.sbs.api.io.image.codec.ImageWriteOptions;
import dev.sbs.api.io.image.codec.ImageWriter;
import dev.sbs.api.io.stream.ByteArrayDataOutput;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Writes BMP images via {@link ImageIO}.
 */
public class BmpImageWriter implements ImageWriter {

    @Override
    public @NotNull ImageFormat getFormat() {
        return ImageFormat.BMP;
    }

    @Override
    @SneakyThrows
    public byte @NotNull [] write(@NotNull ImageData data, @Nullable ImageWriteOptions options) {
        BufferedImage image = data.toBufferedImage();

        // BMP does not support alpha - convert if needed
        if (image.getColorModel().hasAlpha()) {
            BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = rgb.createGraphics();
            try {
                g2d.drawImage(image, 0, 0, null);
            } finally {
                g2d.dispose();
            }
            image = rgb;
        }

        @Cleanup ByteArrayDataOutput dataOutput = new ByteArrayDataOutput();
        ImageIO.write(image, "bmp", dataOutput);
        return dataOutput.toByteArray();
    }

}
