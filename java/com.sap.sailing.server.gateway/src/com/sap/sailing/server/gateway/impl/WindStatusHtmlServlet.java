package com.sap.sailing.server.gateway.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;

import com.sap.sailing.domain.igtimiadapter.BulkFixReceiver;
import com.sap.sailing.domain.igtimiadapter.IgtimiWindListener;
import com.sap.sailing.domain.igtimiadapter.LiveDataConnection;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

/**
 * Shows the state of wind receivers regardless of them being attached to a race. 
 * Currently Expedition and Igtimi are supported.
 * 
 * @author Simon Marcel Pamies
 *
 */
public class WindStatusHtmlServlet extends WindStatusServlet implements IgtimiWindListener, BulkFixReceiver {

    private static final String SAILINGSERVER_WIND_STATUS = "/sailingserver/windStatus";
    private static final long serialVersionUID = 6091476602985063675L;
    private static final Logger logger = Logger.getLogger(WindStatusHtmlServlet.class.getName());
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        initializeWindReceiver(/* reinitialize requires authentication and must be POSTed to the master */ false);
        writePostRefreshingHeadAndBodyWithRefreshForm(req, resp, "Wind Status", SAILINGSERVER_WIND_STATUS);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean reinitializeWindReceiver = getReloadParamter(req);
        final String configureLocalServerPermission = getPermissionToReload();
        if (reinitializeWindReceiver) {
            SecurityUtils.getSubject().checkPermission(configureLocalServerPermission);
            logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" requested re-initialization of wind receivers");
            initializeWindReceiver(/* re-initialize */ true);
            writePostRefreshingHeadAndBodyWithRefreshForm(req, resp, "Wind Status", SAILINGSERVER_WIND_STATUS);
        } else {
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();
            out.println("<p>last refresh from server: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) + "</p>");
            out.println("<h3>Igtimi Wind Status ("+getIgtimiMessagesRawCount()+" raw messages received)</h3>");
            Map<LiveDataConnection, IgtimiConnectionInfo> igtimiConnections = getIgtimiConnections();
            if (!igtimiConnections.isEmpty()) {
                out.println("<h4>Igtimi accounts used</h4>");
                for (Map.Entry<LiveDataConnection, IgtimiConnectionInfo> entry: igtimiConnections.entrySet()) {
                    IgtimiConnectionInfo igtimiConnectionInfo = entry.getValue();
                    int deviceCount = Util.size(igtimiConnectionInfo.getDeviceIDs());
                    out.println(deviceCount + " devices " + igtimiConnectionInfo.getDeviceIDs().toString() + "<br/>");
                    final InetSocketAddress remoteAddress = igtimiConnectionInfo.getRemoteAddress();
                    out.println("Connection used is " + (remoteAddress == null ? "{null}" : remoteAddress.toString()) + "<br/><br/>");
                }
                out.println("<br/>");
            }
            if (getLastIgtimiMessages() != null && !getLastIgtimiMessages().isEmpty()) {
                for (Entry<String, Deque<IgtimiMessageInfo>> deviceAndMessagesList: getLastIgtimiMessages().entrySet()) {
                    final Deque<IgtimiMessageInfo> copyOfLastIgtimiMessages;
                    synchronized (deviceAndMessagesList.getValue()) {
                        copyOfLastIgtimiMessages = new ArrayDeque<>(deviceAndMessagesList.getValue());
                    }
                    out.println("Windbot: <b>" + deviceAndMessagesList.getKey() + "</b>");
                    if (copyOfLastIgtimiMessages.size() > 0) {
                        TimePoint latestTimePoint = copyOfLastIgtimiMessages.peek().getWind().getTimePoint(); 
                        long lastFixDiffInMs = System.currentTimeMillis() - latestTimePoint.asMillis();
                        out.println("&nbsp;&nbsp;&nbsp;&nbsp;Last fix:");
                        if (lastFixDiffInMs / 1000 < 60) {
                            out.println(lastFixDiffInMs / 1000 +"s ago");
                        } else {
                            out.println("<span style=\"color:red;\">" + lastFixDiffInMs / 1000 / 60 +"min ago</span>");
                        }
                    }
                    out.println("<br/>");
                    Iterator<IgtimiMessageInfo> messageIt = copyOfLastIgtimiMessages.iterator();
                    while (messageIt.hasNext()){
                        IgtimiMessageInfo message = messageIt.next();
                        out.println(message);
                        out.println("<br/>");
                    }
                    out.println("<br/>");
                }
            } else {
                if (getIgtimiMessagesRawCount() == 0) {
                    out.println("<i>No Igtimi messages received so far!</i>");
                } else {
                    out.println("<i>"+getIgtimiMessagesRawCount()+" Igtimi message bunch has been received but not enough messages to generate wind information.</i>");
                }
            }
            out.println("<h3>Expedition Wind Status</h3>");
            if (getLastExpeditionMessages() != null && !getLastExpeditionMessages().isEmpty()) {
                final List<ExpeditionMessageInfo> copyOfLastExpeditionMessages;
                synchronized (getLastExpeditionMessages()) {
                    copyOfLastExpeditionMessages = new ArrayList<>(getLastExpeditionMessages());
                }
                int expeditionMsgCounter = 0;
                for (ListIterator<ExpeditionMessageInfo> iterator = copyOfLastExpeditionMessages.listIterator(copyOfLastExpeditionMessages.size()); iterator.hasPrevious();) {
                    expeditionMsgCounter++;
                    ExpeditionMessageInfo message = iterator.previous();
                    out.println(message);
                    out.println("<br/>");
                    if (expeditionMsgCounter >= NUMBER_OF_MESSAGES_TO_SHOW) {
                        break;
                    }
                }
            } else {
                out.println("<i>No Expedition messages received so far!</i>");
            }
            out.close();
        }
    }
    
    private boolean getReloadParamter(HttpServletRequest req) {
        return Boolean.valueOf(req.getParameter(PARAM_RELOAD_WIND_RECEIVER));
    }

}
