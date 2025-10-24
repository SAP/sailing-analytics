package com.sap.sse.common.test;

import static com.sap.sse.common.Util.isEmpty;
import static com.sap.sse.common.Util.splitAlongWhitespaceRespectingDoubleQuotedPhrases;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;

/**
 * Tests {@link Util#splitAlongWhitespaceRespectingDoubleQuotedPhrases(String)}
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class SplittingStringsTest {
    @Test
    public void testSimpleSplit() {
        assertNull(splitAlongWhitespaceRespectingDoubleQuotedPhrases(null));
        assertEquals(Arrays.asList("a", "b", "c"), splitAlongWhitespaceRespectingDoubleQuotedPhrases("a b c"));
        assertEquals(Arrays.asList("a", "b c"), splitAlongWhitespaceRespectingDoubleQuotedPhrases("a \"b c\""));
        assertEquals(Arrays.asList("a", "b \" c"), splitAlongWhitespaceRespectingDoubleQuotedPhrases("a \"b \\\" c\""));
        assertEquals(Arrays.asList("a", "bc", "de"), splitAlongWhitespaceRespectingDoubleQuotedPhrases("a \"bc\"de"));
        assertEquals(Arrays.asList("a", "bc", "de"), splitAlongWhitespaceRespectingDoubleQuotedPhrases("a\"bc\"de "));
        assertEquals(Arrays.asList("a", "b \" c"), splitAlongWhitespaceRespectingDoubleQuotedPhrases("a \"b \\\" c\""));
        assertEquals(Arrays.asList(" "), splitAlongWhitespaceRespectingDoubleQuotedPhrases("\\ "));
        assertEquals(Arrays.asList("\""), splitAlongWhitespaceRespectingDoubleQuotedPhrases("  \\\""));
        assertEquals(Arrays.asList(" \\"), splitAlongWhitespaceRespectingDoubleQuotedPhrases("  \\ \\\\ "));
        assertTrue(isEmpty(splitAlongWhitespaceRespectingDoubleQuotedPhrases(" \n\t  ")));
        assertEquals(Arrays.asList("abc"), splitAlongWhitespaceRespectingDoubleQuotedPhrases("  \"abc")); // ignore unterminated quote
        assertEquals(Arrays.asList("abc "), splitAlongWhitespaceRespectingDoubleQuotedPhrases("  \"abc ")); // ignore unterminated quote
        assertEquals(Arrays.asList("afd", "fsd", "fd\\"), splitAlongWhitespaceRespectingDoubleQuotedPhrases("afd fsd  fd\\")); // trailing escape character stands for itself
        assertEquals(Arrays.asList("a "), splitAlongWhitespaceRespectingDoubleQuotedPhrases("\"a \"")); // trailing space preserved when within quotes
    }
}
