package com.sap.sailing.server.gateway.impl;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.igtimiadapter.BulkFixReceiver;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnection;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnectionFactory;
import com.sap.sailing.domain.igtimiadapter.IgtimiWindListener;
import com.sap.sailing.domain.igtimiadapter.LiveDataConnection;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.shared.IgtimiWindReceiver;
import com.sap.sailing.expeditionconnector.ExpeditionListener;
import com.sap.sailing.expeditionconnector.ExpeditionMessage;
import com.sap.sailing.expeditionconnector.ExpeditionTrackerFactory;
import com.sap.sailing.expeditionconnector.UDPExpeditionReceiver;
import com.sap.sse.ServerInfo;
import com.sap.sse.common.Util;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;

/**
 * Shows the state of wind receivers regardless of them being attached to a race. Currently Expedition and Igtimi are supported.
 * 
 * @author Simon Marcel Pamies
 *
 */
public abstract class WindStatusServlet extends SailingServerHttpServletWithPostBasedContentReplacing implements IgtimiWindListener, BulkFixReceiver {
    private static final Logger logger = Logger.getLogger(WindStatusServlet.class.getName());
    private static final long serialVersionUID = -6791613843435003810L;
    
    protected static final String PARAM_RELOAD_WIND_RECEIVER="reloadWindReceiver";
    protected static final String REFRESHING_CONTENT_DIV_ID = "refreshingContent";

    protected final int NUMBER_OF_MESSAGES_TO_SHOW=100;
    protected final int NUMBER_OF_MESSAGES_PER_DEVICE_TO_SHOW=10;

    protected static final DecimalFormat decimalFormatter2Digits = new DecimalFormat("#.##");
    protected static final DecimalFormat decimalFormatter1Digit = new DecimalFormat("#.#");
    protected static final DecimalFormat latLngDecimalFormatter = new DecimalFormat("#.######");
    protected static final DateFormat dateTimeFormatter = DateFormat.getTimeInstance(DateFormat.LONG);
            
    private static List<ExpeditionMessageInfo> lastExpeditionMessages;
    
    private static Object lock = new Object();
    private static int igtimiRawMessageCount;
    private static Map<String, Deque<IgtimiMessageInfo>> lastIgtimiMessages;
    private static IgtimiWindReceiver igtimiWindReceiver;
    private static Map<LiveDataConnection, IgtimiConnectionInfo> igtimiConnections;
    
    private static boolean isExpeditionListenerRegistered;
    private static boolean isIgtimiListenerRegistered;
    
    public WindStatusServlet() {
        super();
        isExpeditionListenerRegistered = false;
        isIgtimiListenerRegistered = false;
        igtimiConnections = new LinkedHashMap<>();
    }
    
    protected int getIgtimiMessagesRawCount() {
        return igtimiRawMessageCount;
    }
    
    protected Map<String, Deque<IgtimiMessageInfo>> getLastIgtimiMessages() {
        return lastIgtimiMessages;
    }
    
    protected List<ExpeditionMessageInfo> getLastExpeditionMessages() {
        return lastExpeditionMessages;
    }
    
    protected void initializeWindReceiver(boolean reinitialize) {
        synchronized (lock) {
            if (!isExpeditionListenerRegistered || reinitialize) {
                isExpeditionListenerRegistered = registerExpeditionListener();
                lastExpeditionMessages = new ArrayList<WindStatusServlet.ExpeditionMessageInfo>();
            }
        }
        synchronized (lock) {
            if (!isIgtimiListenerRegistered || reinitialize) {
                if (reinitialize) {
                    if (!igtimiConnections.isEmpty()) {
                        for (Map.Entry<LiveDataConnection, IgtimiConnectionInfo> entry: igtimiConnections.entrySet()) {
                            LiveDataConnection igtimiConnection = entry.getKey();
                            try {
                                if (igtimiConnection != null) {
                                    igtimiConnection.stop();
                                    igtimiConnection.removeListener(igtimiWindReceiver);
                                    igtimiConnection.removeListener(this);
                                    
                                }
                            } catch (Exception e) {
                                logger.log(Level.WARNING, "Exception trying to stop Igtimi connection "+igtimiConnection, e);
                            }
                        }
                        igtimiConnections.clear();
                    }
                }
                lastIgtimiMessages = new HashMap<String, Deque<IgtimiMessageInfo>>();
                isIgtimiListenerRegistered = registerIgtimiListener();
                igtimiRawMessageCount = 0;
            }
        }
    }

