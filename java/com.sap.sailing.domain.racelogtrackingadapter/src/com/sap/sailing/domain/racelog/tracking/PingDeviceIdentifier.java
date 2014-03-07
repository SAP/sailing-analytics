package com.sap.sailing.domain.racelog.tracking;

import java.util.UUID;

/**
 * A device identifier used to identify a non-existent, virtual device, which is used only once as an
 * identifier to add a single fix to a track (i.e. pinging the location of that track's item).
 * 
 * @author Fredrik Teschke
 *
 */
public interface PingDeviceIdentifier extends DeviceIdentifier {
    UUID getId();
}
