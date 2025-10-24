package com.sap.sailing.domain.orc;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.extension.ExtendWith;

import com.sap.sailing.domain.common.orc.ORCCertificate;


@ExtendWith(FailIfNoValidOrcCertificateRule.class)
public abstract class AbstractORCCertificateImporterTest {
    protected static final String RESOURCES = "resources/orc/";
    
    protected void testSimpleLocalFileRead(String fileName, String expectedCertificateId) throws IOException, ParseException {
        File fileGER = new File(RESOURCES + fileName);
        ORCCertificatesCollection importer = ORCCertificatesImporter.INSTANCE.read(new FileInputStream(fileGER));
        ORCCertificate milan = importer.getCertificateById(expectedCertificateId);
        assertNotNull(milan);
    }
}
