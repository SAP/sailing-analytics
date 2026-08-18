package com.sap.sailing.domain.orc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.orc.ORCCertificate;


public class TestORCCertificateImporterJSON extends AbstractORCCertificateImporterTest {
    private static final Logger logger = Logger.getLogger(TestORCCertificateImporterJSON.class.getName());

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
        for (final ORCCertificate referenceCert : certificates) {
            assertNotNull(referenceCert);
            // some certificates are not fully filled with allowances for all types of PCS pre-sets; we need to check whether
            // the certificate at hand has those we need for this test; else, we keep going and use another one.
            // We're already excluding LITE certificates to reduce chances for this case
            if (referenceCert.getLongDistanceSpeedPredictions().get(ORCCertificate.ALLOWANCES_TRUE_WIND_SPEEDS[0]) != null) {
                assertTrue(referenceCert.getWindwardLeewardSpeedPrediction().get(ORCCertificate.ALLOWANCES_TRUE_WIND_SPEEDS[0]).getDuration(ORCCertificate.NAUTICAL_MILE).asSeconds() > 10);
                assertTrue(referenceCert.getLongDistanceSpeedPredictions().get(ORCCertificate.ALLOWANCES_TRUE_WIND_SPEEDS[0]).getDuration(ORCCertificate.NAUTICAL_MILE).asSeconds() > 10);
                return;
            } else {
                logger.info("No valid GPH found in certificate "+referenceCert.getBoatName()+" with sail number "+referenceCert.getSailNumber());
            }
        }
        fail("Found only certificates with no valid long distance speed predictions; this seems unlikely and is probably an error");
    }
}
