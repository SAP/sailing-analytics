package com.sap.sailing.domain.racelogtracking;


/**
 * Handle serialization to a {@code String}, for passing the identifier to the GWT frontend.
 * Implementing this is not mandatory - if it were, there would be no need to have separate Mongo, JSON and other
 * serialization handlers.
 * 
 * This way, at least any device identifier that can be serialized to a string can also be exposed to the GWT frontend
 * (can both be displayed and especially be created there!).
 * 
 * @author Fredrik Teschke
 * 
 */
public interface DeviceIdentifierStringSerializationHandler extends DeviceIdentifierSerializationHandler<String> {
    
}
