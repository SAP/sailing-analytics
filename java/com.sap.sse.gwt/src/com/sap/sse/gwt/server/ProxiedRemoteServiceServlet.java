package com.sap.sse.gwt.server;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.sap.sse.common.Duration;
import com.sap.sse.common.HttpRequestHeaderConstants;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.util.HttpRequestUtils;

/**
 * Using GWT in a proxyfied environment can be tricky and leads to strange errors
 * when using GWT-RPC. This is due to the fact that GWT doesn't find the correct
 * serialization rule file. For non proxyfied environments you do not need to change
 * anything. In an Apache based env you need to add a line like the following to your
 * configuration: RequestHeader edit X-GWT-Module-Base sapsailing.com/(gwt/)? sapsailing.com/gwt/<p>
 * 
 * This servlet additionally measures the response time that this servlet takes for each request.
 * Should the response time exceed {@link #LOG_REQUESTS_TAKING_LONGER_THAN} then the request
 * will be logged.
 * 
 * @see RpcRequestBuilder#MODULE_BASE_HEADER
 * 
 * @author Simon Pamies (info@pamies.de)
 * @since Oct 10, 2012
 */
public abstract class ProxiedRemoteServiceServlet extends RemoteServiceServlet {

    private static final long serialVersionUID = 5379097888921157936L;
    
    private static final Duration LOG_REQUESTS_TAKING_LONGER_THAN = Duration.ONE_SECOND.times(2);
    
    private static final Logger logger = Logger.getLogger(ProxiedRemoteServiceServlet.class.getName());
    
    /**
     * The {@link #processCall(RPCRequest)} override in this class records the start and the end time points
     * of the processing of each request. The {@link #service(HttpServletRequest, HttpServletResponse)} method
     * uses this, after having delegated to the superclass method which calls {@link #processCall(RPCRequest)} and
     * performs response serialization afterwards. It can thus determine the actual processing duration as well
     * as the serialization durations. Should the total exceed {@link #LOG_REQUESTS_TAKING_LONGER_THAN}, the
     * request will be logged.
     */
    private static final ThreadLocal<Triple<RPCRequest, TimePoint, TimePoint>> processingStartAndFinishTime = new ThreadLocal<>();

    @Override
    protected SerializationPolicy doGetSerializationPolicy(
            HttpServletRequest request, String moduleBaseURL, String strongName) {
        String moduleBaseURLHdr = request.getHeader(RpcRequestBuilder.MODULE_BASE_HEADER);
        if (moduleBaseURLHdr != null) {
            moduleBaseURL = moduleBaseURLHdr;
        }
        return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
    }

    @Override
    protected void doUnexpectedFailure(Throwable e) {
        if ((e.getCause() != null && e.getCause().getClass().getName().matches("org\\.apache\\.shiro\\.authz\\..*[Aa]uth.*Exception")) ||
                (e instanceof SerializationException && e.getMessage().matches("Type 'org\\.apache\\.shiro\\.authz\\..*[Aa]uth.*Exception' was not included in the set of types which can be serialized.*"))) {
            final HttpServletResponse servletResponse = getThreadLocalResponse();
            try {
                servletResponse.reset();
            } catch (IllegalStateException ex) {
                throw new RuntimeException("Unable to report failure", e);
            }
            ServletContext servletContext = getServletContext();
            servletContext.log("Exception while dispatching incoming RPC call", e.getCause()==null?e:e.getCause());
            try {
                servletResponse.setContentType("text/plain");
                servletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                try {
                    servletResponse.getOutputStream().write((e.getCause()==null?e:e.getCause()).getLocalizedMessage().getBytes("UTF-8"));
                } catch (IllegalStateException ex) {
                    // Handle the (unexpected) case where getWriter() was previously used
                    servletResponse.getWriter().write("Couldn't write exception");
                }
            } catch (IOException ex) {
                servletContext.log(
                        "respondWithUnexpectedFailure failed while sending the previous failure to the client", ex);
            }
        } else {
            super.doUnexpectedFailure(e);
        }
    }
    
    protected Locale getClientLocale() {
        final HttpServletRequest request = getThreadLocalRequest();
        if (request != null) {
            final String localeString = request.getHeader(HttpRequestHeaderConstants.HEADER_KEY_LOCALE);
            if (localeString != null && ! localeString.isEmpty()) {
                try {
                    return Locale.forLanguageTag(localeString);
                } catch (Exception e) {
                    // non-parseable locales are ignored
                }
            }
        }
        // Using default locale if the client locale could not be determined
        return Locale.ENGLISH;
    }

