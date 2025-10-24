package com.sap.sailing.nmeaconnector.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.nmeaconnector.NmeaFactory;
import com.sap.sse.common.Util;

public class NmeaFromZipImportTest {
    private ZipInputStream zipInputStream;

    @BeforeEach
    public void setUp() throws FileNotFoundException {
        NmeaFactory.INSTANCE.getUtil().registerAdditionalParsers();
    }
    
    @Test
    public void testReadOneZipEntry() throws IOException, InterruptedException {
        zipInputStream = new ZipInputStream(new FileInputStream("resources/LogSS.txt.zip"));
        ZipEntry entry;
        while ((entry=zipInputStream.getNextEntry()) != null) {
            if (entry.getName().toLowerCase().endsWith(".txt")) {
                Iterable<Wind> windFixes = NmeaFactory.INSTANCE.readWind(zipInputStream);
                assertTrue(!Util.isEmpty(windFixes));
            }
        }
    }

    @Disabled("This test only makes sense if the large file resources/Log210417.zip is present locally")
    @Test
    public void testOtherZipFile() throws IOException, InterruptedException {
        zipInputStream = new ZipInputStream(new FileInputStream("resources/Log210417.zip"));
        ZipEntry entry;
        while ((entry=zipInputStream.getNextEntry()) != null) {
            if (entry.getName().toLowerCase().endsWith(".txt")) {
                Iterable<Wind> windFixes = NmeaFactory.INSTANCE.readWind(zipInputStream);
                assertTrue(!Util.isEmpty(windFixes));
            }
        }
    }

    @Test
    public void testYetAnotherZipFile() throws IOException, InterruptedException {
        zipInputStream = new ZipInputStream(new FileInputStream("resources/NMEA.zip"));
        ZipEntry entry;
        while ((entry=zipInputStream.getNextEntry()) != null) {
            if (entry.getName().toLowerCase().endsWith(".txt")) {
                Iterable<Wind> windFixes = NmeaFactory.INSTANCE.readWind(zipInputStream);
                assertEquals(740, Util.size(windFixes));
            }
        }
    }

    @Test
    public void testZipFileExcerptWithOnlyEmptyWindRecords() throws IOException, InterruptedException {
        zipInputStream = new ZipInputStream(new FileInputStream("resources/NMEA_Snippet.zip"));
        ZipEntry entry;
        while ((entry=zipInputStream.getNextEntry()) != null) {
            if (entry.getName().toLowerCase().endsWith(".txt")) {
                Iterable<Wind> windFixes = NmeaFactory.INSTANCE.readWind(zipInputStream);
                assertTrue(Util.isEmpty(windFixes));
            }
        }
    }
}
