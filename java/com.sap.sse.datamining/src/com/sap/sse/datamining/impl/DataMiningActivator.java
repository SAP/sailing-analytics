package com.sap.sse.datamining.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.sap.sse.datamining.ClassesWithFunctionsRegistrationService;
import com.sap.sse.datamining.DataMiningServer;
import com.sap.sse.datamining.functions.FunctionProvider;
import com.sap.sse.datamining.functions.FunctionRegistry;
import com.sap.sse.datamining.i18n.DataMiningStringMessages;
import com.sap.sse.datamining.impl.functions.RegistryFunctionProvider;
import com.sap.sse.datamining.impl.functions.SimpleFunctionRegistry;

public class DataMiningActivator implements BundleActivator, ClassesWithFunctionsRegistrationService {

//    private static final Logger LOGGER = Logger.getLogger(DataMiningActivator.class.getName());
    private static final int THREAD_POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 3);

    private static BundleContext context;
    
    private static DataMiningServer dataMiningServer;
    private static DataMiningStringMessages stringMessages;
    private static ThreadPoolExecutor executor;
    
    private static Collection<ServiceRegistration<?>> serviceRegistrations;

    @Override
    public void start(BundleContext context) throws Exception {
        DataMiningActivator.context = context;
        stringMessages = DataMiningStringMessages.Util.getDefaultStringMessages();
        executor = new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 60, TimeUnit.SECONDS,
                                          new LinkedBlockingQueue<Runnable>());

        FunctionRegistry functionRegistry = new SimpleFunctionRegistry();
        FunctionProvider functionProvider = new RegistryFunctionProvider(functionRegistry);
        dataMiningServer = new DataMiningServerImpl(stringMessages, functionRegistry, functionProvider);
        
        serviceRegistrations = new HashSet<>();
        registerDataMiningServer();
        registerClassesWithFunctionsRegistrationService();
    }

    private void registerClassesWithFunctionsRegistrationService() {
        serviceRegistrations.add(context.registerService(ClassesWithFunctionsRegistrationService.class, this, null));
    }

    private void registerDataMiningServer() {
        serviceRegistrations.add(context.registerService(DataMiningServer.class, dataMiningServer, null));
    }

    @Override
    public void registerInternalClassesWithMarkedMethods(Set<Class<?>> classesToScan) {
        dataMiningServer.getFunctionRegistry().registerAllWithInternalFunctionPolicy(classesToScan);
    }

    @Override
    public void registerExternalLibraryClasses(Set<Class<?>> externalClassesToScan) {
        dataMiningServer.getFunctionRegistry().registerAllWithExternalFunctionPolicy(externalClassesToScan);
    }

    @Override
    public void unregisterAllFunctionsOf(Set<Class<?>> classesToUnregister) {
        dataMiningServer.getFunctionRegistry().unregisterAllFunctionsOf(classesToUnregister);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        for (ServiceRegistration<?> serviceRegistration : serviceRegistrations) {
            context.ungetService(serviceRegistration.getReference());
        }
    }

    public static ThreadPoolExecutor getExecutor() {
        return executor;
    }
    
    public static DataMiningStringMessages getStringMessages() {
        return stringMessages;
    }
    
    public static BundleContext getContext() {
        return context;
    }

}
