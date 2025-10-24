package com.sap.sailing.domain.igtimiadapter.gateway.impl.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.igtimiadapter.gateway.impl.Activator;
import com.sap.sailing.domain.igtimiadapter.server.RiotWebsocketHandler;
import com.sap.sse.security.BearerTokenOrBasicOrFormAuthenticationFilter;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.util.ThreadPoolUtil;

@WebSocket
public class RiotWebsocketHandlerImpl implements RiotWebsocketHandler {
    private static final Logger logger = Logger.getLogger(RiotWebsocketHandlerImpl.class.getName());
    
    private User authenticatedUser;

    private Session session;
    
    private final Set<String> deviceSerialNumbers;

    private ScheduledFuture<?> heartbeatSendingTask;
    
    public RiotWebsocketHandlerImpl() {
        this.deviceSerialNumbers = new HashSet<>();
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info("Connect with session from "+session.getRemote());
        this.session = session;
        final UpgradeRequest upgradeRequest = session.getUpgradeRequest();
        final HttpServletRequest requestWrapper = new UpgradeRequestAsHttpServletRequestWrapper(upgradeRequest);
        final AuthenticationToken token = new BearerTokenOrBasicOrFormAuthenticationFilter() {
            @Override
            public AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
                // make the method public
                return super.createToken(request, response);
            }
        }.createToken(requestWrapper, /* response */ null);
        if (token != null) {
            try {
                SecurityUtils.getSubject().login(token);
            } catch (AuthenticationException e) {
                logger.info("No authentication provided for session "+session);
            }
        }
        final Subject subject = SecurityUtils.getSubject();
        logger.info("Subject: "+subject+" with principal "+subject.getPrincipal());
        final Activator activator = Activator.getInstance();
        authenticatedUser = activator.getSecurityService().getCurrentUser() == null ?
                activator.getSecurityService().getAllUser() : activator.getSecurityService().getCurrentUser();
        activator.getRiotServer().addWebSocketClient(this);
        heartbeatSendingTask = scheduleHeartbeat();
    }

    private ScheduledFuture<?> scheduleHeartbeat() {
        return ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor().scheduleAtFixedRate(this::sendHeartbeat, 15, 15, TimeUnit.SECONDS);
    }

    /**
     * Expects to receive either a heartbeat message that contains just the string "1" or
     * a JSON object that requests the devices the client is interested in with a single
     * field {@code "devices"} that has as its value an array of strings specifying the device
     * serial numbers. The latter will update the 
     */
    @OnWebSocketMessage
    public void onText(Session session, String text) throws ParseException {
        if (text.equals("1")) {
            handleHeartbeat();
        } else {
            // expecting a configuration message through which the client tells the
            // devices to the data of which it wants to subscribe; as a response
            // we'll send our server's current time as a Unix timestamp:
            session.getRemote().sendStringByFuture(""+System.currentTimeMillis());
            final JSONObject jsonObject = (JSONObject) new JSONParser().parse(text);
            final JSONArray devices = (JSONArray) jsonObject.get("devices");
            deviceSerialNumbers.clear();
            devices.forEach(d->deviceSerialNumbers.add((String) d));
            logger.info("Igtimi web socket client "+session+" registers for devices "+getDeviceSerialNumbers());
        }
    }

    private void handleHeartbeat() {
        logger.fine(()->"Received heartbeat for "+session);
    }

    private void sendHeartbeat() {
        try {
            sendString("1");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Exception trying to send heartbeat to client "+session.getRemote(), e);
        }
    }
    
    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        logger.info("Session "+session+" closed with statusCode "+statusCode+" for reason "+reason);
        Activator.getInstance().getRiotServer().removeWebSocketClient(this);
        heartbeatSendingTask.cancel(/* mayInterruptIfRunning */ true);
    }
    
    @Override
    public Set<String> getDeviceSerialNumbers() {
        return Collections.unmodifiableSet(deviceSerialNumbers);
    }

    @Override
    public User getAuthenticatedUser() {
        return authenticatedUser;
    }

    @Override
    public void sendBytes(ByteBuffer data) throws IOException {
        session.getRemote().sendBytes(data);
    }

    @Override
    public Future<Void> sendBytesByFuture(ByteBuffer data) {
        return session.getRemote().sendBytesByFuture(data);
    }

    @Override
    public void sendString(String text) throws IOException {
        session.getRemote().sendString(text);
    }

    @Override
    public Future<Void> sendStringByFuture(String text) {
        return session.getRemote().sendStringByFuture(text);
    }

    @Override
    public void flush() throws IOException {
        session.getRemote().flush();
    }
    
    @Override
    public void close(int statusCode, String reason) {
        session.close(statusCode, reason);
    }
    
    @Override
    public String toString() {
        return "Riot web socket for session from "+
                (session==null?"unknown target ":session.getRemote())+
                ", subcribed to devices "+getDeviceSerialNumbers()+
                " and with authenticated user "+(getAuthenticatedUser()==null?"null":getAuthenticatedUser().getName());
    }
}
