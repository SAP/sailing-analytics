package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;

public class URLTest {
    @Test
    public void testEmptyFile() throws MalformedURLException {
        final URL url = new URL("https", "example.com", "");
        assertEquals("", url.getFile());
        assertFalse(url.toString().endsWith("/"));
    }
}
