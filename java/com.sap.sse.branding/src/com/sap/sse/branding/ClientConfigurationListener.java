package com.sap.sse.branding;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpServletRequest;

import com.sap.sse.branding.impl.Activator;
/**
 * JSP servlet is registered on *.html within web.xml . Use the following JSP expression 
 * <pre>applicationScope['clientConfigurationContext.variableName']}</pre> to get strings replaced within the page. The
 * variables listed below are available for replacements:
 * <table border="1">
 * <tr>
 * <th>Variablename</th>
 * <th>branded value</th>
 * <th>debranded/whitelabeled</th>
 * </tr>
 * <tr>
 * <td>"SAP"</td>
 * <td>"SAP&nbsp;"</td>
 * <td>""</td>
 * </tr>
 * <tr>
 * <td>"debrandingActive"</td>
 * <td>"false"</td>
 * <td>"true"</td>
 * </tr>
 * <tr>
 * <td>"whitelabeled"</td>
 * <td>""</td>
 * <td>"-whitelabeled"</td>
 * </tr>
 * </table>
 * <p>
 *
 * Register a the jsp servlet for all the URLs that produce such static pages that you'd like to run replacements on. Example
 * registration in a {@code web.xml} configuration file:
 * 
 * <pre>
 *   &lt;servlet-mapping&gt;
 *       &lt;servlet-name&gt;jsp&lt;/servlet-name&gt;
 *       &lt;url-pattern&gt;*.html&lt;/url-pattern&gt;
 *   &lt;/servlet-mapping&gt;
 * </pre>
 * <p>
 * 
 * 
 * @see com.sap.sailing.server.gateway.test.support.WhitelabelSwitchServlet
 * @author Georg Herdt
 *
 */
public class ClientConfigurationListener implements javax.servlet.ServletRequestListener {
    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        if (sre.getServletRequest().getScheme().startsWith("http")) {
            String path = ((HttpServletRequest) sre.getServletRequest()).getServletPath();
            if (path != null && (path.endsWith("/") || path.endsWith(".html"))) {
                final ServletContext ctx = sre.getServletContext();
                final String ctxDebrandingActive = (String) ctx.getAttribute("clientConfigurationContext.debrandingActive");
                final BrandingConfigurationService brandingConfigurationService = Activator.getDefaultBrandingConfigurationService();
                final boolean brandingActive = brandingConfigurationService.isBrandingActive();
                if (ctxDebrandingActive == null || !Boolean.toString(brandingActive).equalsIgnoreCase(ctxDebrandingActive)) {
                    createReplacementMap(brandingActive).forEach((k, v) -> {
                        sre.getServletContext().setAttribute("clientConfigurationContext." + k, v);
                    });
                }
            }
        }
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        // intentionally left blank
    }

    private Map<String, String> createReplacementMap(boolean brandingActive) {
        final Map<String, String> map = new HashMap<>();
        final String title;
        final String whitelabeled;
        if (brandingActive) {
            title = "SAP ";
            whitelabeled = "";
        } else {
            title = "";
            whitelabeled = "-whitelabeled";
        }
        map.put("SAP", title);
        map.put("debrandingActive", Boolean.toString(!brandingActive));
        map.put("whitelabeled", whitelabeled);
        return map;
    }
}
