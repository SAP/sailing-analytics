package com.sap.sse.security.ui.server;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.impl.User;

/**
 * {@link Filter ServletFilter} implementation that sets a short living {@link Cookie} that carries a user's preferred
 * locale if one is logged in. This allows the GWT UI to be shown in the user's preferred language without the need to
 * rewrite the URL with the locale parameter added. This also ensures that shared URLs are locale-independent unless a
 * user manually selects a specific locale for a page.
 */
public class LocaleInjectionFilter implements Filter {
    private ServiceTracker<SecurityService, SecurityService> securityServiceTracker;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (securityServiceTracker == null) {
            // It would be preferable to do this in init, but somehow, the context is sometimes null
            final BundleContext context = Activator.getContext();
            securityServiceTracker = new ServiceTracker<>(context, SecurityService.class, /* customizer */ null);
            securityServiceTracker.open();
        }
        
        final HttpServletResponse resp = (HttpServletResponse) response;
        final Subject currentSubject = SecurityUtils.getSubject();
        if (currentSubject != null) {
            final Object principal = currentSubject.getPrincipal();
            if (principal instanceof String) {
                final SecurityService service = securityServiceTracker.getService();
                if (service != null) {
                    final User user = service.getUserByName((String) principal);
                    if (user != null) {
                        final Locale preferredLocale = user.getLocale();
                        if (preferredLocale != null) {
                            final Cookie cookie = new Cookie(UserManagementConstants.LOCALE_COOKIE_NAME,
                                    preferredLocale.toLanguageTag());
                            cookie.setMaxAge(-1);
                            cookie.setPath("/");
                            cookie.setSecure(true);
                            resp.addCookie(cookie);
                        }
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        if (securityServiceTracker != null) {
            securityServiceTracker.close();
            securityServiceTracker = null;
        }
    }

}
