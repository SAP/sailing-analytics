package com.sap.sailing.domain.orc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import com.sap.sailing.domain.orc.impl.ORCCertificateImporterRMS;
import com.sap.sailing.domain.orc.impl.ORCCertificateImporterRMS.ORCCertificateValues;

public class TestORCCertificateImporterRMS {
    private static final String RESOURCES = "resources/orc/";

    @Test
    public void testSimpleRMSFileRead() throws FileNotFoundException, IOException {
        final File gerRms = new File(RESOURCES + "GER2016.rms");
        ORCCertificateImporterRMS gerRmsFile = new ORCCertificateImporterRMS(new FileReader(gerRms));
        assertNotNull(gerRmsFile);
        assertTrue(gerRmsFile.getFileIds().contains("GER152487GER884"));
        final ORCCertificateValues ger884 = gerRmsFile.getValuesForFileId("GER152487GER884");
        assertNotNull(ger884);
        assertEquals("DE DOOD", ger884.getValue("BUILDER"));
        assertEquals("498.0", ger884.getValue("R9012"));
    }
}
