package com.sap.sse.landscape.aws.common.shared;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class DefaultRedirectConversionTest {
    @Test
    public void testDefaultRedirectParsing() {
        // Note the strange _=_ parameter; see also https://support.console.aws.amazon.com/support/home#/case/?displayId=8094019001&language=en
        // and the corresponding comment on EventRedirectDTO.QUERY_PREFIX
        final String redirectPath = "/gwt/Home.html?_=_&#{query}#/event/:eventId=b8220cee-9ec7-4640-b8d8-f40e079456d5";
        final RedirectDTO redirect = RedirectDTO.from(redirectPath);
        assertNotNull(redirect);
        assertSame(EventRedirectDTO.class, redirect.getClass());
    }
}
