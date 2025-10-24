package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.util.CSVParser;

public class CSVParserTest {
    @Test
    public void simpleTest() throws IOException {
        final CSVParser p = new CSVParser();
        final Reader r = new StringReader("a,b,c");
        final Iterable<List<String>> result = p.parseWithoutHeader(new BufferedReader(r));
        assertEquals(1, Util.size(result));
        final List<String> line = result.iterator().next();
        assertEquals("a", line.get(0));
        assertEquals("b", line.get(1));
        assertEquals("c", line.get(2));
    }

    @Test
    public void simpleTestWithQuotes() throws IOException {
        final CSVParser p = new CSVParser();
        final Reader r = new StringReader("a,\"b\",c");
        final Iterable<List<String>> result = p.parseWithoutHeader(new BufferedReader(r));
        assertEquals(1, Util.size(result));
        final List<String> line = result.iterator().next();
        assertEquals("a", line.get(0));
        assertEquals("b", line.get(1));
        assertEquals("c", line.get(2));
    }

    @Test
    public void simpleTestWithQuotedQuotes() throws IOException {
        final CSVParser p = new CSVParser();
        final Reader r = new StringReader("a,\"b\"\"\",c");
        final Iterable<List<String>> result = p.parseWithoutHeader(new BufferedReader(r));
        assertEquals(1, Util.size(result));
        final List<String> line = result.iterator().next();
        assertEquals("a", line.get(0));
        assertEquals("b\"", line.get(1));
        assertEquals("c", line.get(2));
    }

    @Test
    public void simpleTestWithHeader() throws IOException {
        final CSVParser p = new CSVParser();
        final Reader r = new StringReader("H1,H2,H3"+"\n"+
                                          "a,b,c");
        final Pair<List<String>, Iterable<List<String>>> result = p.parseWithHeader(new BufferedReader(r));
        assertEquals(Arrays.asList(new String[] {"H1", "H2", "H3"}), result.getA());
        assertEquals(1, Util.size(result.getB()));
        final List<String> line = result.getB().iterator().next();
        assertEquals("a", line.get(0));
        assertEquals("b", line.get(1));
        assertEquals("c", line.get(2));
    }

    @Test
    public void testExceptionForUnmatchedQuote() throws IOException {
        final CSVParser p = new CSVParser();
        final Reader r = new StringReader("a,\"b\"Humba,c");
        try {
            p.parseWithoutHeader(new BufferedReader(r));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
