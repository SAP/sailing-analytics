package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

public class JSONEnumTypeTest {
    private enum TestEnum{ VALUE1, VALUE2 };

    @Test
    public void testWriteEnum() throws IOException, ParseException {
        final String TEST_ENUM_KEY = "testEnum"; 
        TestEnum testEnum = TestEnum.VALUE1;
        Writer writer = new StringWriter(); 
        
        JSONObject jsonWithEnum = new JSONObject();
        jsonWithEnum.put(TEST_ENUM_KEY, testEnum);

        jsonWithEnum.writeJSONString(writer);

        Reader reader = new StringReader(writer.toString());
                
        JSONParser parser = new JSONParser();
        Object resultAsObject = parser.parse(reader);
        assertTrue(resultAsObject instanceof JSONObject);

        JSONObject resultAsJsonObject = (JSONObject) resultAsObject;
        String enumValue = (String) resultAsJsonObject.get(TEST_ENUM_KEY);
        TestEnum parsedTestEnum = TestEnum.valueOf(enumValue);
        
        assertEquals(testEnum, parsedTestEnum);
    }
}
