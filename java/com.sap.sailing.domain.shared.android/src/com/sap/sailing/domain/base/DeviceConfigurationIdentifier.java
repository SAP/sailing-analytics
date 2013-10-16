package com.sap.sailing.domain.base;

/**
 * Each tablet configuration is bound to a specific client. Use this interface instead of a plain {@link String} to
 * enable additions (like event-specific configurations).
 */
public interface DeviceConfigurationIdentifier {

    String getClientIdentifier();

}
