package com.sap.sailing.server.trackfiles.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.server.trackfiles.impl.doublefix.DoubleFixProcessor;
import com.sap.sailing.server.trackfiles.impl.doublefix.DoubleVectorFixData;
import com.sap.sailing.server.trackfiles.impl.doublefix.DownsamplerTo1HzProcessor;

public class DownsamplerTo1HzProcessorTest {

    @Test
    public void testConsolidation() {
        
        MyProcessor delegate = new MyProcessor();
        DownsamplerTo1HzProcessor processor = new DownsamplerTo1HzProcessor(4, delegate);
        int second = 1 * 1000;
        processor.accept(new DoubleVectorFixData(second + 100, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 300, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 550, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 900, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 999, new Double[] { 0d, 0d, 0d, 0d }));
        second = 10 * 1000;
        processor.accept(new DoubleVectorFixData(second + 130, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 200, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 560, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 600, new Double[] { 0d, 0d, 0d, 0d }));
        second = 30 * 1000;
        processor.accept(new DoubleVectorFixData(second + 130, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 200, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 560, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 561, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 562, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 563, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 564, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 600, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 700, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 800, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 900, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 910, new Double[] { 0d, 0d, 0d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 920, new Double[] { 0d, 0d, 0d, 0d }));
        second = 50 * 1000;
        processor.accept(new DoubleVectorFixData(second + 130, new Double[] { 0d, 0d, 0d, 0d }));
        Assertions.assertEquals(3, delegate.countAccepted, "Count consolidated fixes");
        Assertions.assertFalse(delegate.fineshedWasCalled, "Not finished yet");
        
        processor.finish();
        
        Assertions.assertEquals(4, delegate.countAccepted, "Count consolidated fixes");
        Assertions.assertTrue(delegate.fineshedWasCalled, "Finished");
    }
    
    @Test
    public void testAverageComputation() {
        MyProcessor delegate = new MyProcessor();
        DownsamplerTo1HzProcessor processor = new DownsamplerTo1HzProcessor(4, delegate);
        int second = 1 * 1000;
        processor.accept(new DoubleVectorFixData(second + 100, new Double[] { 12.40d, 1d, 1d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 300, new Double[] { 0.01d, 2d, 1d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 550, new Double[] { -10.00d, 3d, 1d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 900, new Double[] { 1.00d, 2d, 1d, 0d }));
        processor.accept(new DoubleVectorFixData(second + 999, new Double[] { 0.00d, 1d, 1d, 0d }));

        processor.finish();
        Assertions.assertTrue(delegate.fineshedWasCalled, "Finished");
        Assertions.assertEquals(1, delegate.countAccepted, "Count consolidated fixes");
        DoubleVectorFixData lastFix = delegate.lastFix;
        Assertions.assertNotNull(lastFix, "Lastfix");
        assertEquals((12.4d + 0.01d - 10d + 1d + 0d) / 5d, (double) lastFix.getFix()[0], 0.000001, "Avg col 1");
        assertEquals(9d / 5d, (double) lastFix.getFix()[1], 0.000001, "Avg col 2");
        assertEquals(1d, (double) lastFix.getFix()[2], 0.000001, "Avg col 3");
        assertEquals(0d, (double) lastFix.getFix()[3], 0.000001, "Avg col 4");
    }

    private final class MyProcessor implements DoubleFixProcessor {
        public int countAccepted = 0;
        public boolean fineshedWasCalled = false;
        public DoubleVectorFixData lastFix;
        @Override
        public void finish() {
            fineshedWasCalled = true;
        }

        @Override
        public void accept(DoubleVectorFixData fix) {
            lastFix = fix;
            countAccepted++;
        }
    }

}
