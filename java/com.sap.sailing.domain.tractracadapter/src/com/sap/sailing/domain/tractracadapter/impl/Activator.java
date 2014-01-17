package com.sap.sailing.domain.tractracadapter.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sap.sailing.domain.tractracadapter.TracTracAdapterFactory;

public class Activator implements BundleActivator {
    private static final Logger logger = Logger.getLogger(Activator.class.getName());
    
    private final TracTracAdapterFactory tracTracAdapterFactory;
    
    private boolean useTracTracMarkPassings = true;

    public Activator() {
        // there is exactly one instance of the racingEventService in the whole server
        tracTracAdapterFactory = new TracTracAdapterFactoryImpl();
    }
    
    public void start(BundleContext context) throws Exception {
        // register the racing service in the OSGi registry
        context.registerService(TracTracAdapterFactory.class.getName(), tracTracAdapterFactory, null);
        logger.log(Level.INFO, "Started "+context.getBundle().getSymbolicName());
        useTracTracMarkPassings = context.getProperty("tractrac.usemarkpassings") == null ? true : context.getProperty(
                "tractrac.usemarkpassings").equals("false") ? false : true;
    }
    
    public void stop(BundleContext context) throws Exception {
    }
    
    public boolean isUseTracTracMarkPassings() {
        return useTracTracMarkPassings;
    }
    
    public static Activator getInstance() {
        return new Activator();
    }
}
