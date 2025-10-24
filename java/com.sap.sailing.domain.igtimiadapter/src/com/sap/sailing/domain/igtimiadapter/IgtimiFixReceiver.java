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

/**
 * Callback interface for Igtimi fix receivers.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface IgtimiFixReceiver {
    void received(AntCbst fix);
    void received(AntHrm fix);
    void received(AWA fix);
    void received(AWS fix);
    void received(BatteryLevel fix);
    void received(COG fix);
    void received(File fix);
    void received(GpsAltitude fix);
    void received(GpsLatLong fix);
    void received(GpsQualityHdop fix);
    void received(GpsQualityIndicator fix);
    void received(GpsQualitySatCount fix);
    void received(HDG fix);
    void received(HDGM fix);
    void received(SOG fix);
    void received(STW fix);
    void received(Log fix);
}
