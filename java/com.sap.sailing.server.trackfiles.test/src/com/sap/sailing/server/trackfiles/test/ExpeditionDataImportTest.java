package com.sap.sailing.server.trackfiles.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.sensordata.ExpeditionExtendedSensorDataMetadata;
import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.server.trackfiles.impl.ExpeditionExtendedDataImporterImpl;

public class ExpeditionDataImportTest {
    
    private int callbackCallCount;
    private double forestayValueSum;

    protected ExpeditionExtendedDataImporterImpl expeditionDataImporter;
    
    protected interface ImportDataDefinition {
        InputStream getInputStream();
        int getExpectedFixesCount();
    }
    
    @BeforeEach
    public void setUp() {
        expeditionDataImporter = new ExpeditionExtendedDataImporterImpl();
    }
    
    protected void testImport(ImportDataDefinition importData) throws FormatNotSupportedException, IOException {
        try (final InputStream is = importData.getInputStream()) {
            expeditionDataImporter.importFixes(is, Charset.defaultCharset(), (fixes, device) -> {
                for (DoubleVectorFix fix : fixes) {
                    if (fix != null) {
                        callbackCallCount++;
                        final Double forestay = fix.get(ExpeditionExtendedSensorDataMetadata.FORESTAY_LOAD.getColumnIndex());
                        if (forestay != null) {
                            forestayValueSum += forestay;
                        }
                    }
                }
            }, "filename.csv", "source", /* downsample */ false);
            Assertions.assertEquals(importData.getExpectedFixesCount(), callbackCallCount);
        }
    }

    @Test
    public void simpleFileRead() throws FormatNotSupportedException, IOException {
        testImport(ImportData.FILE_EXPEDITION_FULL);
        assertTrue(forestayValueSum/callbackCallCount > 0.7 && forestayValueSum/callbackCallCount < 7.0);
    }
    
    @Test
    public void simpleFileReadWithMissingColumns() throws FormatNotSupportedException, IOException {
        testImport(ImportData.FILE_EXPEDITION_PARTIAL);
        assertTrue(forestayValueSum/callbackCallCount > 0.5 && forestayValueSum/callbackCallCount < 7.0);
    }
    
    private enum ImportData implements ImportDataDefinition {
        // find out the number of fixes using the following bash line:
        //    tail -n +5 Undefined\ Race\ -\ BRAVO.txt | awk '{if ($5<$6) print v=$5; else v=$6; sum+=v; count++; } END {print "Sum: ", sum, "Count: ", count, "Average:", sum/count;}'
        FILE_EXPEDITION_FULL(19394) {
            @Override
            public InputStream getInputStream() {
                return getClass().getResourceAsStream("/2017Nov08_Expedition.csv");
            }
        },
        FILE_EXPEDITION_PARTIAL(199) {
            @Override
            public InputStream getInputStream() {
                return getClass().getResourceAsStream("/2018Feb10_clean_short_columns_removed.csv");
            }
        };
        
        private final int expectedFixesCount;

        private ImportData(int expectedFixesCount) {
            this.expectedFixesCount = expectedFixesCount;
        }
        
        @Override
        public int getExpectedFixesCount() {
            return expectedFixesCount;
        }
    }

}
