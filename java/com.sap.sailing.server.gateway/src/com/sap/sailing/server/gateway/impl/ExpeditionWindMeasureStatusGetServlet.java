package com.sap.sailing.server.gateway.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.sailing.expeditionconnector.ExpeditionListener;
import com.sap.sailing.expeditionconnector.ExpeditionMessage;
import com.sap.sailing.server.gateway.SailingServerHttpServlet;

public class ExpeditionWindMeasureStatusGetServlet extends SailingServerHttpServlet implements ExpeditionListener {
    private static final long serialVersionUID = -6791613843435009810L;

    private Map<Integer, ExpeditionMessageInfo> lastMessageInfosPerBoat;
    private boolean isExpeditionListenerRegistered;
    
    private static long MIN_TIME_SINCE_LAST_MESSAGE = 5 * 1000; // 5s; 
    private static long MAX_TIME_SINCE_LAST_MESSAGE = 24 * 60 * 60 * 1000; // 1 day 

    public ExpeditionWindMeasureStatusGetServlet() {
        super();
        lastMessageInfosPerBoat = new HashMap<Integer, ExpeditionMessageInfo>();
        isExpeditionListenerRegistered = false;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(!isExpeditionListenerRegistered) {
            isExpeditionListenerRegistered = registerExpeditionListener();
        }
        
        resp.setContentType("text/html");

        PrintWriter out = resp.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Expedition Wind Status</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h3>Expedition Wind Status</h3>");
        out.println("<input type='button' value='Refresh' onClick='window.location.reload()'><br/>");
       
        if(lastMessageInfosPerBoat.size() > 0) {
            Date now = new Date(); 
            List<Integer> messagesToDrop = new ArrayList<Integer>();
            for(ExpeditionMessageInfo info: lastMessageInfosPerBoat.values()) {
                out.println("Boat-No:" + "&nbsp;" + info.boatID);
                out.println("<br/>");
                long timeSinceLastMessageInMs = now.getTime() - info.messageReceivedAt.getTime();
                if(timeSinceLastMessageInMs > MAX_TIME_SINCE_LAST_MESSAGE) {
                    messagesToDrop.add(info.boatID);
                } else if(timeSinceLastMessageInMs > MIN_TIME_SINCE_LAST_MESSAGE) {
                    long hours = TimeUnit.MILLISECONDS.toHours(timeSinceLastMessageInMs);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(timeSinceLastMessageInMs) - TimeUnit.HOURS.toMinutes(hours);
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(timeSinceLastMessageInMs) - (TimeUnit.MINUTES.toSeconds(minutes) + TimeUnit.HOURS.toSeconds(hours));
                    out.println("Time since last message:" + "&nbsp;" + String.format("%02d:%02d:%02d", hours, minutes, seconds));
                    out.println("<br/>");
                }
                out.println("Last message received:" + "&nbsp;" + info.messageReceivedAt.toString());
                out.println("<br/>");
                out.println("Last message:" + "&nbsp;" + info.message.getOriginalMessage());
                out.println("<br/><br/>");
            }
            for(Integer boatID: messagesToDrop) {
                lastMessageInfosPerBoat.remove(boatID);
            }
        } else {
            out.println("No expedition wind sources available.");
        }
        
        out.println("</body>");
        out.println("</html>");
        out.close();
    }

    private boolean registerExpeditionListener() {
        boolean result = false;
        try {
            getService().addExpeditionListener(this, false);
            result = true;
        } catch (SocketException e) {
            result = false;
        }
        return result;
    }
    
    @Override
    public void received(final ExpeditionMessage message) {
        if(message != null && message.getBoatID() >= 0) {
            ExpeditionMessageInfo info = new ExpeditionMessageInfo();
            info.boatID = message.getBoatID();
            info.message = message;
            info.messageReceivedAt = new Date();
            lastMessageInfosPerBoat.put(info.boatID, info);
        }
    }

    private class ExpeditionMessageInfo {
        Integer boatID;
        ExpeditionMessage message;
        Date messageReceivedAt;
    }
}
