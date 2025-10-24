package com.sap.sailing.domain.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.util.UrlHelper;
import com.sap.sse.shared.util.impl.NonGwtUrlHelper;

public class NonGwtUrlHelperTest {
    
    private final UrlHelper urlHelper = NonGwtUrlHelper.INSTANCE;
    
    @Test
    public void testEncodeQueryStringSimple() {
        testCase("TestTest", "TestTest");
    }
    
    @Test
    public void testEncodeQueryStringWithWhitespace() {
        testCase("Test+Test", "Test Test");
    }
    
    @Test
    public void testEncodeQueryStringWithSlash() {
        testCase("Test%2FTest", "Test/Test");
    }
    
    @Test
    public void testEncodeQueryStringWithPlusSign() {
        testCase("Test%2BTest", "Test+Test");
    }
    
    @Test
    public void testEncodeQueryStringWithAmpersand() {
        testCase("Test%26Test", "Test&Test");
    }
    
    @Test
    public void testEncodeQueryStringComplex() {
        testCase("Test+%2B+Test+%2F+Test+%26+Test", "Test + Test / Test & Test");
    }
    
    private void testCase(String expectedEncodedString, String decodedString) {
        assertEquals(expectedEncodedString, urlHelper.encodeQueryString(decodedString));
    }

}
