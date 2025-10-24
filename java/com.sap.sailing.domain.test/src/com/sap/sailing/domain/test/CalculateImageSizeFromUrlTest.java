package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.impl.ImageSizeImpl;
import com.sap.sse.common.media.ImageSize;

public class CalculateImageSizeFromUrlTest {

    @Test
    public void calculateImageSizeFromUrl() throws IOException {
        int width = 350;
        int height = 150;
        ImageSize size = calculate(getClass().getResource("/"+width+"x"+height+".png").toString());
        assertTrue(size.getWidth() == width);
        assertTrue(size.getHeight() == height);
    }

    public ImageSize calculate(String urlS) throws IOException {
        ImageInputStream in = null;
        try {
            URL url = new URL(urlS);
            URLConnection conn = url.openConnection();
            in = ImageIO.createImageInputStream(conn.getInputStream());
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    return new ImageSizeImpl(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return null;
    }
}