    /**
     * Subclasses overriding this method must either delegate to this method or, if they don't, call
     * {@link #beforeProcessCall()} at the beginning and {@link #afterProcessCall(RPCRequest, TimePoint)} at the end of
     * the method, ideally in a {@code finally} clause, passing the result of the {@link #beforeProcessCall()} call as
     * the {@code startOfRequestProcessing} parameter.
     */
    @Override
    public String processCall(RPCRequest rpcRequest) throws SerializationException {
        final TimePoint startOfRequestProcessing = beforeProcessCall();
        try {
            return super.processCall(rpcRequest);
        } finally {
            afterProcessCall(rpcRequest, startOfRequestProcessing);
        }
    }

    protected TimePoint beforeProcessCall() {
        final TimePoint startOfRequestProcessing = MillisecondsTimePoint.now();
        return startOfRequestProcessing;
    }
    
    protected void afterProcessCall(RPCRequest rpcRequest, final TimePoint startOfRequestProcessing) {
        processingStartAndFinishTime.set(new Triple<>(rpcRequest, startOfRequestProcessing, MillisecondsTimePoint.now()));
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.service(req, resp);
        final TimePoint afterSendingResultToResponse = MillisecondsTimePoint.now();
        final Triple<RPCRequest, TimePoint, TimePoint> startAndEndOfProcessing = processingStartAndFinishTime.get();
        if (startAndEndOfProcessing == null) {
            // the processing time annotation happens only for POST requests in the doPost-->processCall-->afterProcessCall method chain
            if (req.getMethod().equals("POST")) {
                logger.warning("A request with method POST from address "+HttpRequestUtils.getClientIP(req)
                    +" was processed. No timing information available. Perhaps there was an IncompatibleRemoteServiceException thrown, so the call was not processed.");
            }
        } else {
            final Duration totalTime = startAndEndOfProcessing.getB().until(afterSendingResultToResponse);
            if (totalTime.compareTo(LOG_REQUESTS_TAKING_LONGER_THAN) > 0) {
                logRequest(startAndEndOfProcessing.getA(), startAndEndOfProcessing.getB(), startAndEndOfProcessing.getC(), afterSendingResultToResponse);
            }
            processingStartAndFinishTime.set(null);
        }
    }

    private void logRequest(RPCRequest request, TimePoint startOfProcessing, TimePoint endOfProcessing, TimePoint afterSendingResultToResponse) {
        final Duration processingDuration = startOfProcessing.until(endOfProcessing);
        final Duration sendingToResponseDuration = endOfProcessing.until(afterSendingResultToResponse);
        final Duration totalDuration = startOfProcessing.until(afterSendingResultToResponse);
        final String username;
        final Subject subject = SecurityUtils.getSubject();
        if (subject != null) {
            username = subject.getPrincipal() == null ? null : subject.getPrincipal().toString();
        } else {
            username = null;
        }
        logger.log(Level.WARNING, "GWT RPC Request "+request.getMethod()+
                " by user "+username+
                " with parameters "+getRequestParameterValues(request)+" on "+this+
                " took "+processingDuration+" to process, "+sendingToResponseDuration+" to send result into response, so "+
                totalDuration+" in total.");
    }

    /**
     * Try to hide parameter values if their name has any of "secret", "pass", "pw", "token", or "code" in its name, regardless
     * the case and where in the parameter name any of these sub-strings appear.
     */
    private List<Object> getRequestParameterValues(RPCRequest request) {
        final Method method = request.getMethod();
        final List<Object> result = new ArrayList<>();
        int i=0;
        for (final Parameter parameter : method.getParameters()) {
            if (!parameter.isNamePresent() || isSecuritySensitiveParameterName(parameter.getName())) {
                result.add("<hidden for security reasons>");
            } else {
                result.add(request.getParameters()[i]);
            }
            i++;
        }
        return result;
    }

    /**
     * A parameter name is considered security-sensitive if it has any of "secret", "pass", "pw", "token", or "code" in
     * its name, regardless the case and where in the parameter name any of these sub-strings appear.
     */
    private static final String[] SENSITIVE_PARAMETER_NAME_SUBSTRINGS = {"secret", "pass", "pw", "token", "code", "credentials"};
    private boolean isSecuritySensitiveParameterName(String name) {
        for (final String sensitiveSubstring : SENSITIVE_PARAMETER_NAME_SUBSTRINGS) {
            if (name.toLowerCase().contains(sensitiveSubstring)) {
                return true;
            }
        }
        return false;
    }
}
