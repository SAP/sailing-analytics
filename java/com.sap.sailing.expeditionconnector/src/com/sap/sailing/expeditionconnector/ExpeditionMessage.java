package com.sap.sailing.expeditionconnector;

import java.util.Set;

import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.udpconnector.UDPMessage;

public interface ExpeditionMessage extends UDPMessage {
    /* Here is a typical sequence of data as received from Expedition
       
#0,6,347.6,94,349.6,95,2.41*33
#0,4,133.9,5,2.12,6,348.5,94,349.8,95,2.41*09
#0,6,349.1,48,53.967217,49,10.888550,94,349.9,95,2.41*37
#0,94,350.1,95,2.41*22
#0,4,134.2,5,2.02,6,349.7*37
#0,94,350.3,95,2.41*20
#0,4,134.2,5,2.02,6,349.7,94,350.5,95,2.41*02
#0,6,349.9,94,350.6,95,2.41*3A
#0,4,134.2,5,2.02,6,348.5,94,350.8,95,2.40*0D
#0,94,350.5,95,2.37*27
#0,6,347.2,94,350.5,95,2.37*3D
#0,4,134.6,5,2.00,6,347.6,94,350.7,95,2.37*08
#0,6,343.5,94,350.8,95,2.37*33
#0,4,134.6,5,2.06,6,343.5,40,2.10,94,350.9,95,2.37*1E
#0,6,343.6,94,350.9,95,2.36*30
#0,94,350.9,95,2.36*2A
#0,4,134.6,5,2.06,6,343.7,94,350.9,95,2.36*04
#0,94,350.9,95,2.36*2A
#0,6,343.7,94,350.9,95,2.36*31
#0,4,134.6,5,2.06,6,343.7,94,350.9,95,2.36*04
#0,6,343.3,94,350.9,95,2.35*36
#0,4,135.0,5,2.06,6,343.7*3A
#0,6,343.3,48,53.967217,49,10.888533,94,350.9,95,2.35*31
#0,94,350.9,95,2.35*29
#0,4,135.0,5,2.06,6,343.1,94,350.9,95,2.35*06
#0,94,350.9,95,2.35*29
#0,6,343.2,94,350.9,95,2.35*37
#0,4,134.6,5,2.06,6,342.8*33
#0,6,342.6,94,350.8,95,2.35*33
#0,94,350.9,95,2.35*29
#0,4,134.2,5,2.08,6,342.4,94,350.9,95,2.35*0F
#0,94,350.9,95,2.35*29
#0,4,134.2,5,2.08,6,343.4,94,350.8,95,2.35*0F
#0,40,2.10,94,350.8,95,2.35*31
#0,6,344.2,40,2.10,94,350.8,95,2.35*28
#0,4,135.0,5,2.06,6,345.5,94,350.8,95,2.35*05
     */
    /**
     * variable ID for the GPS-measured latitude, in decimal degrees
     */
    final int ID_GPS_LAT = 48;

    /**
     * variable ID for the GPS-measured longitude, in decimal degrees
     */
    final int ID_GPS_LNG = 49;
    
    /**
     * variable ID for the GPS-measured course over ground (CoG) in decimal degrees
     */
    final int ID_GPS_COG = 50;
    
    /**
     * variable ID for the GPS-measured speed over ground (SoG)
     */
    final int ID_GPS_SOG = 50;
    
    /**
     * variable ID for the GPS-measured time as days since 31.12.1899 UTC, meaning 1.0 is 1.1.1900 0:00:00 UTC
     */
    final int ID_GPS_TIME = 146;
    
    /**
     * variable ID for magnetic heading, meaning the keel's direction, in decimal degrees
     */
    final int ID_HEADING = 13;
    
    /**
     * variable ID for true wind angle, relative to keel, in decimal degrees
     */
    final int ID_TWA = 4;
    
    /**
     * Variable ID for what Expedition thinks is the true wind direction ("from"), in decimal degrees. Some
     * users prefer to not enter the current declination for a time / place into their Expedition
     * client. In this case, the values presented for this key are the magnetic wind direction instead.
     * They should then be corrected by adding the current declination.
     */
    final int ID_TWD = 6;
    
    /**
     * variable ID for true wind speed in decimal knots
     */
    final int ID_TWS = 5;
    
    /**
     * True wind direction over ground ("from") in decimal degrees, cleansed using the GPS device
     */
    final int ID_GWD = 94;
    
    /**
     * True wind speed over ground, cleansed using the GPS device
     */
    final int ID_GWS = 95;
            
    /**
     * A message's checksum determines whether the package is to be considered valid.
     */
    boolean isValid();

    /**
     * The ID of the boat that sent this message
     */
    int getBoatID();

    /**
     * Lists all variable IDs for which this message has a value
     */
    Set<Integer> getVariableIDs();

    /**
     * Tells if <code>variableID</code> appears in {@link #getVariableIDs()}.
     */
    boolean hasValue(int variableID);

    /**
     * If {@link #hasValue(int)} is <code>true</code> for <code>variableID</code>, the variable's value is returned.
     * Otherwise, an {@link IllegalArgumentException} is thrown.
     */
    double getValue(int variableID);
    
    GPSFix getGPSFix();
    
    GPSFixMoving getGPSFixMoving();
    
    /**
     * Returns what what Expedition thinks is the true wind direction, in decimal degrees. Some users prefer to not
     * enter the current declination for a time / place into their Expedition client. In this case, the values presented
     * for this key are the magnetic wind direction instead. They should then be corrected by adding the current
     * declination.
     */
    SpeedWithBearing getTrueWind();
    
    SpeedWithBearing getSpeedOverGround();

    Bearing getTrueWindBearing();

    Bearing getCourseOverGround();

    TimePoint getTimePoint();

    TimePoint getCreatedAt();

    /**
     * The original text string as received from the Expedition program, including trailing checksum.
     * 
     * @return for example <code>"#0,6,349.1,48,53.967217,49,10.888550,94,349.9,95,2.41*37"</code>
     */
    String getOriginalMessage();
}
