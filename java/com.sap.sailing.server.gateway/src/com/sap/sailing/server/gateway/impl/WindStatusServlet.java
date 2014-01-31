package com.sap.sailing.server.gateway.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.domain.igtimiadapter.Account;
import com.sap.sailing.domain.igtimiadapter.BulkFixReceiver;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnection;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnectionFactory;
import com.sap.sailing.domain.igtimiadapter.IgtimiWindListener;
import com.sap.sailing.domain.igtimiadapter.LiveDataConnection;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.shared.IgtimiWindReceiver;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.expeditionconnector.ExpeditionListener;
import com.sap.sailing.expeditionconnector.ExpeditionMessage;
import com.sap.sailing.expeditionconnector.ExpeditionWindTrackerFactory;
import com.sap.sailing.expeditionconnector.UDPExpeditionReceiver;
import com.sap.sailing.server.gateway.SailingServerHttpServlet;

/**
 * Shows the state of wind receivers regardless of them being attached to a race. Currently Expedition and Igtimi are supported.
 * 
 * @author Simon Marcel Pamies
 *
 */
public class WindStatusServlet extends SailingServerHttpServlet implements IgtimiWindListener, BulkFixReceiver {
    private static final long serialVersionUID = -6791613843435003810L;
    
    private static List<ExpeditionMessageInfo> lastExpeditionMessages;
    
    private static int igtimiRawMessageCount;
    private static List<IgtimiMessageInfo> lastIgtimiMessages;
    private static IgtimiWindReceiver igtimiWindReceiver;
    private static LiveDataConnection liveDataConnection;

    private static boolean isExpeditionListenerRegistered;
    private static boolean isIgtimiListenerRegistered;
    
    public WindStatusServlet() {
        super();
        isExpeditionListenerRegistered = false;
        isIgtimiListenerRegistered = false;
    }
    
    private void initializeWindReceiver() {
        if(!isExpeditionListenerRegistered) {
            isExpeditionListenerRegistered = registerExpeditionListener();
            lastExpeditionMessages = new ArrayList<WindStatusServlet.ExpeditionMessageInfo>();
        }
        
        if (!isIgtimiListenerRegistered) {
            isIgtimiListenerRegistered = registerIgtimiListener();
            lastIgtimiMessages = new ArrayList<WindStatusServlet.IgtimiMessageInfo>();
            igtimiRawMessageCount = 0;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        initializeWindReceiver();
        resp.setContentType("text/html");

        PrintWriter out = resp.getWriter();
        
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Wind Status</title>");
        out.println("<meta http-equiv=refresh content=10>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h3>Igtimi Wind Status ("+igtimiRawMessageCount+" raw messages received)</h3><br/>");
        if (lastIgtimiMessages != null) {
            int counter = 0;
            for (ListIterator<IgtimiMessageInfo> iterator = WindStatusServlet.lastIgtimiMessages.listIterator(WindStatusServlet.lastIgtimiMessages.size()); iterator.hasPrevious();) {
                counter++;
                IgtimiMessageInfo message = iterator.previous();
                out.println(message);
                out.println("<br/>");
                if (counter >= 10) {
                    break;
                }
            }
        } else {
            if (igtimiRawMessageCount == 0) {
                out.println("<i>No Igtimi messages received so far!</i>");
            } else {
                out.println("<i>"+igtimiRawMessageCount+" Igtimi message bunch has been received but not enough messages to generate wind information.</i>");
            }
        }
        out.println("<h3>Expedition Wind Status</h3>");
        if (lastExpeditionMessages.size()>0) {
            for (ExpeditionMessageInfo message : lastExpeditionMessages) {
                out.println(message);
            }
        } else {
            out.println("<div>No Expedition messages received so far!</div>");
        }
        out.println("</body>");
        out.println("</html>");
        out.close();
    }

    private boolean registerIgtimiListener() {
        boolean result = false;
        ServiceTracker<IgtimiConnectionFactory, IgtimiConnectionFactory> igtimiServiceTracker = new ServiceTracker<IgtimiConnectionFactory, IgtimiConnectionFactory>(getContext(), IgtimiConnectionFactory.class, null);
        igtimiServiceTracker.open();
        IgtimiConnectionFactory igtimiConnectionFactory = igtimiServiceTracker.getService();
        for (Account account : igtimiConnectionFactory.getAllAccounts()) {
            if (account.getUser() != null) {
                IgtimiConnection igtimiConnection = igtimiConnectionFactory.connect(account);
                try {
                    liveDataConnection = igtimiConnection.createLiveConnection(igtimiConnection.getWindDevices());
                    igtimiWindReceiver = new IgtimiWindReceiver(igtimiConnection.getWindDevices());
                    igtimiWindReceiver.addListener(this);
                    liveDataConnection.addListener(igtimiWindReceiver);
                    liveDataConnection.addListener(this);
                    result = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private boolean registerExpeditionListener() {
        boolean result = false;
        try {
            ServiceTracker<ExpeditionWindTrackerFactory, ExpeditionWindTrackerFactory> expeditionServiceTracker = new ServiceTracker<ExpeditionWindTrackerFactory, ExpeditionWindTrackerFactory>(
                    getContext(), ExpeditionWindTrackerFactory.class.getName(), null);
            expeditionServiceTracker.open();
            UDPExpeditionReceiver receiver = expeditionServiceTracker.getService().getOrCreateWindReceiverOnDefaultPort();
            receiver.addListener(new ExpeditionListener() {
                @Override
                public void received(ExpeditionMessage message) {
                    if(message != null && message.getBoatID() >= 0) {
                        ExpeditionMessageInfo info = new ExpeditionMessageInfo();
                        info.boatID = message.getBoatID();
                        info.message = message;
                        info.messageReceivedAt = new Date();
                        lastExpeditionMessages.add(info);
                    }
                }
            } 
            , /*validMessagesOnly*/ false);            
            result = true;
        } catch (SocketException e) {
            result = false;
        }
        return result;
    }
    
    private class ExpeditionMessageInfo {
        Integer boatID;
        ExpeditionMessage message;
        Date messageReceivedAt;
        
        public String toString() {
            if (message.getTrueWind() != null) {
                return messageReceivedAt.toString() + ": [" + boatID + "] Knots: " + message.getTrueWind().getKnots() + " Bearing: " + message.getTrueWindBearing().getDegrees();
            }
            return messageReceivedAt.toString() + ": [" + boatID + "] " + message.getOriginalMessage();
        }
    }
    
    private class IgtimiMessageInfo {
        Wind wind;
        private String deviceSerialInfo;
        
        public IgtimiMessageInfo(Wind wind, String deviceSerialInfo) {
            this.wind = wind;
            this.deviceSerialInfo = deviceSerialInfo;
        }
        
        public String toString() {
            return deviceSerialInfo + ":" + wind.toString();
        }
    }

    @Override
    public void windDataReceived(Wind wind, String deviceSerialNumber) {
        lastIgtimiMessages.add(new IgtimiMessageInfo(wind, deviceSerialNumber));
    }
    
    @Override
    public void destroy() {
        if (liveDataConnection != null) {
            try {
                liveDataConnection.stop();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                liveDataConnection = null;
                isIgtimiListenerRegistered = false;
            }
        }
    }

    @Override
    public void received(Iterable<Fix> fixes) {
        igtimiRawMessageCount += 1;
    }
}
