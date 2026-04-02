package dev.sbs.api.io.image.codec.jpeg;

import dev.sbs.api.io.image.ImageData;
import dev.sbs.api.io.image.ImageFormat;
import dev.sbs.api.io.image.codec.ImageWriteOptions;
import dev.sbs.api.io.image.codec.ImageWriter;
import dev.sbs.api.io.stream.ByteArrayDataOutput;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Writes JPEG images via {@link ImageIO} with configurable quality.
 */
public class JpegImageWriter implements ImageWriter {

    @Override
    public @NotNull ImageFormat getFormat() {
        return ImageFormat.JPEG;
    }

    @Override
    @SneakyThrows
    public byte @NotNull [] write(@NotNull ImageData data, @Nullable ImageWriteOptions options) {
        BufferedImage image = data.toBufferedImage();
        float quality = 0.75f;

        if (options instanceof JpegWriteOptions jpegOptions)
            quality = jpegOptions.getQuality();

        // JPEG does not support alpha - convert if needed
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

        javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        @Cleanup ByteArrayDataOutput dataOutput = new ByteArrayDataOutput();
        @Cleanup ImageOutputStream outputStream = ImageIO.createImageOutputStream(dataOutput);
        writer.setOutput(outputStream);

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();

        return dataOutput.toByteArray();
    }

}
