package com.sap.sailing.gwt.autoplay.client.nodes.base;

import java.util.function.Consumer;

import com.google.web.bindery.event.shared.EventBus;

public interface AutoPlayNode {
    String getName();
    void start(EventBus eventBus);
    void stop();

    void log(String logMessage);
    
    void onStart();
    
    default void onStop() {}
    
    /**
     * Will be called before onStart, before calling this the default duration is submitted. Override for custom duration.
     * A time can be supplied multiple times, the timer will restart from the time of submission
     */
    default void customDurationHook(Consumer<Integer> consumerOfDurationInSeconds) {
    };
}