package com.sap.sailing.declination.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.declination.impl.NOAAImporterForTesting;

public class NOAARegexpTest extends AbstractDeclinationTest<NOAAImporterForTesting> {
    @BeforeEach
    public void setUp() {
        importer = new NOAAImporterForTesting();
    }
    
    @Test
    public void testRegexpDeclination() {
        Matcher declinationMatcher = importer.getDeclinationPattern().matcher("    <p class=\"indent\"><b>Declination</b> = 12&deg; 41' W");
        assertTrue(declinationMatcher.find());
        assertEquals("12", declinationMatcher.group(1));
        assertEquals("41", declinationMatcher.group(2));
        assertEquals("W", declinationMatcher.group(3));
    }
    
    @Test
    public void testRegexpAnnualChange() {
        Matcher annualChangeMatcher = importer.getAnnualChangePattern().matcher("    changing by 0&deg; 11' E/year </p>");
        assertTrue(annualChangeMatcher.find());
        assertEquals("0", annualChangeMatcher.group(1));
        assertEquals("11", annualChangeMatcher.group(2));
        assertEquals("E", annualChangeMatcher.group(3));
    }
}
