package com.sap.sse.rest;

import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 * Use this in a Jersey servlet definition inside a {@code web.xml} file as follows to set CORS headers based on the
 * current {@link CORSFilterConfiguration}:
 * 
 * <pre>
&lt;servlet&gt;
  &lt;servlet-name&gt;Jersey REST API&lt;/servlet-name&gt;
  &lt;servlet-class&gt;com.sun.jersey.spi.container.servlet.ServletContainer&lt;/servlet-class&gt;
  &lt;init-param&gt;
    &lt;param-name&gt;javax.ws.rs.Application&lt;/param-name&gt;
    &lt;param-value&gt;com.sap.sse.threadmanager.RestApiApplication&lt;/param-value&gt;
  &lt;/init-param&gt;
  &lt;init-param&gt;
    &lt;param-name&gt;com.sun.jersey.spi.container.ContainerResponseFilters&lt;/param-name&gt;
    &lt;param-value&gt;com.sap.sse.rest.AccessControlAndEncodingResponseFilter&lt;/param-value&gt;
  &lt;/init-param&gt;   
  &lt;init-param&gt;
    &lt;param-name&gt;com.sun.jersey.config.feature.DisableWADL&lt;/param-name&gt;
    &lt;param-value&gt;true&lt;/param-value&gt;
  &lt;/init-param&gt;
  &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
&lt;/servlet&gt;
 * </pre>
 * 
 * It is applied when the servlet has successfully produced a response which is then to be delivered to the client. It
 * will <em>not</em> be invoked if request processing fails, e.g., at an earlier filter stage, for example when the
 * request is not authorized to proceed.
 * <p>
 * 
 * @see CORSFilter
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class AccessControlAndEncodingResponseFilter implements CORSAndCSPHeaderProvider, ContainerResponseFilter {
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        MultivaluedMap<String, Object> httpHeaders = response.getHttpHeaders();
        final String origin = request.getHeaderValue("Origin");
        for (final Entry<String, String> headerNameAndValue : getCORSAndCSPHeaders(origin).entrySet()) {
            httpHeaders.add(headerNameAndValue.getKey(), headerNameAndValue.getValue());
        }
        return response;
    }
}