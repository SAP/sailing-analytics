package com.sap.sailing.gwt.ui.shared;

import java.util.Date;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ReplicaDTO implements IsSerializable {
    private String hostname;
    private String identifier;
    private Date registrationTime;
    private Map<String, Integer> operationCountByOperationClassName;
    private double averageNumberOfOperationsPerMessage;
    private long numberOfMessagesSent;
    private double averageMessageSizeInBytes;
    private long totalNumberOfBytesSent;
    private String[] replicableIdsAsStrings;
    ReplicaDTO() {}
    public ReplicaDTO(String hostname, Date registrationTime,
            String identifier, String[] replicableIdsAsStrings, Map<String, Integer> operationCountByOperationClassName,
            double averageNumberOfOperationsPerMessage, long numberOfMessagesSent, long totalNumberOfBytesSent, double averageMessageSizeInBytes) {
        this.hostname = hostname;
        this.identifier = identifier;
        this.replicableIdsAsStrings = replicableIdsAsStrings;
        this.registrationTime = registrationTime;
        this.operationCountByOperationClassName = operationCountByOperationClassName;
        this.averageNumberOfOperationsPerMessage = averageNumberOfOperationsPerMessage;
        this.numberOfMessagesSent = numberOfMessagesSent;
        this.averageMessageSizeInBytes = averageMessageSizeInBytes;
        this.totalNumberOfBytesSent = totalNumberOfBytesSent;
    }
    public String getHostname() {
        return hostname;
    }
    public Date getRegistrationTime() {
        return registrationTime;
    }
    public Map<String, Integer> getOperationCountByOperationClassName() {
        return operationCountByOperationClassName;
    }
    public double getAverageNumberOfOperationsPerMessage() {
        return averageNumberOfOperationsPerMessage;
    }
    public long getNumberOfMessagesSent() {
        return numberOfMessagesSent;
    }
    public long getNumberOfBytesSent() {
        return totalNumberOfBytesSent;
    }
    public double getAverageMessageSizeInBytes() {
        return averageMessageSizeInBytes;
    }
    public String getIdentifier() {
        return identifier;
    }
    public String[] getReplicableIdsAsStrings() {
        return replicableIdsAsStrings;
    }
}