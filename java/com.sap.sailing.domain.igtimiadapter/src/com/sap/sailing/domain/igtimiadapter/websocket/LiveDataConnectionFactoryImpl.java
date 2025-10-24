package com.sap.sailing.domain.igtimiadapter.websocket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.sap.sailing.domain.igtimiadapter.IgtimiConnection;
import com.sap.sailing.domain.igtimiadapter.LiveDataConnection;
import com.sap.sse.common.Util;

public class LiveDataConnectionFactoryImpl implements LiveDataConnectionFactory {
    private static final Logger logger = Logger.getLogger(LiveDataConnectionFactoryImpl.class.getName());
    private final IgtimiConnection connection;
    private final Map<Set<String>, LiveDataConnection> dataConnectionsForDeviceSerialNumbers;
    private final Map<LiveDataConnection, Set<String>> deviceSerialNumersForDataConnections;
    private final Map<LiveDataConnection, Integer> usageCounts;

    public LiveDataConnectionFactoryImpl(IgtimiConnection connection) {
        this.connection = connection;
        dataConnectionsForDeviceSerialNumbers = new HashMap<>();
        deviceSerialNumersForDataConnections = new HashMap<>();
        usageCounts = new HashMap<>();
    }
    
    @Override
    public synchronized LiveDataConnection getOrCreateLiveDataConnection(Iterable<String> deviceSerialNumbers) throws Exception {
        final LiveDataConnection finalResult;
        if (deviceSerialNumbers == null || Util.isEmpty(deviceSerialNumbers)) {
            logger.info("Not creating a live Igtimi data connection for an empty set of device serial numbers through connection "+connection);
            finalResult = null;
        } else {
            final Set<String> deviceSerialNumbersAsSet = new HashSet<>();
            Util.addAll(deviceSerialNumbers, deviceSerialNumbersAsSet);
            LiveDataConnection result = dataConnectionsForDeviceSerialNumbers.get(deviceSerialNumbersAsSet);
            if (result == null) {
                logger.info("Didn't find an existing Igtimi LiveDataConnection for devices "+deviceSerialNumbersAsSet+"; creating one...");
                result = new WebSocketConnectionManager(connection, deviceSerialNumbers);
                dataConnectionsForDeviceSerialNumbers.put(deviceSerialNumbersAsSet, result);
                deviceSerialNumersForDataConnections.put(result, deviceSerialNumbersAsSet);
            } else {
                logger.info("Found an existing Igtimi LiveDataConnection for devices "+deviceSerialNumbersAsSet+"; using it.");
            }
            Integer usageCount = usageCounts.get(result);
            if (usageCount == null) {
                usageCount = 0;
            }
            usageCount++;
            usageCounts.put(result, usageCount);
            finalResult = new LiveDataConnectionWrapper(this, result);
        }
        return finalResult;
    }

    public synchronized void stop(LiveDataConnection actualConnection) throws Exception {
        Integer usageCount = usageCounts.get(actualConnection);
        if (usageCount == null || usageCount == 0) {
            logger.warning("Strange: the Igtimi live data connection "+actualConnection+" is released by another client although no client should be using it anymore.");
        } else {
            usageCount--;
            if (usageCount == 0) {
                usageCounts.remove(actualConnection);
                Set<String> deviceSerialNumbersAsSet = deviceSerialNumersForDataConnections.remove(actualConnection);
                dataConnectionsForDeviceSerialNumbers.remove(deviceSerialNumbersAsSet);
                actualConnection.stop();
            } else {
                usageCounts.put(actualConnection, usageCount);
            }
        }
    }

}
