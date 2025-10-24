package com.sap.sailing.declination.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.declination.impl.DeclinationImporter;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public abstract class DeclinationImportTest<I extends DeclinationImporter> extends AbstractDeclinationTest<I> {
    
    @Test
    public void importSimpleDeclination() throws IOException, ParseException, ParserConfigurationException, SAXException {
        Declination record = importer.importRecord(new DegreePosition(53, 3),
                new MillisecondsTimePoint(simpleDateFormat.parse("2020-04-17").getTime()));
        assertEquals(1.03748, record.getBearing().getDegrees(), 0.05);
        assertEquals(0.14795, record.getAnnualChange().getDegrees(), 0.06);
    }

    @Test
    public void importSouthernHemisphereDeclination() throws IOException, ParseException, ParserConfigurationException, SAXException {
        Declination record = importer.importRecord(new DegreePosition(-10, 3),
                new MillisecondsTimePoint(simpleDateFormat.parse("2019-12-14").getTime()));
        assertEquals(-7.8071, record.getBearing().getDegrees(), 0.05);
        assertEquals(0.17712, record.getAnnualChange().getDegrees(), 0.05);
    }
    
    @Test
    public void readOnlineOrFromFile() throws IOException, ClassNotFoundException, ParseException {
        Declination declination = importer.getDeclination(new DegreePosition(53, 3),
                new MillisecondsTimePoint(simpleDateFormat.parse("2019-12-12").getTime()), 
                /* timeoutForOnlineFetchInMilliseconds */ 100000);
        assertNotNull(declination);
    }
}
