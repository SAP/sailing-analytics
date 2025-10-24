package com.sap.sailing.server.trackfiles.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackimport.DoubleVectorFixImporter;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.server.trackfiles.impl.BaseBravoDataImporterImpl;
import com.sap.sailing.server.trackfiles.impl.BravoDataImporterImpl;
import com.sap.sailing.server.trackfiles.impl.doublefix.DownsamplerTo1HzProcessor;
import com.sap.sailing.server.trackfiles.impl.doublefix.LearningBatchProcessor;
import com.sap.sse.common.Util;

public abstract class AbstractBravoDataImportTest {
    
    private DownsamplerTo1HzProcessor downsampler;

    protected BaseBravoDataImporterImpl bravoDataImporter;
    private int callbackCallCount = 0;
    
    protected abstract int getTrackColumnCount();
    
    protected abstract Map<String, Integer> getColumnData();
    
    protected interface ImportDataDefinition {
        InputStream getInputStream();
        int getExpectedFixesCount();
        int getExpectedFixesConsolidated();
    }
    
    @BeforeEach
    public void setUp() {
        this.callbackCallCount = 0;
        bravoDataImporter = new BaseBravoDataImporterImpl(getColumnData(), BravoDataImporterImpl.BRAVO_TYPE) {
            protected com.sap.sailing.server.trackfiles.impl.doublefix.DoubleFixProcessor createDownsamplingProcessor(
                    DoubleVectorFixImporter.Callback callback,
                    TrackFileImportDeviceIdentifier trackIdentifier) {
                final LearningBatchProcessor batchProcessor = new LearningBatchProcessor(5000, 5000, callback, trackIdentifier);
                downsampler = new DownsamplerTo1HzProcessor(getTrackColumnCount(), batchProcessor);
                return downsampler;
            }
        };
    }
    
    protected void testImport(ImportDataDefinition importData) throws FormatNotSupportedException, IOException {
        try (final InputStream is = importData.getInputStream()) {
            bravoDataImporter.importFixes(is, Charset.defaultCharset(), (fixes, device) -> {
                callbackCallCount+=Util.size(fixes);
            }, "filename", "source", /* downsample */ true);
            Assertions.assertEquals(importData.getExpectedFixesCount(), downsampler.getCountSourceTtl());
            Assertions.assertEquals(importData.getExpectedFixesConsolidated(), downsampler.getCountImportedTtl());
            Assertions.assertEquals(importData.getExpectedFixesConsolidated(), callbackCallCount);
        }
    }

}
