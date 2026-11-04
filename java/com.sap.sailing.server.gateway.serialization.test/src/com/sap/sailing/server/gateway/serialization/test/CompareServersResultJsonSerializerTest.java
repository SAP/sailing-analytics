package com.sap.sailing.server.gateway.serialization.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import com.sap.sailing.server.gateway.deserialization.impl.CompareServersResultJsonDeserializer;
import com.sap.sailing.server.gateway.interfaces.CompareServersResult;
import com.sap.sailing.server.gateway.serialization.impl.CompareServersResultJsonSerializer;
import com.sap.sse.common.Util;
import com.sap.sse.shared.json.JsonDeserializationException;

public class CompareServersResultJsonSerializerTest {
    @Test
    public void testRoundTripPreservesServerToDiffAssociations() throws JsonDeserializationException {
        final JSONObject original = new JSONObject();
        final JSONObject sourceDiff = new JSONObject();
        sourceDiff.put("event", "Source event");
        final JSONArray sourceDiffs = new JSONArray();
        sourceDiffs.add(sourceDiff);
        final JSONObject archiveDiff = new JSONObject();
        archiveDiff.put("regatta", "Archive regatta");
        final JSONArray archiveDiffs = new JSONArray();
        archiveDiffs.add(archiveDiff);
        original.put("source", sourceDiffs);
        original.put("archive", archiveDiffs);
        assertRoundTrip(original);
        // The empty side must also stay associated with its server, regardless of JSON key iteration order.
        archiveDiffs.clear();
        assertRoundTrip(original);
    }

    private void assertRoundTrip(final JSONObject original) throws JsonDeserializationException {
        final CompareServersResult result = new CompareServersResultJsonDeserializer().deserialize(original);
        assertTrue(result.hasDiffs());
        assertEquals(original.get(result.getServerA()), Util.asList(result.getADiffs()));
        assertEquals(original.get(result.getServerB()), Util.asList(result.getBDiffs()));
        assertEquals(original, new CompareServersResultJsonSerializer().serialize(result));
    }
}
