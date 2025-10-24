package com.sap.sailing.server.gateway.trackfiles.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.server.trackfiles.impl.ExpeditionImportFileHandler;

public class ExpeditionImportFilenameUtilsTest {

    @Test
    public void testTruncateFilenameExtentions_CSV() {
        testCase("2017Nov08_2.csv", "2017Nov08_2");
        testCase("2017Nov04VeryShort.CSV", "2017Nov04VeryShort");
        testCase("2017Nov04.1s_clean.csv", "2017Nov04.1s_clean");
    }

    @Test
    public void testTruncateFilenameExtentions_TXT() {
        testCase("Expedition_28Oct17_0820.txt", "Expedition_28Oct17_0820");
        testCase("VarLog1Hz_20171104_100149_843.Txt", "VarLog1Hz_20171104_100149_843");
        testCase("VarLog1Hz_20171105_091128_062.TXT", "VarLog1Hz_20171105_091128_062");
    }

    @Test
    public void testTruncateFilenameExtentions_LOG() {
        testCase("file1.log", "file1");
        testCase("file2.LOG", "file2");
        testCase("file3.Log", "file3");
    }

    @Test
    public void testTruncateFilenameExtentions_GZ() {
        testCase("archive1.gz", "archive1");
        testCase("archive2.GZ", "archive2");
    }

    @Test
    public void testTruncateFilenameExtentions_ZIP() {
        testCase("archive1.zip", "archive1");
        testCase("archive2.ZIP", "archive2");
    }

    @Test
    public void testTruncateFilenameExtentions_CSV_GZ() {
        testCase("2017Nov08.csv.gz", "2017Nov08");
    }

    @Test
    public void testTruncateFilenameExtentions_CSV_ZIP() {
        testCase("2017Nov08.csv.zip", "2017Nov08");
    }

    @Test
    public void testTruncateFilenameExtentions_TXT_GZ() {
        testCase("VarLog1Hz_20171105_091128_062.Txt.gz", "VarLog1Hz_20171105_091128_062");
    }

    @Test
    public void testTruncateFilenameExtentions_TXT_ZIP() {
        testCase("VarLog1Hz_20171105_091128_062.Txt.zip", "VarLog1Hz_20171105_091128_062");
    }

    @Test
    public void testTruncateFilenameExtentions_LOG_GZ() {
        testCase("log-archive.log.gz", "log-archive");
    }

    @Test
    public void testTruncateFilenameExtentions_LOG_ZIP() {
        testCase("log-archive.log.zip", "log-archive");
    }

    private void testCase(String actualFilename, String expectedResult) {
        Assertions.assertEquals(expectedResult, ExpeditionImportFilenameUtils.truncateFilenameExtentions(actualFilename, new ExpeditionImportFileHandler() {
            @Override
            protected void handleExpeditionFile(String fileName, InputStream inputStream, Charset charset)
                    throws IOException, FormatNotSupportedException {
                // used only to resolve the file name extensions
            }
            
        }));
    }

}
