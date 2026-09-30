package com.sap.sailing.domain.tracking;

import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.domain.abstractlog.race.RaceLogWindFixEvent;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

public class RaceLogWindFixDeclinationHelper {
    private static final Logger logger = Logger.getLogger(RaceLogWindFixDeclinationHelper.class.getName());
    
    public Wind getOptionallyDeclinationCorrectedWind(RaceLogWindFixEvent event) {
        Wind wind;
        if (event.isMagnetic()) {
            final Wind magneticNorthBasedWindFix = event.getWindFix();
            final DeclinationService declinationService = DeclinationService.INSTANCE;
            Declination declination;
            try {
                declination = declinationService.getDeclination(magneticNorthBasedWindFix.getTimePoint(),
                        magneticNorthBasedWindFix.getPosition(), /* timeoutForOnlineFetchInMilliseconds */
                        Duration.ONE_SECOND.times(5).asMillis());
                wind = new WindImpl(magneticNorthBasedWindFix.getPosition(), magneticNorthBasedWindFix.getTimePoint(),
                        new KnotSpeedWithBearingImpl(magneticNorthBasedWindFix.getKnots(), magneticNorthBasedWindFix
                                .getBearing().add(
                                        declination.getBearingCorrectedTo(magneticNorthBasedWindFix.getTimePoint()))));
            } catch (IOException | ParseException e) {
                logger.log(Level.SEVERE, "Error trying to correct magnetic wind fix " + magneticNorthBasedWindFix
                        + " by declination. " + "Using uncorrected magnetic wind direction instead.", e);
                wind = magneticNorthBasedWindFix;
            }
        } else {
            wind = event.getWindFix();
        }
        return wind;
    }
}
