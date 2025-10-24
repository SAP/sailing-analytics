package com.sap.sse.rest;

import com.sap.sse.common.http.HttpHeaderUtil;

/**
 * Defines how to set the {@code Access-Control-Allow-Origin} response header in the
 * {@link CORSFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
 * method on this server. This influences, from which "origins" (basically the domains from which a web page has been
 * loaded into a brower) REST requests are allowed access through the {@link CORSFilter}. There are two possible
 * options:
 * <ol>
 * <li>wildcard (*): in this case, "*" will be set for the header field, and requests from all origins will be
 * allowed; this is strongly discouraged, especially because it would give incentives to developers to embed
 * access tokens in their server-less web page to make authenticated requests against this server, thus
 * exposing the access token to the browser's user.</li>
 * <li>a set of origin strings: the origin strings must include the full schema and hostname, for example
 * {@code https://developer.mozilla.org}.</li>
 * </ol>
 * 
 * Origin strings are treated ignoring case.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface CORSFilterConfiguration {
    /**
     * Tells if this filter configuration shall allow requests from all origins ("*"). If {@code true},
     * the results of {@link #contains(String)} are undefined.
     */
    boolean isWildcard();
    
    /**
     * Configures the corresponding {@link CORSFilter} such that requests from all origins are accepted. This clears any
     * set of explicitly allowed origins that may have been the basis for the {@link #contains(String)} method to
     * respond.
     */
    void setWildcard();

    /**
     * Well-defined if and only if {@link #isWildcard()} returns {@code false} for this configuration, meaning that
     * requests not from all but only from selected origins are allowed access through the {@link CORSFilter}.
     * 
     * @param origin
     *            the origin string as found in the {@code Origin} header of the HTTP request; it includes the
     *            protocol/scheme and would therefore look something like {@code https://developer.mozilla.org}.
     */
    boolean contains(String origin);
    
    /**
     * Makes this a non-{@link #isWildcard() wildcard} configuration that allows access only from those origins listed
     * in {@code allowedOrigins}.
     * 
     * @param allowedOrigins
     *            must not be {@code null} but may be empty; the strings in the iterable specify the origins from which
     *            the corresponding {@link CORSFilter} will accept requests, by setting an
     *            {@code Access-Control-Allow-Origin} header to the requesting origin if that origin string is contained
     *            in the {@code allowedOrigins} iterable.
     * @throws IllegalArgumentException
     *             in case any of the {@code allowedOrigins} doesn't pass the
     *             {@link HttpHeaderUtil#isValidOriginHeaderValue(String)} check; in this case, no change is performed
     *             to this configuration.
     */
    void setOrigins(Iterable<String> allowedOrigins) throws IllegalArgumentException;
    
    Iterable<String> getOrigins();
}
