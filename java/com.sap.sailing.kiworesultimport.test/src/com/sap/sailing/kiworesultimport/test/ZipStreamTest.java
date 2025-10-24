package com.sap.sailing.kiworesultimport.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.kiworesultimport.Boat;
import com.sap.sailing.kiworesultimport.BoatResultInRace;
import com.sap.sailing.kiworesultimport.ParserFactory;
import com.sap.sailing.kiworesultimport.RegattaSummary;
import com.sap.sailing.kiworesultimport.ZipFile;
import com.sap.sse.common.Util;

public class ZipStreamTest {
    private final static String ZIP_EXAMPLE_FILE = "resources/Kieler_Woche_2011_Export.zip";
    private final static String ZIP_EXAMPLE_FILE_YES = "resources/2012 Young Europeans Sailing-Export.zip";
    private final static String ZIP_EXAMPLE_FILE_KIWO_2012 = "resources/2012 Kieler Woche-Export.zip";
    
    @Test
    public void testOpenZip() throws IOException {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(ZIP_EXAMPLE_FILE));
        boolean foundStartberichteDir = false;
        ZipEntry nextEntry = zis.getNextEntry();
        while (nextEntry != null) {
            if (nextEntry.getName().startsWith("Startberichte\\")) {
                foundStartberichteDir = true;
            }
            nextEntry = zis.getNextEntry();
        }
        zis.close();
        assertTrue(foundStartberichteDir);
    }
    
    @Test
    public void testZipParser() throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
        ZipFile zipFile = ParserFactory.INSTANCE.createZipFileParser().parse(new FileInputStream(ZIP_EXAMPLE_FILE));
        assertNotNull(zipFile);
        Util.contains(zipFile.getBoatClassNames(), "Laser");
        RegattaSummary laser = zipFile.getRegattaSummary("Laser");
        assertEquals(9, Util.size(laser.getRaces()));
    }
    
    @Test
    public void twentyPercentTest() throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
        // test GER1899 in YES 29er regatta, race number 1
        ZipFile zipFile = ParserFactory.INSTANCE.createZipFileParser().parse(new FileInputStream(ZIP_EXAMPLE_FILE_YES));
        RegattaSummary twentyNiner = zipFile.getRegattaSummary("29er");
        Boat ger1899 = twentyNiner.getRace(1).getBoat("GER 1899");
        BoatResultInRace ger1899results = ger1899.getResultsInRace(1);
        assertEquals(MaxPointsReason.ZFP, ger1899results.getMaxPointsReason());
    }

    @Test
    public void asteriskNoParenthesesTest() throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
        // test GER1899 in YES 29er regatta, race number 1
        ZipFile zipFile = ParserFactory.INSTANCE.createZipFileParser().parse(new FileInputStream(ZIP_EXAMPLE_FILE_KIWO_2012));
        RegattaSummary laser = zipFile.getRegattaSummary("Laser");
        Boat stelmaszyk = laser.getRace(1).getBoat("POL 202671");
        BoatResultInRace stelmaszykResults = stelmaszyk.getResultsInRace(8);
        assertEquals(20.02, stelmaszykResults.getPoints(), 0.0000000001);
    }
}
