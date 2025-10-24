package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.mockito.internal.verification.VerificationModeFactory;

import com.sap.sse.rest.CORSFilter;
import com.sap.sse.rest.CORSFilterConfiguration;

public class CORSFilterConfigurationTest {
    @Test
    public void testWildcardFilter() throws IOException, ServletException {
        final com.sap.sse.impl.Activator activator = new com.sap.sse.impl.Activator();
        final CORSFilterConfiguration corsConfig = activator.getCORSFilterConfiguration();
        corsConfig.setWildcard();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Origin")).thenReturn("https://www.example.com");
        new CORSFilter().doFilter(request, response, chain);
        verify(response).setHeader("Access-Control-Allow-Origin", "*");
    }

    @Test
    public void testSpecificOriginFilter() throws IOException, ServletException {
        final com.sap.sse.impl.Activator activator = new com.sap.sse.impl.Activator();
        final CORSFilterConfiguration corsConfig = activator.getCORSFilterConfiguration();
        corsConfig.setOrigins(Collections.singleton("https://www.example.com"));
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Origin")).thenReturn("https://www.example.com");
        new CORSFilter().doFilter(request, response, chain);
        verify(response).setHeader("Access-Control-Allow-Origin", "https://www.example.com");
    }

    @Test
    public void testUnmatchedOriginFilter() throws IOException, ServletException {
        final com.sap.sse.impl.Activator activator = new com.sap.sse.impl.Activator();
        final CORSFilterConfiguration corsConfig = activator.getCORSFilterConfiguration();
        corsConfig.setOrigins(Collections.singleton("https://www.someotherexample.com"));
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Origin")).thenReturn("https://www.example.com");
        new CORSFilter().doFilter(request, response, chain);
        verify(response, VerificationModeFactory.atMost(0)).setHeader("Access-Control-Allow-Origin", "https://www.example.com");
    }

    @Test
    public void testNoOriginProvidedNoWildcard() throws IOException, ServletException {
        final com.sap.sse.impl.Activator activator = new com.sap.sse.impl.Activator();
        final CORSFilterConfiguration corsConfig = activator.getCORSFilterConfiguration();
        corsConfig.setOrigins(Collections.singleton("https://www.example.com"));
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        new CORSFilter().doFilter(request, response, chain);
        // expect that with no Origin header set, no allow-origin will be set either if not wildcard
        verify(response, VerificationModeFactory.atMost(0)).setHeader("Access-Control-Allow-Origin", "https://www.example.com");
    }

    @Test
    public void testNoOriginProvidedWildcard() throws IOException, ServletException {
        final com.sap.sse.impl.Activator activator = new com.sap.sse.impl.Activator();
        final CORSFilterConfiguration corsConfig = activator.getCORSFilterConfiguration();
        corsConfig.setWildcard();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        new CORSFilter().doFilter(request, response, chain);
        // expect that with no Origin header set the wildcard is still set
        verify(response).setHeader("Access-Control-Allow-Origin", "*");
    }

    @Test
    public void testCorrectOriginSetIfMultiplePossible() throws IOException, ServletException {
        final com.sap.sse.impl.Activator activator = new com.sap.sse.impl.Activator();
        final CORSFilterConfiguration corsConfig = activator.getCORSFilterConfiguration();
        corsConfig.setOrigins(Arrays.asList("https://www.example.com", "https://www.someotherexample.com"));
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Origin")).thenReturn("https://www.someotherexample.com");
        new CORSFilter().doFilter(request, response, chain);
        verify(response).setHeader("Access-Control-Allow-Origin", "https://www.someotherexample.com");
    }

    @Test
    public void testNoneSetIfMultiplePossibleButNoneMatching() throws IOException, ServletException {
        final com.sap.sse.impl.Activator activator = new com.sap.sse.impl.Activator();
        final CORSFilterConfiguration corsConfig = activator.getCORSFilterConfiguration();
        corsConfig.setOrigins(Arrays.asList("https://www.example.com", "https://www.someotherexample.com"));
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Origin")).thenReturn("https://www.yetsomeotherexample.com");
        new CORSFilter().doFilter(request, response, chain);
        verify(response, VerificationModeFactory.atMost(0)).setHeader("Access-Control-Allow-Origin", "https://www.yetsomeotherexample.com");
    }

    @Test
    public void testChangingFilterConfigFromWildcardToExplicit() throws IOException, ServletException {
        final com.sap.sse.impl.Activator activator = new com.sap.sse.impl.Activator();
        final CORSFilterConfiguration corsConfig = activator.getCORSFilterConfiguration();
        corsConfig.setWildcard();
        corsConfig.setOrigins(Arrays.asList("https://www.example.com", "https://www.someotherexample.com"));
        assertFalse(corsConfig.isWildcard());
    }
}
