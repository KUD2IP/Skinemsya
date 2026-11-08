package skinemsya.vse.ru.integrations.infrastructure.ml;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ReceiptImageEncoderTest {

    @Test
    void shrinksLongSideToLimit() throws Exception {
        BufferedImage image = new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 2000, 1000);
        graphics.dispose();
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);

        ReceiptImageEncoder.EncodedImage encoded = ReceiptImageEncoder.toJpeg(output.toByteArray(), 1600);
        BufferedImage decoded = ImageIO.read(new java.io.ByteArrayInputStream(encoded.bytes()));

        assertThat(encoded.mimeType()).isEqualTo("image/jpeg");
        assertThat(decoded.getWidth()).isLessThanOrEqualTo(1600);
        assertThat(decoded.getHeight()).isLessThanOrEqualTo(1600);
        assertThat(Math.max(decoded.getWidth(), decoded.getHeight())).isEqualTo(1600);
    }
}
