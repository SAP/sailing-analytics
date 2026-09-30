package com.sap.sailing.server.trackfiles.common;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackimport.GPSFixImporter;
import com.sap.sse.common.SpeedWithBearing;

/**
 * When an importer only provides latitude/longitude positions but no course over ground (COG) and no speed over ground
 * (SOG) values, this class infers the COG/SOG values from the positions and timestamps of the basic fixes. Objects of this
 * class are stateful in the sense that each call to {@link #addFixAndInfer(com.sap.sailing.domain.trackimport.GPSFixImporter.Callback, boolean, GPSFix)}
 * remembers the fix just handled. The {@link #addFixAndInfer(com.sap.sailing.domain.trackimport.GPSFixImporter.Callback, boolean, GPSFix)} method
 * therefore needs to be invoked with the fixes in ascending time point order.<p>
 * 
 * If the fixes delivered to {@link #addFixAndInfer(com.sap.sailing.domain.trackimport.GPSFixImporter.Callback, boolean, GPSFix)} are
 * already of type {@link GPSFixMoving}, they are used as-is. Otherwise, except for the first fix which is dropped, COG/SOG values
 * will be inferred, a {@link GPSFixMoving} will be constructed and forwarded to the callback instance.
 * 
 * @author Fredrik Teschke
 * 
 */
public abstract class BaseGPSFixImporterImpl implements GPSFixImporter {
    private GPSFix previousFix;
    
    protected void addFixAndInfer(Callback callback, boolean inferSpeedAndBearing, GPSFix fix,
            TrackFileImportDeviceIdentifier device) {
        if (inferSpeedAndBearing && ! (fix instanceof GPSFixMoving)) {
            if (previousFix == null) {
                //have to infer speed and bearing, but this is the first fix -> drop it
                previousFix = fix;
                return;
            }
            SpeedWithBearing speedWithBearing = previousFix.getSpeedAndBearingRequiredToReach(fix);
            fix = new GPSFixMovingImpl(fix.getPosition(), fix.getTimePoint(), speedWithBearing, /* optionalTrueHeading */ null);
        }
        previousFix = fix;
        callback.addFix(fix, device);
    }
}
