package com.sap.sse.security.shiro;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.web.servlet.ShiroFilter;

/**
 * A special {@link ShiroFilter} that disables itself for HTTP requests using the <tt>OPTIONS</tt>
 * method. This is useful because such requests are generally sent without authentication / authorization
 * information as all HTTP headers are usually stripped from these requests. Yet, browsers depend on
 * these "pre-flight" requests to succeed with a 2XX response status in order to continue with the real
 * request (which then uses proper <tt>Authorization</tt> headers).<p>
 * 
 * Since responses to <tt>OPTIONS</tt> requests don't contain any payload in their body, ignoring the
 * authorization and authentication and instead returning full CORS headers is so intended and a 2XX
 * status is what helps browsers to continue the right way.<p>
 * 
 * Use this in your web.xml file, e.g., like so:
 * <pre>
 *      &lt;filter&gt;
 *          &lt;filter-name&gt;ShiroFilter&lt;/filter-name&gt;
 *          &lt;filter-class&gt;com.sap.sse.security.shiro.ShiroFilterForAllButOptionsRequests&lt;/filter-class&gt;
 *      &lt;/filter&gt;
 *      &lt;filter-mapping&gt;
 *          &lt;filter-name&gt;ShiroFilter&lt;/filter-name&gt;
 *          &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 *          &lt;dispatcher&gt;REQUEST&lt;/dispatcher&gt;
 *          &lt;dispatcher&gt;FORWARD&lt;/dispatcher&gt;
 *          &lt;dispatcher&gt;INCLUDE&lt;/dispatcher&gt;
 *          &lt;dispatcher&gt;ERROR&lt;/dispatcher&gt;
 *      &lt;/filter-mapping&gt;
 * </pre>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class ShiroFilterForAllButOptionsRequests extends ShiroFilter {
    /**
     * Enables this filter for all request methods other than OPTIONS where
     * authentication information cannot be expected to be provided, yet as
     * no content will be delivered, the request shall not fail for lack of
     * proper authentication.
     */
    @Override
    protected boolean isEnabled(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        final boolean result;
        if (request instanceof HttpServletRequest && ((HttpServletRequest) request).getMethod().equals("OPTIONS")) {
            result = false;
        } else {
            result = super.isEnabled(request, response);
        }
        return result;
    }

}
