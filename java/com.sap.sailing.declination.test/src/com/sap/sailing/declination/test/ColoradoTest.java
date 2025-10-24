package com.sap.sailing.declination.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.text.ParseException;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.declination.impl.ColoradoImporter;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;

@Disabled("currently, http://magcalc.geomag.info/ seems down")
public class ColoradoTest {
    @Test
    public void simpleDocumentParsingTest() throws SAXException, IOException, ParserConfigurationException {
        final Declination declination = new ColoradoImporter().getDeclinationFromXml(getClass().getResourceAsStream("/colorado.xml"));
        validateSomeDeclinationInJanuary2018(declination);
    }

    private void validateSomeDeclinationInJanuary2018(final Declination declination) {
        assertEquals(10.0, declination.getPosition().getLatDeg(), 0.000001);
        assertEquals(20.0, declination.getPosition().getLngDeg(), 0.000001);
        assertEquals(1.91357, declination.getBearing().getDegrees(), 0.000001);
        assertEquals(0.1102, declination.getAnnualChange().getDegrees(), 0.000001);
    }
    
    @Test
    public void simpleOnlineTest() throws IOException, ParseException {
        final GregorianCalendar cal = new GregorianCalendar(2018, 0, 23);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        final Declination declination = new ColoradoImporter().getDeclination(new DegreePosition(10, 20),
                new MillisecondsTimePoint(cal.getTime()),
                /* timeoutForOnlineFetchInMilliseconds */ 5000);
        validateSomeDeclinationInJanuary2018(declination);
    }
}
