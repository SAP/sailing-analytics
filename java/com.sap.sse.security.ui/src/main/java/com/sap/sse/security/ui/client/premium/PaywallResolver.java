package com.sap.sse.security.ui.client.premium;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.gwt.event.shared.HandlerRegistration;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.ui.client.UserStatusEventHandler;

/**
 * Paywall Resolver is needed to offer all services and functions related to the paywall.
 */
public interface PaywallResolver {
    
    /**
     * The registered provider from subscription service factory will be initialized.
     * 
     * @param userService
     *            user service to provide permission check and get user change event.
     * @param subscriptionServiceFactory
     *            subscription service factory to handle providers and provide subscription service.
     */
    public void getUnlockingSubscriptionPlans(final Action action, final SecuredDTO dtoContext,
            final Consumer<List<String>> callback);
    
    /**
     * Get the permission based on the given action and secured DTO context object. If no action is set the default
     * {@link DefaultActions}.READ will be used. 
     * 
     * @param action
     *            The action on which the permission check is based on.
     * @param dtoContext
     *            the secured DTO context on which the permission check is based on.
     * @return the permission check result
     */
    public boolean hasPermission(final Action action, final SecuredDTO dtoContext);
    
    /**
     * Get all HasPermissions of a set of actions.
     * 
     * @param premiumActions
     *            A set of premium actions.
     * @param dtoContext
     *            the context DTO (SecuredDTO)
     * 
     * @return a Map of HasPermissions results based on a set of premium {@link Action}s.
     */
    public Map<Action, Boolean> getHasPermissionMap(final Set<Action> premiumActions, final SecuredDTO dtoContext);
    
    /**
     * It is possible to register an event handler to get the user status change event and react on this.
     * 
     * @param handler
     *            an implementation of the {@link UserStatusEventHandler}.
     * @return the registration object.
     */
    public HandlerRegistration registerUserStatusEventHandler(final UserStatusEventHandler handler);
}
