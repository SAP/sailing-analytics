package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.util.ImageConverter;
import com.sap.sse.util.ImageConverter.ImageWithMetadata;

/**
 * 
 * @author Robin Fleige (D067799)
 *
 */
public class ImageConverterTest {
    private ImageConverter converter;
    private ImageWithMetadata pngImage;
    private ImageWithMetadata jpgImage;

    private final byte[] onePixelPngExample =        new byte[] { -119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0, -112, 119, 83, -34, 0, 0, 0, 12, 73, 68, 65, 84, 120, -38, 99, 96, 96, 96, 0, 0, 0, 4, 0, 1, -56, -22, -21, -7, 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126};
    private final byte[] onePixelPngExample_Java11 = new byte[] { -119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0, -112, 119, 83, -34, 0, 0, 0, 12, 73, 68, 65, 84, 120, 94, 99, 96, 96, 96, 0, 0, 0, 4, 0, 1, 15, -46, -83, -28, 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126 };
    private final byte[] onePixelJpgExample = new byte[]{-1, -40, -1, -32, 0, 16, 74, 70, 73, 70, 0, 1, 2, 0, 0, 1, 0, 1, 0, 0, -1, -37, 0, 67, 0, 8, 6, 6, 7, 6, 5, 8, 7, 7, 7, 9, 9, 8, 10, 12, 20, 13, 12, 11, 11, 12, 25, 18, 19, 15, 20, 29, 26, 31, 30, 29, 26, 28, 28, 32, 36, 46, 39, 32, 34, 44, 35, 28, 28, 40, 55, 41, 44, 48, 49, 52, 52, 52, 31, 39, 57, 61, 56, 50, 60, 46, 51, 52, 50, -1, -37, 0, 67, 1, 9, 9, 9, 12, 11, 12, 24, 13, 13, 24, 50, 33, 28, 33, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, -1, -64, 0, 17, 8, 0, 1, 0, 1, 3, 1, 34, 0, 2, 17, 1, 3, 17, 1, -1, -60, 0, 31, 0, 0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, -1, -60, 0, -75, 16, 0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 125, 1, 2, 3, 0, 4, 17, 5, 18, 33, 49, 65, 6, 19, 81, 97, 7, 34, 113, 20, 50, -127, -111, -95, 8, 35, 66, -79, -63, 21, 82, -47, -16, 36, 51, 98, 114, -126, 9, 10, 22, 23, 24, 25, 26, 37, 38, 39, 40, 41, 42, 52, 53, 54, 55, 56, 57, 58, 67, 68, 69, 70, 71, 72, 73, 74, 83, 84, 85, 86, 87, 88, 89, 90, 99, 100, 101, 102, 103, 104, 105, 106, 115, 116, 117, 118, 119, 120, 121, 122, -125, -124, -123, -122, -121, -120, -119, -118, -110, -109, -108, -107, -106, -105, -104, -103, -102, -94, -93, -92, -91, -90, -89, -88, -87, -86, -78, -77, -76, -75, -74, -73, -72, -71, -70, -62, -61, -60, -59, -58, -57, -56, -55, -54, -46, -45, -44, -43, -42, -41, -40, -39, -38, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -1, -60, 0, 31, 1, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, -1, -60, 0, -75, 17, 0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 119, 0, 1, 2, 3, 17, 4, 5, 33, 49, 6, 18, 65, 81, 7, 97, 113, 19, 34, 50, -127, 8, 20, 66, -111, -95, -79, -63, 9, 35, 51, 82, -16, 21, 98, 114, -47, 10, 22, 36, 52, -31, 37, -15, 23, 24, 25, 26, 38, 39, 40, 41, 42, 53, 54, 55, 56, 57, 58, 67, 68, 69, 70, 71, 72, 73, 74, 83, 84, 85, 86, 87, 88, 89, 90, 99, 100, 101, 102, 103, 104, 105, 106, 115, 116, 117, 118, 119, 120, 121, 122, -126, -125, -124, -123, -122, -121, -120, -119, -118, -110, -109, -108, -107, -106, -105, -104, -103, -102, -94, -93, -92, -91, -90, -89, -88, -87, -86, -78, -77, -76, -75, -74, -73, -72, -71, -70, -62, -61, -60, -59, -58, -57, -56, -55, -54, -46, -45, -44, -43, -42, -41, -40, -39, -38, -30, -29, -28, -27, -26, -25, -24, -23, -22, -14, -13, -12, -11, -10, -9, -8, -7, -6, -1, -38, 0, 12, 3, 1, 0, 2, 17, 3, 17, 0, 63, 0, -9, -6, 40, -94, -128, 63, -1, -39};
    
    private final int baseSize = 100;
    private final int halfSize = baseSize / 2;
    private final int doubleSize = baseSize * 2;
    private final int minSize = 0;

    /**
     * Setting up the testing environment
     */
    @BeforeEach
    public void setUp() {
        converter = new ImageConverter();
        pngImage = converter.loadImage(new ByteArrayInputStream(onePixelPngExample), "png");
        jpgImage = converter.loadImage(new ByteArrayInputStream(onePixelJpgExample), "jpg");
    }

    /**
     * Tests the calculation of the dimensions, which is used for resizing the image to a fitting size
     */
    @Test
    public void testCalculateDimensions() {
        int[] dimensions;
        dimensions = converter.calculateDimensions(baseSize, baseSize, halfSize, doubleSize, halfSize, doubleSize);
        assertEquals(dimensions[0], baseSize);
        assertEquals(dimensions[1], baseSize);
        dimensions = converter.calculateDimensions(baseSize, baseSize, minSize, halfSize, minSize, halfSize);
        assertEquals(dimensions[0], halfSize);
        assertEquals(dimensions[1], halfSize);
        dimensions = converter.calculateDimensions(baseSize, doubleSize, minSize, halfSize, minSize, halfSize);
        assertTrue(dimensions[0] < halfSize);
        assertEquals(dimensions[1], halfSize);
        dimensions = converter.calculateDimensions(minSize, minSize, halfSize, baseSize, halfSize, baseSize);
        assertNull(dimensions);
    }

    /**
     * Tests the conversions that are done while resizing an image
     * 
     * @throws IOException
     */
    @Test
    public void testConversions() throws IOException {
        byte[] byteArray;
        byteArray = converter.inputStreamToByteArray(
                converter.imageWithMetadataToInputStream(jpgImage.getImage(), jpgImage.getMetadata(), "jpg"));
        assertArrayEquals(onePixelJpgExample, byteArray);
        byteArray = converter.inputStreamToByteArray(
                converter.imageWithMetadataToInputStream(pngImage.getImage(), pngImage.getMetadata(), "png"));
        final String javaVersion = System.getProperty("java.version");
        final String majorVersion = javaVersion.substring(0, javaVersion.indexOf('.'));
        final Integer majorVersionInt = majorVersion == null ? null : Integer.valueOf(majorVersion);
        if (majorVersionInt != null && majorVersionInt >= 11) {
            assertArrayEquals(onePixelPngExample_Java11, byteArray);
        } else {
            assertArrayEquals(onePixelPngExample, byteArray);
        }
    }
}