    private boolean registerIgtimiListener() {
        boolean result = false;
        ServiceTracker<IgtimiConnectionFactory, IgtimiConnectionFactory> igtimiServiceTracker = new ServiceTracker<IgtimiConnectionFactory, IgtimiConnectionFactory>(getContext(), IgtimiConnectionFactory.class, null);
        igtimiServiceTracker.open();
        IgtimiConnectionFactory igtimiConnectionFactory = igtimiServiceTracker.getService();
        igtimiWindReceiver = new IgtimiWindReceiver(DeclinationService.INSTANCE);
        igtimiWindReceiver.addListener(this);
        IgtimiConnection igtimiConnection = igtimiConnectionFactory
                .getOrCreateConnection(() -> getSecurityService().getCurrentUser() == null ? null
                        : getSecurityService().getAccessToken(getSecurityService().getCurrentUser().getName()));
        try {
            final LiveDataConnection newIgtimiConnection = igtimiConnection.getOrCreateLiveConnection(igtimiConnection.getWindDevices());
            result = newIgtimiConnection != null;
            if (result) {
                newIgtimiConnection.addListener(igtimiWindReceiver);
                newIgtimiConnection.addListener(this);
                IgtimiConnectionInfo newIgtimiConnectionInfo = new IgtimiConnectionInfo(
                        newIgtimiConnection, igtimiConnection.getWindDevices());
                igtimiConnections.put(newIgtimiConnection, newIgtimiConnectionInfo);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception trying to stop Igtimi connection "+igtimiConnection, e);
        }
        return result;
    }

    private boolean registerExpeditionListener() {
        boolean result = false;
        try {
            ServiceTracker<ExpeditionTrackerFactory, ExpeditionTrackerFactory> expeditionServiceTracker = new ServiceTracker<ExpeditionTrackerFactory, ExpeditionTrackerFactory>(
                    getContext(), ExpeditionTrackerFactory.class.getName(), null);
            expeditionServiceTracker.open();
            UDPExpeditionReceiver receiver = expeditionServiceTracker.getService().getOrCreateWindReceiverOnDefaultPort();
            receiver.addListener(new ExpeditionListener() {
                @Override
                public void received(ExpeditionMessage message) {
                    if (message != null && message.getBoatID() >= 0) {
                        ExpeditionMessageInfo info = new ExpeditionMessageInfo();
                        info.boatID = message.getBoatID();
                        info.message = message;
                        info.messageReceivedAt = new Date();
                        synchronized (lastExpeditionMessages) {
                            lastExpeditionMessages.add(info);
                        }
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

    protected class IgtimiConnectionInfo {
        private final LiveDataConnection igtimiLiveConnection;
        private final Iterable<String> deviceIDs;
        
        public IgtimiConnectionInfo(LiveDataConnection newIgtimiConnection, Iterable<String> deviceIDs) {
            super();
            this.igtimiLiveConnection = newIgtimiConnection;
            final List<String> deviceIDsList = new ArrayList<>();
            this.deviceIDs = deviceIDsList;
            Util.addAll(deviceIDs, deviceIDsList);
        }

        public InetSocketAddress getRemoteAddress() {
            return igtimiLiveConnection.getRemoteAddress();
        }

        public Iterable<String> getDeviceIDs() {
            return deviceIDs;
        }
    }

    protected class ExpeditionMessageInfo {
        Integer boatID;
        ExpeditionMessage message;
        Date messageReceivedAt;
        
        public String toString() {
            if (message.getTrueWind() != null) {
                return messageReceivedAt.toString() + ": [" + boatID + "] Knots: " + message.getTrueWind().getKnots() + " From: " + getFromAsDegrees();
            }
            return messageReceivedAt.toString() + ": [" + boatID + "] " + message.getOriginalMessage();
        }

        private double getFromAsDegrees() {
            return message.getTrueWindBearing().reverse().getDegrees();
        }
    }
    
    protected class IgtimiMessageInfo {
        private final Wind wind;
        private final Set<Fix> fixesUsed;
        
        public IgtimiMessageInfo(Wind wind, Set<Fix> fixesUsed) {
            this.wind = wind;
            this.fixesUsed = fixesUsed;
        }
        
        public Wind getWind() {
            return wind;
        }
        
        public String toString() {
            final StringBuilder formattedInfo = new StringBuilder();
            if (wind.getTimePoint() != null) {
                formattedInfo.append("Time: " + dateTimeFormatter.format(wind.getTimePoint().asDate()));
            }
            if (wind.getPosition() != null) {
                formattedInfo.append(", Pos: " + latLngDecimalFormatter.format(wind.getPosition().getLatDeg()) + " "
                        + latLngDecimalFormatter.format(wind.getPosition().getLngDeg()));
            }
            formattedInfo.append(", Wind: " + decimalFormatter2Digits.format(wind.getKnots()) + "kn");
            if (wind.getFrom() != null) {
                formattedInfo.append(" from " + decimalFormatter1Digit.format(wind.getFrom().getDegrees()) + "&deg;");
            }
            final Iterable<Fix> sortedFixes = fixesUsed.stream().sorted((f1, f2)->f1.toString().compareTo(f2.toString()))::iterator;
            formattedInfo.append(Util.joinStrings(", ", sortedFixes));
            return formattedInfo.toString();
        }
    }

    @Override
    public void windDataReceived(Wind wind, Set<Fix> fixesUsed, String deviceSerialNumber) {
        Deque<IgtimiMessageInfo> messagesPerDevice = lastIgtimiMessages.get(deviceSerialNumber);
        if (messagesPerDevice == null) {
            messagesPerDevice = new ArrayDeque<>(NUMBER_OF_MESSAGES_PER_DEVICE_TO_SHOW);
            lastIgtimiMessages.put(deviceSerialNumber, messagesPerDevice);
        }
        synchronized (messagesPerDevice) {
            messagesPerDevice.addFirst(new IgtimiMessageInfo(wind, fixesUsed));
            if (messagesPerDevice.size() > NUMBER_OF_MESSAGES_PER_DEVICE_TO_SHOW) {
                messagesPerDevice.pollLast();
            }
        }
    }
    
    @Override
    public void destroy() {
        for (Map.Entry<LiveDataConnection, IgtimiConnectionInfo> entry: igtimiConnections.entrySet()) {
            LiveDataConnection igtimiConnection = entry.getKey();
            try {
                igtimiConnection.stop();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception trying to stop Igtimi connection "+igtimiConnection, e);
            }
        }
        igtimiConnections.clear();
        isIgtimiListenerRegistered = false;
    }

    @Override
    public void received(Iterable<Fix> fixes) {
        igtimiRawMessageCount += 1;
    }

    protected static String getPermissionToReload() {
        return SecuredSecurityTypes.SERVER.getStringPermissionForTypeRelativeIdentifier(
                SecuredSecurityTypes.ServerActions.CONFIGURE_LOCAL_SERVER, new TypeRelativeObjectIdentifier(ServerInfo.getName()));
    }

    public static Map<LiveDataConnection, IgtimiConnectionInfo> getIgtimiConnections() {
        return igtimiConnections;
    }
}
