package com.sap.sailing.domain.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.test.AbstractSerializationTest;
import com.sap.sse.common.Util;
import com.sap.sse.common.scalablevalue.KadaneExtremeSubsequenceFinder;
import com.sap.sse.common.scalablevalue.ScalableDouble;

public class KadaneSerializationTest extends AbstractSerializationTest {
    @Test
    public void testBasicKadaneSerialization() throws IOException, ClassNotFoundException {
        final KadaneExtremeSubsequenceFinder<Double, Double, ScalableDouble> original = new com.sap.sse.common.scalablevalue.KadaneExtremeSubsequenceFinderLinkedNodesImpl<>();
        original.add(new ScalableDouble(17));
        original.add(new ScalableDouble(42));
        original.add(new ScalableDouble(-3));
        original.add(new ScalableDouble(-99));
        original.add(new ScalableDouble(12));
        assertDeserializedEqualsOriginal(original);
    }
    
    @Test
    public void testLongKadaneSerialization() throws IOException, ClassNotFoundException {
        final Random random = new Random();
        final KadaneExtremeSubsequenceFinder<Double, Double, ScalableDouble> original = new com.sap.sse.common.scalablevalue.KadaneExtremeSubsequenceFinderLinkedNodesImpl<>();
        final int NODES = 100000;
        for (int i=0; i<NODES; i++) {
            original.add(new ScalableDouble(random.nextDouble()-0.5));
        }
        assertDeserializedEqualsOriginal(original);
    }

    private void assertDeserializedEqualsOriginal(
            final KadaneExtremeSubsequenceFinder<Double, Double, ScalableDouble> original)
            throws IOException, ClassNotFoundException {
        final KadaneExtremeSubsequenceFinder<Double, Double, ScalableDouble> clone = cloneBySerialization(original, null);
        assertNotSame(clone, original);
        assertEquals(Util.asList(original), Util.asList(clone));
        assertEquals(original.getMaxSum(), clone.getMaxSum());
        assertEquals(original.getMinSum(), clone.getMinSum());
        assertEquals(original.getStartIndexOfMinSumSequence(), clone.getStartIndexOfMinSumSequence());
        assertEquals(original.getStartIndexOfMaxSumSequence(), clone.getStartIndexOfMaxSumSequence());
    }
}
