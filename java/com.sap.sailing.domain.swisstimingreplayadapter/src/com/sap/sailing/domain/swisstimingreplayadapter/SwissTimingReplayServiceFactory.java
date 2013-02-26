package com.sap.sailing.domain.swisstimingreplayadapter;

import com.sap.sailing.domain.swisstimingreplayadapter.impl.SwissTimingReplayServiceFactoryImpl;

public interface SwissTimingReplayServiceFactory {
    static SwissTimingReplayServiceFactory INSTANCE = new SwissTimingReplayServiceFactoryImpl();
    
    SwissTimingReplayService createSwissTimingReplayService();
}
