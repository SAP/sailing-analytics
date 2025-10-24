package com.sap.sailing.domain.orc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collection;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.orc.ORCCertificate;


public class TestORCCertificateImporterJSON extends AbstractORCCertificateImporterTest {
    @Test
    public void testSimpleLocalJSONFileRead() throws IOException, ParseException {
        testSimpleLocalFileRead("GER2019.json", "GER20041179");
    }
    
    @Test
    public void testReadingJSONWithSpecificBins() throws IOException, ParseException {
        testSimpleLocalFileRead("03600000HUH.json", "03600000HUH");
    }

    @FailIfNoValidOrcCertificates
    @Test
    public void testSimpleOnlineFileRead() throws IOException, ParseException, InterruptedException {
        Collection<ORCCertificate> certificates = FailIfNoValidOrcCertificateRule.getAvailableCerts();
        final ORCCertificate referenceCert = certificates.stream().findFirst().get();
        assertNotNull(referenceCert);
        assertTrue(referenceCert.getWindwardLeewardSpeedPrediction().get(ORCCertificate.ALLOWANCES_TRUE_WIND_SPEEDS[0]).getDuration(ORCCertificate.NAUTICAL_MILE).asSeconds() > 10); 
        assertTrue(referenceCert.getLongDistanceSpeedPredictions().get(ORCCertificate.ALLOWANCES_TRUE_WIND_SPEEDS[0]).getDuration(ORCCertificate.NAUTICAL_MILE).asSeconds() > 10);
    }
}
