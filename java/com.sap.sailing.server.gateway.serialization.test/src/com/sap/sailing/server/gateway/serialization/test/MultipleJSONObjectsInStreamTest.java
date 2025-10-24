package com.sap.sailing.server.gateway.serialization.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates and validates how to write more than one JSON object into a single stream as a JSONArray
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class MultipleJSONObjectsInStreamTest {
    @Test
    public void testMultipleJSONObjectsInOneStream() throws IOException, ParseException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        JSONObject o1 = new JSONObject();
        o1.put("o1", "o1");
        JSONObject o2 = new JSONObject();
        o2.put("o2", "o2");
        bos.write('[');
        bos.write(o1.toJSONString().getBytes());
        bos.write(',');
        bos.write(o2.toJSONString().getBytes());
        bos.write(']');
        bos.close();
        
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        JSONParser parser = new JSONParser();
        Object o = parser.parse(new InputStreamReader(bis));
        assertTrue(o instanceof JSONArray);
        JSONArray a = (JSONArray) o;
        assertEquals("o1", ((JSONObject) a.get(0)).get("o1"));
        assertEquals("o2", ((JSONObject) a.get(1)).get("o2"));
    }
}
