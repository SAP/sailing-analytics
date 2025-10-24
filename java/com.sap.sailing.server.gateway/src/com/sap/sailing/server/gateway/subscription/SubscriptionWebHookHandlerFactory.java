package com.sap.sailing.server.gateway.subscription;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.server.gateway.subscription.chargebee.ChargebeeWebHookHandler;

/**
 * Register and return subscription webhook handler instance
 */
public class SubscriptionWebHookHandlerFactory {
    private static final Logger logger = Logger.getLogger(SubscriptionWebHookHandlerFactory.class.getName());
    private static SubscriptionWebHookHandlerFactory instance;

    public static SubscriptionWebHookHandlerFactory getInstance() {
        if (instance == null) {
            instance = new SubscriptionWebHookHandlerFactory();
        }
        return instance;
    }

    private Map<String, Class<? extends SubscriptionWebHookHandler>> handlers = new HashMap<String, Class<? extends SubscriptionWebHookHandler>>();

    private SubscriptionWebHookHandlerFactory() {
        registerSubscriptionHandlers();
    }

    /**
     * Return webhook handler for request path, return null in case handler could not be found for the request
     */
    public SubscriptionWebHookHandler getHandlerForPath(String path, SubscriptionWebHookServlet context) {
        final Class<? extends SubscriptionWebHookHandler> handlerCls = handlers.get(path);
        SubscriptionWebHookHandler handler = null;
        if (handlerCls != null) {
            // New instance of handler should be used for each request so handler object would be thread-safe for any
            // specific implementation
            handler = createHandlerInstance(handlerCls);
            if (handler != null) {
                handler.setServletContext(context);
            }
        }
        return handler;
    }

    /**
     * Register all webhook handlers
     */
    private void registerSubscriptionHandlers() {
        registerHandler(ChargebeeWebHookHandler.class);
    }

    private void registerHandler(Class<? extends SubscriptionWebHookHandler> handlerCls) {
        final SubscriptionWebHookHandler inst = createHandlerInstance(handlerCls);
        if (inst != null) {
            handlers.put(inst.getHandlerPath(), handlerCls);
        }
    }

    private SubscriptionWebHookHandler createHandlerInstance(Class<? extends SubscriptionWebHookHandler> handlerCls) {
        SubscriptionWebHookHandler inst = null;
        try {
            Constructor<? extends SubscriptionWebHookHandler> cons = handlerCls.getConstructor();
            inst = cons.newInstance();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to create subscription webhook handler instance", e);
        }
        return inst;
    }
}
