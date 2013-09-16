package com.sap.sailing.gwt.ui.client;

import java.util.Date;

import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;

public class RaceTimesCalculationUtil {

    public static Pair<Date, Date> caluclateRaceMinMax(Timer timer, RaceTimesInfoDTO raceTimesInfo) {
        Date min = null;
        Date max = null;

        switch (timer.getPlayMode()) {
        case Live:
            if (raceTimesInfo.startOfRace != null) {
                long extensionTime = calculateRaceExtensionTime(raceTimesInfo.startOfRace, raceTimesInfo.newestTrackingEvent);
                
                min = new Date(raceTimesInfo.startOfRace.getTime() - extensionTime);
            } else if (raceTimesInfo.startOfTracking != null) {
                min = raceTimesInfo.startOfTracking;
            }
            
            if (raceTimesInfo.newestTrackingEvent != null) {
                max = raceTimesInfo.newestTrackingEvent;
            }
            break;
        case Replay:
            long extensionTime = calculateRaceExtensionTime(raceTimesInfo.startOfRace, raceTimesInfo.endOfRace);
            
            if (raceTimesInfo.startOfRace != null) {
                min = new Date(raceTimesInfo.startOfRace.getTime() - extensionTime);
            } else if (raceTimesInfo.startOfTracking != null) {
                min = raceTimesInfo.startOfTracking;
            }
            
            if (raceTimesInfo.endOfRace != null) {
                max = new Date(raceTimesInfo.endOfRace.getTime() + extensionTime);
            } else if (raceTimesInfo.newestTrackingEvent != null) {
                max = raceTimesInfo.newestTrackingEvent;
            }
            break;
        }
        
        return new Pair<Date, Date>(min, max);
    }
    
    private static long calculateRaceExtensionTime(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return 5 * 60 * 1000; //5 minutes
        }
        
        long minExtensionTime = 60 * 1000; // 1 minute
        long maxExtensionTime = 10 * 60 * 1000; // 10 minutes
        double extensionTimeFactor = 0.1; // 10 percent of the overall race length
        long extensionTime = (long) ((endTime.getTime() - startTime.getTime()) * extensionTimeFactor);
        
        return extensionTime < minExtensionTime ? minExtensionTime : extensionTime > maxExtensionTime ? maxExtensionTime : extensionTime;
    }

}
