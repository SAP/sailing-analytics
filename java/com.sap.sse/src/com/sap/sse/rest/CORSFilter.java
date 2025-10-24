package com.sap.sse.rest;

import java.io.IOException;
import java.util.Map.Entry;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Use this to filter a servlet request and set CORS headers based on the current {@link CORSFilterConfiguration}.
 * Specify in your web bundle's {@code web.xml} file as follows:
 * 
 * <pre>
&lt;filter&gt;
    &lt;filter-name&gt;CORSFilter&lt;/filter-name&gt;
    &lt;filter-class&gt;com.sap.sse.rest.CORSFilter&lt;/filter-class&gt;
&lt;/filter&gt;
&lt;filter-mapping&gt;
    &lt;filter-name&gt;CORSFilter&lt;/filter-name&gt;
    &lt;url-pattern&gt;*&lt;/url-pattern&gt;
&lt;/filter-mapping&gt;
 * </pre>
 * 
 * Being a full-fledged servlet filter and not a Jersey response filter, CORS header setting
 * also takes place if the servlet request will eventually not produce a response, e.g., because
 * authorization fails or some other exception is thrown prior to response production.
 * 
 * @see AccessControlAndEncodingResponseFilter
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class CORSFilter implements CORSAndCSPHeaderProvider, Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final String origin = ((HttpServletRequest) request).getHeader("Origin");
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        for (final Entry<String, String> headerNameAndValue : getCORSAndCSPHeaders(origin).entrySet()) {
            httpResponse.setHeader(headerNameAndValue.getKey(), headerNameAndValue.getValue());
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
