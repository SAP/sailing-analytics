package com.sap.sailing.domain.igtimiadapter;

import com.sap.sailing.domain.igtimiadapter.datatypes.AWA;
import com.sap.sailing.domain.igtimiadapter.datatypes.AWS;
import com.sap.sailing.domain.igtimiadapter.datatypes.AntCbst;
import com.sap.sailing.domain.igtimiadapter.datatypes.AntHrm;
import com.sap.sailing.domain.igtimiadapter.datatypes.BatteryLevel;
import com.sap.sailing.domain.igtimiadapter.datatypes.COG;
import com.sap.sailing.domain.igtimiadapter.datatypes.File;
import com.sap.sailing.domain.igtimiadapter.datatypes.GpsAltitude;
import com.sap.sailing.domain.igtimiadapter.datatypes.GpsLatLong;
import com.sap.sailing.domain.igtimiadapter.datatypes.GpsQualityHdop;
import com.sap.sailing.domain.igtimiadapter.datatypes.GpsQualityIndicator;
import com.sap.sailing.domain.igtimiadapter.datatypes.GpsQualitySatCount;
import com.sap.sailing.domain.igtimiadapter.datatypes.HDG;
import com.sap.sailing.domain.igtimiadapter.datatypes.HDGM;
import com.sap.sailing.domain.igtimiadapter.datatypes.Log;
import com.sap.sailing.domain.igtimiadapter.datatypes.SOG;
import com.sap.sailing.domain.igtimiadapter.datatypes.STW;

public class IgtimiFixReceiverAdapter implements IgtimiFixReceiver {
    @Override
    public void received(AntCbst fix) {
    }

    @Override
    public void received(AntHrm fix) {
    }

    @Override
    public void received(AWA fix) {
    }

    @Override
    public void received(AWS fix) {
    }

    @Override
    public void received(BatteryLevel fix) {
    }

    @Override
    public void received(COG fix) {
    }

    @Override
    public void received(File fix) {
    }

    @Override
    public void received(GpsAltitude fix) {
    }

    @Override
    public void received(GpsLatLong fix) {
    }

    @Override
    public void received(GpsQualityHdop fix) {
    }

    @Override
    public void received(GpsQualityIndicator fix) {
    }

    @Override
    public void received(GpsQualitySatCount fix) {
    }

    @Override
    public void received(HDG fix) {
    }

    @Override
    public void received(HDGM fix) {
    }

    @Override
    public void received(SOG fix) {
    }

    @Override
    public void received(STW fix) {
    }

    @Override
    public void received(Log fix) {
    }
}
