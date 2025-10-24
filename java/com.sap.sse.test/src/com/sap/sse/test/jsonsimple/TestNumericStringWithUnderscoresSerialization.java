package com.sap.sse.test.jsonsimple;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

/**
 * We face a strange issue with JSON String literals that represent file names coming from an iPhone
 * where the file name contains mostly digits, sometimes separated by two underscore characters.
 * These literals, it seems, are not properly getting parsed on Safari on iPhones.<p>
 * 
 * Does this have to do with the possibility to use single underscores in numeric literals to
 * separate groups of digits, where two consecutive underscores are forbidden?<p>
 * 
 * This test is asserting that at least no issues in this regard exist in the <tt>org.json.simple</tt>
 * library we're using for (de-)serializtion on the server side.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class TestNumericStringWithUnderscoresSerialization {
    private static final String FILE_NAME = "fileName";

    @Test
    public void testDoubleUnderscoreInStringStartingWithDigits() throws ParseException {
        assertSerializationOfValue("012__3456");
        assertSerializationOfValue("012__3456.jpeg");
        assertSerializationOfValue("012___3456");
        assertSerializationOfValue("01__3456");
    }

    private void assertSerializationOfValue(final String value) throws ParseException {
        final JSONObject o = new JSONObject();
        o.put(FILE_NAME, value);
        final String oAsJsonString = o.toJSONString();
        final JSONParser parser = new JSONParser();
        final JSONObject oParsed = (JSONObject) parser.parse(oAsJsonString);
        assertEquals(value, oParsed.get(FILE_NAME));
    }
}
