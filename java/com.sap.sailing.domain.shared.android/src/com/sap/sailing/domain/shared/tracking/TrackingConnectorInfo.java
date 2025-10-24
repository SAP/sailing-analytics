
package com.sap.sailing.domain.shared.tracking;

import java.io.Serializable;

/**
 * Identifies the tracking connector that was used to create a TrackedRace.
 * Further the Connector can provide a webUrl, that leads to an event web page.
 */
public interface TrackingConnectorInfo extends Serializable {
    
    /**
     * gets the name associated with the tracking technology used for the Race
     */
    String getTrackingConnectorName();
    
    /**
     * gets a {@link String} representation of the default web-URL associated with the tracking technology used for the Race.
     * may be {@code null} if there is none provided in the adapter.
     */
    String getTrackingConnectorDefaultUrl();
    
    /**
     * gets a {@link String} representation of the web-URL associated with the Event. 
     * may be {@code null} if the API of the respective Tracking-Service does not provide a URL.
     */
    String getWebUrl();
}
