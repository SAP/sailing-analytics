package com.sap.sse.common.impl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.impl.AbstractColor;
import com.sap.sse.common.impl.HSVColor;
import com.sap.sse.common.impl.RGBColor;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util.Triple;

public class AbstractColorTest {

    @Test
    public void testShortRGBColor() {
        Color color = AbstractColor.getCssColor("#0");
        Assertions.assertEquals("#000000", color.getAsHtml());
        color = AbstractColor.getCssColor("#00F");
        Assertions.assertEquals("#0000FF", color.getAsHtml());
        color = AbstractColor.getCssColor("F");
        Assertions.assertEquals("#0F0F0F", color.getAsHtml());
        color = AbstractColor.getCssColor("0F0");
        Assertions.assertEquals("#00FF00", color.getAsHtml());
    }
    
    @Test
    public void testRGBColor() throws Exception {
        Color color = AbstractColor.getCssColor("rgb(255,255,255)");
        Assertions.assertEquals("#FFFFFF", color.getAsHtml());
        color = AbstractColor.getCssColor("rgb(1, 2, 3)");
        Assertions.assertEquals("#010203", color.getAsHtml());
        color = AbstractColor.getCssColor("rgb(100%,100%,100%)");
        Assertions.assertEquals("#FFFFFF", color.getAsHtml());
        color = AbstractColor.getCssColor("rgb(255,255,255,777,354)");
        Assertions.assertEquals(null, color);
        color = AbstractColor.getCssColor("rgb(255   ,255,255)");
        Assertions.assertEquals("#FFFFFF", color.getAsHtml());
        color = AbstractColor.getCssColor("rgb(3666 ,255,255)");
        Assertions.assertEquals("#FFFFFF", color.getAsHtml());
        color = AbstractColor.getCssColor("rgb(3666.98 ,255,255)");
        Assertions.assertEquals(null, color);
    }

    @Test
    public void testRGBA() throws Exception {
        Color color = AbstractColor.getCssColor("rgba(255,255,255,0.3)");
        Assertions.assertEquals("#FFFFFF", color.getAsHtml());
        color = AbstractColor.getCssColor("rgba(255   ,255,255,90)");
        Assertions.assertEquals("#FFFFFF", color.getAsHtml());
        color = AbstractColor.getCssColor("rgba(3666 ,255,255,0.3)");
        Assertions.assertEquals("#FFFFFF", color.getAsHtml());
        color = AbstractColor.getCssColor("rgba(3666.98 ,255,255,0.777)");
        Assertions.assertEquals(null, color);
    }

    @Test
    public void testHSL() throws Exception {
        Color color = AbstractColor.getCssColor("hsl(360, 100%, 100%)");
        Assertions.assertEquals("#FF00FF", color.getAsHtml());
        color = AbstractColor.getCssColor("HSL(360,     1.0, 1.0)");
        Assertions.assertEquals("#FF00FF", color.getAsHtml());
        color = AbstractColor.getCssColor("hsl(360,     1.0, 1.0,435,789)");
        Assertions.assertEquals(null, color);
    }

    @Test
    public void testHSLA() throws Exception {
        Color color = AbstractColor.getCssColor("hsla(360, 100%, 100%,hjhb)");
        Assertions.assertEquals("#FF00FF", color.getAsHtml());
        color = AbstractColor.getCssColor("HSLa(360,     1.0, 1.0,0.4)");
        Assertions.assertEquals("#FF00FF", color.getAsHtml());
        color = AbstractColor.getCssColor("hsla(360,     1.0, 1.0,435,789)");
        Assertions.assertEquals(null, color);
    }

    @Test
    public void testColorName() {
        Color color = AbstractColor.getCssColor("BrOwn");
        Assertions.assertEquals(Color.BROWN, color);
        color = AbstractColor.getCssColor("caffee");
        Assertions.assertEquals("#CAFFEE", color.getAsHtml());
    }
    
    @Test
    public void testRgbWithoutParentheses() {
        assertNull(AbstractColor.getCssColor("rgb"));
    }
    
    @Test
    public void testHSVConversion() {
        Color color = new RGBColor(5, 6, 7);
        Triple<Float, Float, Float> hsvValues = color.getAsHSV();
        HSVColor hsv = new HSVColor(hsvValues.getA(), hsvValues.getB(), hsvValues.getC());
        Triple<Integer, Integer, Integer> rgbValues = hsv.getAsRGB();
        assertEquals(5, (int) rgbValues.getA());
        assertEquals(6, (int) rgbValues.getB());
        assertEquals(7, (int) rgbValues.getC());
        // according to http://www.rapidtables.com/convert/color/rgb-to-hsv.htm rgb(5, 6, 7) equals
        // hsv(210, 0.286, 0.027)
        assertEquals(210.0, hsvValues.getA(), 0.01);
        assertEquals(0.286, hsvValues.getB(), 0.01);
        assertEquals(0.027, hsvValues.getC(), 0.01);
    }

}
