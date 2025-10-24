package com.sap.sailing.domain.queclinkadapter.tracker;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

import com.sap.sailing.domain.queclinkadapter.FRIReport;
import com.sap.sailing.domain.queclinkadapter.HBDAcknowledgement;
import com.sap.sailing.domain.queclinkadapter.PDPReport;
import com.sap.sailing.domain.queclinkadapter.impl.AbstractMessageVisitor;
import com.sap.sailing.domain.queclinkadapter.impl.HBDServerAcknowledgementImpl;
import com.sap.sailing.domain.queclinkadapter.impl.PositionRelatedReportToGPSFixConverter;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;

public class MessageVisitorWithSensorFixStore<Backchannel> extends AbstractMessageVisitor<Void> {
    private final SensorFixStore sensorFixStore;
    private final MessageToDeviceSender messageToDeviceSender;
    private final ConcurrentMap<String, Backchannel> backchannelMapByImei;
    private final Backchannel backchannel;
    private final static PositionRelatedReportToGPSFixConverter gpsFixFactory = new PositionRelatedReportToGPSFixConverter();

    public MessageVisitorWithSensorFixStore(SensorFixStore sensorFixStore, MessageToDeviceSender messageToDeviceSender,
            ConcurrentMap<String, Backchannel> backchannelMapByImei, Backchannel backchannel) {
        super();
        this.sensorFixStore = sensorFixStore;
        this.messageToDeviceSender = messageToDeviceSender;
        this.backchannelMapByImei = backchannelMapByImei;
        this.backchannel = backchannel;
    }

    @Override
    public Void visit(FRIReport friReport) {
        backchannelMapByImei.putIfAbsent(friReport.getImei(), backchannel);
        gpsFixFactory.ingestFixesToStore(sensorFixStore, friReport);
        return null;
    }

    @Override
    public Void visit(PDPReport pdpConnectionReport) {
        backchannelMapByImei.putIfAbsent(pdpConnectionReport.getImei(), backchannel);
        return null;
    }
    
    @Override
    public Void visit(HBDAcknowledgement heartbeat) {
        try {
            messageToDeviceSender.sendToDevice(gpsFixFactory.getDeviceIdentifier(heartbeat), new HBDServerAcknowledgementImpl(heartbeat.getProtocolVersion(), heartbeat.getCountNumber()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
