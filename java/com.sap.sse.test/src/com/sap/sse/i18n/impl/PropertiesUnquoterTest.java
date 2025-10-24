package com.sap.sse.i18n.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * A class whose {@link #main(String[])} method can be used to convert a .properties file written in the conventions as
 * expected by {@link ResourceBundleStringMessageImpl} to a plain text file. Duplicated single quotes will be un-escaped
 * into one single quote; text quoted by single quotes is output as is, without the enclosing single quotes. Unquoted
 * placeholders are output as specified in {@link #createPlaceholder(int)} (the current implementation only outputs the
 * placeholder in .properties file syntax, leaving it unchanged and indistinguishable from a quoted placeholder).
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class PropertiesUnquoterTest {
    private final String randomText = ""+new Random().nextDouble();
    
    @Test
    public void testMessageUnquotingSimpleString() {
        assertEquals("'Humba'", unquote("''Humba''"));
    }

    @Test
    public void testMessageUnquotingQuotedPlaceholder() {
        assertEquals("{0}", unquote("'{0}'"));
    }

    @Test
    public void testMessageUnquotingOnlyPlaceholder() {
        assertEquals(randomText, unquote("{0}", randomText));
    }

    @Test
    public void testMessageUnquotingQuotedSimpleString() {
        assertEquals("Humba", unquote("Hum'ba'"));
    }

    @Test
    public void testMessageUnquotingSimpleMultiLine() {
        assertEquals("Humba\nTrala", unquote("Humba\nTrala"));
    }

    @Test
    public void testMessageUnquotingMultiLineWithPlaceholderInSecondLine() {
        assertEquals("Humba\nTr"+randomText+"ala", unquote("Humba\nTr{0}ala", randomText));
    }

    @Test
    public void testMessageUnquotingMultiLineWithPlaceholderInFirstLine() {
        assertEquals("Hu"+randomText+"mba\nTrala", unquote("Hu{0}mba\nTrala", randomText));
    }

    private String unquote(String message, String... parameters) {
        return new ResourceBundleStringMessagesImpl(null, null).get(message, parameters);
    }
}
