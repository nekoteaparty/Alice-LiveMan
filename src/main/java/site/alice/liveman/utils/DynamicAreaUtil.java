package site.alice.liveman.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class DynamicAreaUtil {

    public static File createAreaImage(String context, int width, int height) throws IOException {
        File imageFile = new File("dynamicImage.bmp");
        BufferedImage image = new BufferedImage(width, height, TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setFont(new Font("YaHei Consolas Hybrid", Font.PLAIN, 28));
        g.setBackground(Color.BLACK);
        int x = 0;
        int y = 0;
        for (String line : context.split("\n")) {
            g.drawString(line, x, y += g.getFontMetrics().getHeight());
        }
        ImageIO.write(image, "bmp", imageFile);
        return imageFile;
    }

}
