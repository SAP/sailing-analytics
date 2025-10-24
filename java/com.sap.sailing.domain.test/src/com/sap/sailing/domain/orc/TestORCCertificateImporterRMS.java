package com.sap.sailing.domain.orc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.orc.impl.ORCCertificatesRmsImporter;
import com.sap.sse.common.Duration;

public class TestORCCertificateImporterRMS extends AbstractORCCertificateImporterTest {
    private static final String RESOURCES = "resources/orc/";
    
    @Test
    public void testSimpleLocalRMSFileRead() throws IOException, ParseException {
        testSimpleLocalFileRead("GER2019.rms", "GER20041179");
    }
    
    @Test
    public void testRolexSwanCup2019() throws FileNotFoundException, IOException, ParseException {
        File fileSWAN = new File(RESOURCES + "SWAN2019.rms");
        ORCCertificatesCollection importer = ORCCertificatesRmsImporter.INSTANCE.read(new FileInputStream(fileSWAN));
        ORCCertificate flow = importer.getCertificateById("SW00029054");
        assertNotNull(flow);
        final Duration runAllowanceAtTenKnots = flow.getRunAllowances().get(new KnotSpeedImpl(10));
        assertNotNull(runAllowanceAtTenKnots);
        assertEquals(500.0, runAllowanceAtTenKnots.asSeconds(), 0.01);
    }
    
    @Test
    public void testLocalRMSFileReadWithIdenticalSailnumbers() throws IOException, ParseException {
        // additional test to ensure certificates with same sailnumbers but different ids are equally parsed and saved
        File file = new File(RESOURCES + "multipleIdenticalSailnumbers.rms");
        ORCCertificatesCollection certificates = ORCCertificatesImporter.INSTANCE.read(new FileInputStream(file));
        assertEquals(14, ((Collection<?>) (certificates.getCertificateIds())).size());
    }
}    
    
