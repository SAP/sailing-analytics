package com.sap.sse.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.sap.sse.rest.CORSFilterConfiguration;
import com.sap.sse.rest.impl.CORSFilterConfigurationImpl;

/**
 * Registers a shutdown hook that gracefully stops the OSGi framework by
 * grabbing the system bundle and stopping it.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class Activator implements BundleActivator {
    private static final Logger logger = Logger.getLogger(Activator.class.getName());
    
    private static Activator INSTANCE;
    
    private final CORSFilterConfiguration corsFilterConfiguration;
    
    public Activator() {
        INSTANCE = this;
        corsFilterConfiguration = new CORSFilterConfigurationImpl();
    }
    
    public static Activator getInstance() {
        return INSTANCE;
    }
    
    public CORSFilterConfiguration getCORSFilterConfiguration() {
        return corsFilterConfiguration;
    }
    
    @Override
    public void start(BundleContext context) throws Exception {
        INSTANCE = this;
        context.registerService(CORSFilterConfiguration.class, corsFilterConfiguration, /* properties */ null);
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            logger.info("Executing shutdown hook, gracefully shutting down OSGi framework");
            try {
                // TODO figure out if we're in the middle of a graceful shutdown already and then skip this...
                final Bundle systemBundle = context.getBundle(0);
                if (systemBundle == null) {
                    logger.warning("Couldn't find system bundle (anymore?). Perhaps already shut down.");
                } else {
                    logger.info("Found system bundle "+systemBundle.getSymbolicName());
                    systemBundle.stop();
                }
            } catch (BundleException e) {
                logger.log(Level.WARNING, "Exception while trying to gracefully stop the OSGi framework", e);
            }
        }, "com.sap.sse shutdown hook"));
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
