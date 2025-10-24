package com.sap.sse.datamining.impl.i18n;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.ModifiableDataMiningServer;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.TestsUtil;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class TestI18N {

    private static final Locale DEFAULT_LOCALE = new Locale("default");
    private static final String SIMPLE_TEST_MESSAGE_KEY = "SimpleTestMessage";
    private static final String DEFAULT_TEST_MESSAGE_KEY = "DefaultTestMessage";
    private static final String TEST_MESSAGE_WITH_PARAMETERS = "TestMessageWithParameters";
    private static final String TEST_MESSAGE_WITH_SINGLE_QUOTE = "MessageWithSingleQuote";
    private static final String TEST_MESSAGE_WITH_OPENING_CURLY_BRACE = "MessageWithOpeningCurlyBrace";
    private static final String TEST_MESSAGE_WITH_ESCAPED_PARAMETERS = "MessageWithEscapedParameters";
    private static final String TEST_MESSAGE_WITH_PARAMETERS_AND_ESCAPED_SINGLE_QUOTES = "MessageWithEscapedSingleQuotesAndParameters";
    private static final String TEST_MESSAGE_WITH_MULTIPLE_OUT_OF_ORDER_PARAMS = "MessageWithMultipleParameterOccurrencesOutOfOrder";
    
    private ResourceBundleStringMessages testStringMessages;
    
    @BeforeEach
    public void initializeBundleManager() {
        testStringMessages = TestsUtil.getTestStringMessages();
    }

    @Test
    public void testMultipleOutOfOrderParams() {
        assertThat(testStringMessages.get(DEFAULT_LOCALE, TEST_MESSAGE_WITH_MULTIPLE_OUT_OF_ORDER_PARAMS, "First", "Second", "Third"), is("Third Third First Second First"));
        assertThat(testStringMessages.get(Locale.GERMAN, TEST_MESSAGE_WITH_MULTIPLE_OUT_OF_ORDER_PARAMS, "First", "Second", "Third"), is("Third Third First Second First"));
    }
    
    @Test
    public void testEscapedSingleQuotesWithParameters() {
        assertThat(testStringMessages.get(DEFAULT_LOCALE, TEST_MESSAGE_WITH_PARAMETERS_AND_ESCAPED_SINGLE_QUOTES, "First", "Second"), is("'First' ' 'Second'"));
        assertThat(testStringMessages.get(Locale.GERMAN, TEST_MESSAGE_WITH_PARAMETERS_AND_ESCAPED_SINGLE_QUOTES, "First", "Second"), is("'First' ' 'Second'"));
    }
    
    @Test
    public void testSingleQuoteInString() {
        assertThat(testStringMessages.get(DEFAULT_LOCALE, TEST_MESSAGE_WITH_SINGLE_QUOTE), is("A Single ' Quote"));
        assertThat(testStringMessages.get(Locale.GERMAN, TEST_MESSAGE_WITH_SINGLE_QUOTE), is("Ein einfaches ' Anführungszeichen"));
    }
    
    @Test
    public void testCurlyBracesInString() {
        assertThat(testStringMessages.get(DEFAULT_LOCALE, TEST_MESSAGE_WITH_OPENING_CURLY_BRACE), is("An opening { curly brace"));
        assertThat(testStringMessages.get(Locale.GERMAN, TEST_MESSAGE_WITH_OPENING_CURLY_BRACE), is("Eine öffnende { geschweifte Klammer"));
    }
    
    @Test
    public void testEscapedParametersInString() {
        assertThat(testStringMessages.get(DEFAULT_LOCALE, TEST_MESSAGE_WITH_ESCAPED_PARAMETERS), is("{0} {1} {2}"));
        assertThat(testStringMessages.get(Locale.GERMAN, TEST_MESSAGE_WITH_ESCAPED_PARAMETERS), is("{0} {1} {2}"));
    }
    
    @Test
    public void testGettingASimpleMessage() {
        assertThat(testStringMessages.get(DEFAULT_LOCALE, SIMPLE_TEST_MESSAGE_KEY), is("English"));
        assertThat(testStringMessages.get(Locale.ROOT, SIMPLE_TEST_MESSAGE_KEY), is("English"));
        assertThat(testStringMessages.get(Locale.ENGLISH, SIMPLE_TEST_MESSAGE_KEY), is("English"));
        assertThat(testStringMessages.get(Locale.GERMAN, SIMPLE_TEST_MESSAGE_KEY), is("Deutsch"));
    }
    
    @Test
    public void testGettingADefaultMessage() {
        assertThat(testStringMessages.get(DEFAULT_LOCALE, DEFAULT_TEST_MESSAGE_KEY), is("Default"));
        assertThat(testStringMessages.get(Locale.ROOT, DEFAULT_TEST_MESSAGE_KEY), is("Default"));
        assertThat(testStringMessages.get(Locale.ENGLISH, DEFAULT_TEST_MESSAGE_KEY), is("Default"));
        assertThat(testStringMessages.get(Locale.GERMAN, DEFAULT_TEST_MESSAGE_KEY), is("Default"));
    }
    
    @Test
    public void testGettingAMessageWithParameters() {
        assertThat(testStringMessages.get(DEFAULT_LOCALE, TEST_MESSAGE_WITH_PARAMETERS, "Param0", "Param1"), is("English Param0 - Param1"));
        assertThat(testStringMessages.get(Locale.ROOT, TEST_MESSAGE_WITH_PARAMETERS, "Param0", "Param1"), is("English Param0 - Param1"));
        assertThat(testStringMessages.get(Locale.ENGLISH, TEST_MESSAGE_WITH_PARAMETERS, "Param0", "Param1"), is("English Param0 - Param1"));
        assertThat(testStringMessages.get(Locale.GERMAN, TEST_MESSAGE_WITH_PARAMETERS, "Param0", "Param1"), is("Deutsch Param0 - Param1"));
    }
    
    @Test
    public void testDynamicCompoundStringMessages() {
        ModifiableDataMiningServer server = TestsUtil.createNewServer();
        
        try {
            server.getStringMessages().get(Locale.ENGLISH, SIMPLE_TEST_MESSAGE_KEY);
            fail("There shouldn't be a string message in an empty compound string messages");
        } catch (MissingResourceException e) { }

        Date beforeChange = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.addStringMessages(testStringMessages);
        assertThat(server.getComponentsChangedTimepoint().after(beforeChange), is(true));
        
        beforeChange = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.addStringMessages(testStringMessages);
        assertThat(server.getComponentsChangedTimepoint().after(beforeChange), is(false));
        
        assertThat(server.getStringMessages().get(Locale.ENGLISH, SIMPLE_TEST_MESSAGE_KEY), is("English"));
        assertThat(server.getStringMessages().get(Locale.GERMAN, SIMPLE_TEST_MESSAGE_KEY), is("Deutsch"));

        beforeChange = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.removeStringMessages(testStringMessages);
        assertThat(server.getComponentsChangedTimepoint().after(beforeChange), is(true));

        beforeChange = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.removeStringMessages(testStringMessages);
        assertThat(server.getComponentsChangedTimepoint().after(beforeChange), is(false));
        try {
            server.getStringMessages().get(Locale.ENGLISH, SIMPLE_TEST_MESSAGE_KEY);
            fail("There shouldn't be a string message in an empty compound string messages");
        } catch (MissingResourceException e) { }
    }
    
    @Test
    public void testGetLocaleForLocaleInfoName() {
        assertThat(ResourceBundleStringMessages.Util.getLocaleFor("default"), is(DEFAULT_LOCALE));
        assertThat(ResourceBundleStringMessages.Util.getLocaleFor("en"), is(Locale.ENGLISH));
        assertThat(ResourceBundleStringMessages.Util.getLocaleFor("de"), is(Locale.GERMAN));
        assertThat(ResourceBundleStringMessages.Util.getLocaleFor("zh"), is(Locale.CHINESE));
        assertThat(ResourceBundleStringMessages.Util.getLocaleFor("ja"), is(Locale.JAPANESE));

        assertThat(ResourceBundleStringMessages.Util.getLocaleFor("Unsupported locale info name"), is(Locale.ROOT));
    }
    
    @Test
    public void testGettingAMessageForAMissingKeyFromSimpleStringMessages() {
        assertThrows(MissingResourceException.class, () -> {
            testStringMessages.get(Locale.ENGLISH, "$%&MissingKey/()");
        });
    }
    
    @Test
    public void testGettingAMessageForAMissingKeyFromCompoundStringMessages() {
        assertThrows(MissingResourceException.class, () -> {
            TestsUtil.getTestStringMessagesWithProductiveMessages().get(Locale.ENGLISH, "$%&MissingKey/()");
        });
    }
}
