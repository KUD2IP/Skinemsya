package skinemsya.vse.ru.integrations.infrastructure.ml;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/** Сжимает снимок чека в JPEG, чтобы vision-запрос не платил за лишние пиксели. */
public final class ReceiptImageEncoder {

    private ReceiptImageEncoder() {}

    public record EncodedImage(byte[] bytes, String mimeType) {}

    public static EncodedImage toJpeg(byte[] source, int maxSide) {
        if (source == null || source.length == 0) {
            return new EncodedImage(source == null ? new byte[0] : source, "image/jpeg");
        }
        BufferedImage decoded;
        try {
            decoded = ImageIO.read(new ByteArrayInputStream(source));
        } catch (IOException ex) {
            return new EncodedImage(source, "image/jpeg");
        }
        if (decoded == null) {
            return new EncodedImage(source, "image/jpeg");
        }

        int width = decoded.getWidth();
        int height = decoded.getHeight();
        double scale = Math.min(1.0, maxSide / (double) Math.max(width, height));
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        Image scaled = decoded.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage rgb = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.drawImage(scaled, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        try {
            return new EncodedImage(writeJpeg(rgb), "image/jpeg");
        } catch (IOException ex) {
            return new EncodedImage(source, "image/jpeg");
        }
    }

    private static byte[] writeJpeg(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("JPEG writer is not available");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.8f);
            }
            writer.write(null, new IIOImage(image, null, null), param);
            imageOutput.flush();
            return output.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
