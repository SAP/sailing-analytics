package com.sap.sailing.gwt.autoplay.client.nodes.base;

import java.util.logging.Logger;

import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.ResettableEventBus;

public abstract class AutoPlayNodeBase implements AutoPlayNode {
    private static final Logger LOG = Logger.getLogger(AutoPlaySequenceNode.class.getName());
    private ResettableEventBus bus;
    private boolean isStopped;
    private String name;

    public AutoPlayNodeBase(String name) {
        super();
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public final void start(EventBus bus) {
        isStopped = false;
        this.bus = new ResettableEventBus(bus);
        onStart();
    }

    @Override
    public final void stop() {
        isStopped = true;
        onStop();
        if (bus != null) {
            this.bus.removeHandlers();
        }
    }

    public boolean isStopped() {
        return isStopped;
    }

    protected EventBus getBus() {
        return bus;
    }

    @Override
    public void log(String logMessage) {
        StringBuilder msg = new StringBuilder();
        msg.append(">> AUTOPLAY ");
        msg.append(getName());
        msg.append(" >> ");
        msg.append(logMessage);
        LOG.info(msg.toString());
    }

}