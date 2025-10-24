package com.sap.sailing.domain.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.impl.RGBColor;

public class RGBColorTest {
    /**
     * Tests http://www.w3.org/TR/css3-color/#colorunits section 4.2.1 for three-digit color values. The specification
     * reads:
     * "The three-digit RGB notation (#rgb) is converted into six-digit form (#rrggbb) by replicating digits, not by adding zeros. For example, #fb0 expands to #ffbb00."
     */
    @Test
    public void testThreeDigitColor() {
        final RGBColor threeDigit = new RGBColor("#08f");
        final RGBColor sixDigit = new RGBColor("#0088ff");
        assertEquals(sixDigit, threeDigit);
    }
    
    @Test
    public void testHtmlConversion() {
        assertEquals("#0088FF", new RGBColor(0, 136, 255).getAsHtml());
    }
}
