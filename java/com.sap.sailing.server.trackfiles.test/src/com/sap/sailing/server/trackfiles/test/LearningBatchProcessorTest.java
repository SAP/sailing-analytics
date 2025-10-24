package com.sap.sailing.server.trackfiles.test;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackimport.DoubleVectorFixImporter;
import com.sap.sailing.server.trackfiles.impl.doublefix.DoubleVectorFixData;
import com.sap.sailing.server.trackfiles.impl.doublefix.LearningBatchProcessor;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class LearningBatchProcessorTest {



    @Test
    public void testConsolidation() {
        
        MyCallback delegate = new MyCallback();
        LearningBatchProcessor processor = new LearningBatchProcessor(5, 10, delegate, device);
        int currentSecondAsMillis = 1 * 1000;
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 100, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 300, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 550, new Double[] { 0d, 0d, 0d, 0d }));
        Assertions.assertEquals(0, delegate.batchesDelivered, "Batch not full");

        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 900, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 999, new Double[] { 0d, 0d, 0d, 0d }));
        Assertions.assertEquals(0, delegate.batchesDelivered, "Batch not full, should be learing");

        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 130, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 200, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 560, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 600, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 130, new Double[] { 0d, 0d, 0d, 0d }));
        Assertions.assertEquals(2, delegate.batchesDelivered, "Batches after learning");
        Assertions.assertEquals(5, delegate.lastBatchSize, "Last batch after learning");

        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 200, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 560, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 561, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 562, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 563, new Double[] { 0d, 0d, 0d, 0d }));
        Assertions.assertEquals(3, delegate.batchesDelivered, "Next batch received");
        Assertions.assertEquals(5, delegate.lastBatchSize, "First batch after learning");

        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 564, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 600, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 700, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(currentSecondAsMillis + 800, new Double[] { 0d, 0d, 0d, 0d }));

        Assertions.assertEquals(3, delegate.batchesDelivered, "Still on same batch");
        processor.finish();
        
        Assertions.assertEquals(4, delegate.batchesDelivered, "Batch count after finish");
        Assertions.assertEquals(4, delegate.lastBatchSize, "Last batch size after finish");
        Assertions.assertEquals((currentSecondAsMillis + 800), delegate.lastFix.getTimePoint().asMillis(), "Last fix ");
    }
    


    private final class MyCallback implements DoubleVectorFixImporter.Callback {
        public int lastBatchSize = 0;
        public int batchesDelivered = 0;
        public DoubleVectorFix lastFix;
        
        @Override
        public void addFixes(Iterable<DoubleVectorFix> fix, TrackFileImportDeviceIdentifier device) {
            lastBatchSize = Util.size(fix);
            batchesDelivered++;
            lastFix = Util.last(fix);
        }
    }

    TrackFileImportDeviceIdentifier device = new TrackFileImportDeviceIdentifier() {
        private static final long serialVersionUID = 870061505508090216L;
        private UUID uuid = UUID.randomUUID();
        private MillisecondsTimePoint uploadedAt = new MillisecondsTimePoint(0);

        @Override
        public String getStringRepresentation() {
            return "stringRepresentation";
        }

        @Override
        public String getIdentifierType() {
            return "identifierType";
        }

        @Override
        public TimePoint getUploadedAt() {
            return uploadedAt;
        }

        @Override
        public String getTrackName() {
            return "trackname";
        }

        @Override
        public UUID getId() {
            return uuid;
        }

        @Override
        public String getFileName() {
            return "filename";
        }
    };
}
