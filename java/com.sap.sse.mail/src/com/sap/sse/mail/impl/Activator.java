package com.sap.sse.mail.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.sap.sse.mail.MailService;
import com.sap.sse.osgi.CachedOsgiTypeBasedServiceFinderFactory;
import com.sap.sse.replication.Replicable;
import com.sap.sse.util.ClearStateTestSupport;

public class Activator implements BundleActivator {
    private static final Logger logger = Logger.getLogger(Activator.class.getName());

    private ServiceRegistration<?> registration;

    public void start(BundleContext context) throws Exception {
        // Load mail properties
        final String jettyHome = System.getProperty("jetty.home", "configuration");
        final File propertiesDir = new File(jettyHome).getParentFile();
        File propertiesfile = new File(propertiesDir, "mail.properties");
        Properties mailProperties = new Properties();
        try {
            mailProperties.load(new FileReader(propertiesfile));
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Couldn't read mail properties from " + propertiesfile.getCanonicalPath(), ioe);
        }
        MailService mailService = new MailServiceImpl(mailProperties, new MailServiceResolverAgainstOsgiRegistryImpl(
                new CachedOsgiTypeBasedServiceFinderFactory(context).createServiceFinder(MailService.class)));
        registration = context.registerService(MailService.class, mailService, null);
        final Dictionary<String, String> replicableServiceProperties = new Hashtable<>();
        replicableServiceProperties.put(Replicable.OSGi_Service_Registry_ID_Property_Name, mailService.getId()
                .toString());
        context.registerService(Replicable.class.getName(), mailService, replicableServiceProperties);
        context.registerService(ClearStateTestSupport.class.getName(), mailService, null);
        Logger.getLogger(Activator.class.getName()).info("Mail Service registered.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext bundleContext) throws Exception {
        registration.unregister();
    }

}
