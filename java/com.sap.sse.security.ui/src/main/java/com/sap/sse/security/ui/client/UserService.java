package com.sap.sse.security.ui.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.TakedownNoticeRequestContext;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.GenericSerializableSettings;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.ServerInfoDTO;
import com.sap.sse.gwt.client.Storage;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.media.TakedownNoticeService;
import com.sap.sse.gwt.client.xdstorage.CrossDomainStorage;
import com.sap.sse.gwt.client.xdstorage.CrossDomainStorageEvent;
import com.sap.sse.gwt.client.xdstorage.DelegatingCrossDomainStorageFuture;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.PermissionChecker;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.OwnershipDTO;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.shared.dto.StrippedUserGroupDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.ui.client.i18n.StringMessages;
import com.sap.sse.security.ui.oauth.client.util.ClientUtils;
import com.sap.sse.security.ui.shared.EssentialSecuredDTO;
import com.sap.sse.security.ui.shared.SecurityServiceSharingDTO;
import com.sap.sse.security.ui.shared.SuccessInfo;

/**
 * Encapsulates the current user, remembered as a {@link UserDTO} object. The current user is determined by a call to
 * the server which in turn considers the current session to determine if a valid user is attached to the session. The
 * {@link UserDTO} for the current user tells about the user's roles, the user name and, if known, the e-mail address.
 * <p>
 * 
 * The {@link UserDTO} object is {@link #updateUser(boolean) updated} once the {@link #login(String, String)} or
 * {@link #logout()} methods have completed, and registered handlers and, optionally, other instances of this
 * service living in other tabs or browser windows, are notified about the changes.<p>
 * 
 * Clients can subscribe to this service for changes of the current user, using
 * {@link #addUserStatusEventHandler(UserStatusEventHandler)}. They will be notified each time the user object is
 * fetched successfully from the server or when a log-out sets the user object to <code>null</code>.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class UserService implements TakedownNoticeService {
    private static final Logger logger = Logger.getLogger(UserService.class.getName());
    
    private static final StringMessages stringMessages = GWT.create(StringMessages.class);
    
    /**
     * Key for the HTML5 local store that can be used to notify other instances of this service in other tabs
     * and windows about changes in the currently logged-in user. 
     */
    private static final String LOCAL_STORAGE_UPDATE_KEY = "current-user-has-changed";
    
    /**
     * Storage key to remember when a user was authenticated or dismissed the login hint the last time.
     */
    protected static final String STORAGE_KEY_FOR_USER_LOGIN_HINT = "sse.ui.lastLoginOrSuppression";
    
    /**
     * Delay when the login hint will be shown next time after a user logged in or dismissed the message.
     */
    protected static final Duration SUPRESSION_DELAY = Duration.ONE_WEEK;
    
    private final UserManagementServiceAsync userManagementService;
    
    private final UserManagementWriteServiceAsync userManagementWriteService;

    private final Set<UserStatusEventHandler> handlers;

    private boolean userInitiallyLoaded = false;
    
    private boolean preAuthenticated = false;
    
    private UserDTO currentUser;

    private final String id;

    private UserDTO anonymousUser;
    
    private ServerInfoDTO serverInfo;
    
    private final Set<HasPermissions> allKnownHasPermissions;
    
    /**
     * The storage configured based on what {@link UserManagementServiceAsync#getSharingConfiguration(AsyncCallback)} has returned.
     */
    private final DelegatingCrossDomainStorageFuture crossDomainStorage;

    public UserService(UserManagementServiceAsync userManagementService, UserManagementWriteServiceAsync userManagementWriteService) {
        this.id = UUID.randomUUID().toString();
        this.userManagementService = userManagementService;
        this.userManagementWriteService = userManagementWriteService;
        handlers = new HashSet<>();
        allKnownHasPermissions = new HashSet<>();
        crossDomainStorage = new DelegatingCrossDomainStorageFuture();
        initializeCrossDomainStorage();
        Util.addAll(SecuredSecurityTypes.getAllInstances(), allKnownHasPermissions); // to start with...
        // ...but the server may know more because HasPermissionsProviders can register in the OSGi registry
        // dynamically, and the SecurityService exposes the results:
        userManagementService.getAllHasPermissions(new AsyncCallback<ArrayList<HasPermissions>>() {
            @Override
            public void onFailure(Throwable caught) {
                GWT.log("Error trying to obtain secured types: "+caught.getMessage());
            }

            @Override
            public void onSuccess(ArrayList<HasPermissions> result) {
                GWT.log("Loaded secured types "+result);
                allKnownHasPermissions.addAll(result);
            }
        });
        registerStorageEventHandler();
        updateUser(/* notifyOtherInstances */ false);
    }

    private void initializeCrossDomainStorage() {
        assert userManagementService != null;
        assert crossDomainStorage != null;
        userManagementService.getSharingConfiguration(new AsyncCallback<SecurityServiceSharingDTO>() {
            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(caught.getMessage(), NotificationType.ERROR);
            }

            @Override
            public void onSuccess(SecurityServiceSharingDTO result) {
                crossDomainStorage.setStorageToUse(CrossDomainStorage.create(result.getBaseUrlForCrossDomainStorage()));
            }
        });
    }
    
    /**
     * Use this instead of {@link Storage#getLocalStorageIfSupported()}. The resulting {@link CrossDomainStorage} object
     * is configured by the server settings for sharing or isolation of the security service. Note that the methods all
     * work asynchronously, talking to a callback that the caller needs to pass. It can also happen that first requests
     * initially have to wait a few milliseconds because the configuration needs to be loaded from the back-end first
     * (which happens immediately when this {@link UserService} is constructed), and if a shared storage is configured,
     * an {@code iframe} needs to be loaded, and request processing is delayed until the loading of that {@code iframe}
     * has completed.
     */
    public CrossDomainStorage getStorage() {
        return crossDomainStorage;
    }

    private void registerStorageEventHandler() {
        getStorage().addStorageEventHandler(new CrossDomainStorageEvent.Handler() {
            @Override
            public void onStorageChange(CrossDomainStorageEvent event) {
                logger.finest("Received storage event { key: "+event.getKey()+", newValue: "+event.getNewValue()+", oldValue: "+
                        event.getOldValue()+", url: "+event.getUrl());
                // ignore update events coming from this object itself
                if (LOCAL_STORAGE_UPDATE_KEY.equals(event.getKey()) && event.getNewValue() != null
                        && !event.getNewValue().isEmpty() && !event.getNewValue().equals(id.toString())) {
                    updateUser(/* Don't play endless ping-pong between instances! */ false);
                }
            }
        });
    }

    /**
     * Used to synchronize changes in the user status between all {@link UserService} instances across all browser
     * tabs/windows.
     */
    public void fireUserUpdateEvent() {
        getStorage().setItem(LOCAL_STORAGE_UPDATE_KEY, "", // force a change
            e->getStorage().setItem(LOCAL_STORAGE_UPDATE_KEY, id, null));
    }

    /**
     * Fetches the {@link UserDTO} for the currently signed-in user, identified by the current session, from the server.
     * Receiving a result or an error condition will {@link #fireUserUpdateEvent() fire an update event} to all other
     * instances of this class in other tabs / windows if <code>notifyOtherInstances</code> is <code>true</code>. All
     * {@link UserStatusEventHandler}s registered with this instance will be
     * {@link UserStatusEventHandler#onUserStatusChange(UserDTO) notified} in all cases.
     * 
     * @param notifyOtherInstances
     *            if <code>true</code>, other instances of this class will be notified about the result of the call
     */
    public void updateUser(final boolean notifyOtherInstances) {
        userManagementService.getCurrentUser(
                new MarkedAsyncCallback<Triple<UserDTO, UserDTO, ServerInfoDTO>>(new AsyncCallback<Triple<UserDTO, UserDTO, ServerInfoDTO>>() {
            @Override
            public void onSuccess(Triple<UserDTO, UserDTO, ServerInfoDTO> result) {
                setCurrentUser(result, notifyOtherInstances);
            }

            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(caught.getMessage(), NotificationType.ERROR);
            }
        }));
    }

    /**
     * Signs in a user with username and password. If successful, the {@link #getCurrentUser() current user} will be
     * updated with the user data. Otherwise, it will remain unchanged. This means in particular that any previously
     * signed-in user will remain to be signed in.
     */
    public void login(String username, String password, final AsyncCallback<SuccessInfo> callback) {
        userManagementService.login(username, password,
                new MarkedAsyncCallback<SuccessInfo>(new AsyncCallback<SuccessInfo>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(SuccessInfo result) {
                if (result.isSuccessful()) {
                    setCurrentUser(result.getUserDTO(), /* notify other instances */ true);
                }
                callback.onSuccess(result);
            }
        }));
    }

    public void verifySocialUser(final AsyncCallback<UserDTO> callback) throws Exception {
        final String authProviderName = ClientUtils.getAuthProviderNameFromCookie();
        logger.info("Verifying " + authProviderName + " user ...");
        userManagementService.verifySocialUser(ClientUtils.getCredential(),
                new MarkedAsyncCallback<Triple<UserDTO, UserDTO, ServerInfoDTO>>(new AsyncCallback<Triple<UserDTO, UserDTO, ServerInfoDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(Triple<UserDTO, UserDTO, ServerInfoDTO> result) {
                setCurrentUser(result, /* notifyOtherInstances */ true);
                        logger.info(authProviderName + " user '" + result.getA().getName() + "' is verified!\n");
                        callback.onSuccess(result.getA());
            }
        }));
    }

    public void logout() {
        userManagementService.logout(new AsyncCallback<SuccessInfo>() {
            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(stringMessages.couldNotSignOut(caught.getMessage()), NotificationType.ERROR);
            }
    
            @Override
            public void onSuccess(SuccessInfo result) {
                currentUser = null;
                preAuthenticated = false;
                notifyUserStatusEventHandlers(false);
                fireUserUpdateEvent();
            }
        });
    };

    /**
     * The user currently signed in, identified using the current web session, or <code>null</code> if no user is
     * currently signed in.
     */
    public UserDTO getCurrentUser() {
        return currentUser;
    }

    private void setCurrentUser(Triple<UserDTO, UserDTO, ServerInfoDTO> resultAndAnomynous, final boolean notifyOtherInstances) {
        if (resultAndAnomynous.getA() == null) {
            currentUser = null;
        } else {
            // we remember that a user was authenticated to suppress the hint for some time
            setUserLoginHintToStorage();
            currentUser = resultAndAnomynous.getA();
        }
        anonymousUser = resultAndAnomynous.getB();
        serverInfo = resultAndAnomynous.getC();
        preAuthenticated = (!userInitiallyLoaded && currentUser != null);
        userInitiallyLoaded = true;
        logger.info("User changed to "
                + (currentUser == null ? "No User" : (currentUser.getName() + " roles: " + currentUser.getRoles())));
        logger.info("User anonymous changed to " + (anonymousUser == null ? "No User" : (anonymousUser.getName() + " roles: " + anonymousUser.getRoles())));
        notifyUserStatusEventHandlers(preAuthenticated);
        if (notifyOtherInstances) {
            fireUserUpdateEvent();
        }
    }

    public void addUserStatusEventHandler(UserStatusEventHandler handler) {
        addUserStatusEventHandler(handler, false);
    }
    
    public void addUserStatusEventHandler(UserStatusEventHandler handler, boolean fireIfUserIsAlreadyAvailable) {
        handlers.add(handler);
        if (userInitiallyLoaded && fireIfUserIsAlreadyAvailable) {
            handler.onUserStatusChange(currentUser, preAuthenticated);
        }
    }

    public void executeWithServerInfo(Consumer<ServerInfoDTO> consumer) {
        this.addUserStatusEventHandler(new UserStatusEventHandler() {
            @Override
            public void onUserStatusChange(UserDTO u, boolean p) {
                consumer.accept(getServerInfo());
                removeUserStatusEventHandler(this);
            }
        }, /* fireIfUserIsAlreadyAvailable */ true);
    }

    public void removeUserStatusEventHandler(UserStatusEventHandler handler) {
        handlers.remove(handler);
    }

    private void notifyUserStatusEventHandlers(boolean preAuthenticated) {
        for (UserStatusEventHandler handler : new HashSet<>(handlers)) {
            handler.onUserStatusChange(getCurrentUser(), preAuthenticated);
        }
    }

    public UserManagementServiceAsync getUserManagementService() {
        return userManagementService;
    }
    
    public UserManagementWriteServiceAsync getUserManagementWriteService() {
        return userManagementWriteService;
    }
    
    /**
     * Loads the {@link #getCurrentUser() current user}'s preference with the given {@link String key} from server.
     * The preferences are passed to the {@link AsyncCallback} as serialized in {@link String}.
     * 
     * @param key
     *            key of the preference to load
     * @param callback
     *            {@link AsyncCallback} for GWT RPC call
     *            
     */
    public void getPreference(String key,
            final AsyncCallback<String> callback) {
        String username = getCurrentUser().getName();
        getUserManagementService().getPreference(username, key, callback);
    }
    
    public void getPreferences(List<String> keys,
            final AsyncCallback<Map<String, String>> callback) {
        String username = getCurrentUser().getName();
        getUserManagementService().getPreferences(username, keys, callback);
    }
    
    public void getAllPreferences(final AsyncCallback<Map<String, String>> callback) {
        String username = getCurrentUser().getName();
        getUserManagementService().getAllPreferences(username, callback);
    }
    
    /**
     * Sets the {@link #getCurrentUser() current user}'s preference with the given {@link String key} on server.
     * 
     * @param key
     *            key of the preference to set
     * @param serializedSettings
     *            Serialized settings as {@link String} containing the preferences
     */
    public void setPreference(String key, String serializedSettings, final AsyncCallback<Void> callback) {
        String username = getCurrentUser().getName();
        userManagementWriteService.setPreference(username, key, serializedSettings, callback);
    }
    
    public void setPreferences(Map<String, String> keyValuePairs,
            final AsyncCallback<Void> callback) {
        String username = getCurrentUser().getName();
        userManagementWriteService.setPreferences(username, keyValuePairs, callback);
    }
    
    /**
     * Unsets the {@link #getCurrentUser() current user}'s preference with the given key on server.
     * 
     * @param key key of the preference to unset
     *            
     * @see GenericSerializableSettings
     * @see AbstractGenericSerializableSettings
     */
    public void unsetPreference(String key) {
        String username = getCurrentUser().getName();
        userManagementWriteService.unsetPreference(username, key, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(caught.getMessage(), NotificationType.ERROR);
            }
            
            @Override
            public void onSuccess(Void result) {
            }
        });
    }
    
    /**
     * Unauthenticated users get a hint that it has benefits to create an account and log in. When a user was recently
     * logged in or dismissed the notification, he won't see the hint again for some time. This method checks if a user
     * was logged in or dismissed the message recently.
     * 
     * @param runnable
     *            called if the user was not recently logged in or dismissed the hint
     */
    public void runIfUserWasNotRecentlyLoggedInOrDismissedTheHint(Runnable runnable) {
        parseLastNewUserSupression(lastLoginOrSuppression->{
            if (lastLoginOrSuppression == null
                    || !lastLoginOrSuppression.plus(SUPRESSION_DELAY).before(MillisecondsTimePoint.now())) {
                runnable.run();
            }
        });
    }

    private void parseLastNewUserSupression(Consumer<TimePoint> callback) {
        getStorage().getItem(STORAGE_KEY_FOR_USER_LOGIN_HINT, stringValue->{
            try {
                if (stringValue != null) {
                    callback.accept(new MillisecondsTimePoint(Long.parseLong(stringValue)));
                } else {
                    callback.accept(null);
                }
            } catch (Exception e) {
                logger.warning("Error parsing localstore value '" + stringValue + "'");
                getStorage().removeItem(STORAGE_KEY_FOR_USER_LOGIN_HINT, v->callback.accept(null));
            }
        });
    }

    /**
     * Unauthenticated users get a hint that it has benefits to create an account and log in. When a user was recently
     * logged in or dismissed the notification, he won't see the hint again for some time. This method triggers the
     * suppression.
     */
    public void setUserLoginHintToStorage() {
        getStorage().setItem(STORAGE_KEY_FOR_USER_LOGIN_HINT, String.valueOf(MillisecondsTimePoint.now().asMillis()), /* callback */ null);
    }
    
    /**
     * Checks whether the user has the permission to the given action for the given object.
     */
    public boolean hasPermission(SecuredDTO securedDTO, Action action) {
        final boolean result;
        if (securedDTO == null) {
            result = false;
        } else {
            result = PermissionChecker.isPermitted(securedDTO.getIdentifier().getPermission(action), currentUser,
                anonymousUser, securedDTO.getOwnership(), securedDTO.getAccessControlList());
        }
        return result;
    }
    
    /**
     * From a {@link HasPermissions} permission type, an object name and the type-relative object identifier parts this
     * method constructs a full-fledged {@link SecuredDTO} and asks the server to fill in the corresponding security
     * information, in particular the ownership and ACL data. The {@link SecuredDTO} that the server has augmented this
     * way is sent to the {@code callback}'s {@link AsyncCallback#onSuccess(Object) onSuccess} method where it can,
     * e.g., be used for a permission check as in {@link #hasPermission(SecuredDTO, Action)}.
     * <p>
     * 
     * This is useful in case a full-fledged {@link SecuredDTO} is not available on the client for some entity, but all
     * information about its type and ID are available. Imagine, for example, a situation where an identifier for a race
     * is available on the client, and a permission check needs to be performed for the race identified this way.
     * Instead of implementing a service that returns the full {@code RaceDTO} with large amounts of data attached that
     * is not needed for the security check, this method can be used to obtain a proxy that is sufficient to check
     * whether the current user has the permission to execute a specific action.
     */
    public void createEssentialSecuredDTOByIdAndType(HasPermissions permissionType, String name, TypeRelativeObjectIdentifier typeRelativeObjectIdentifier, final AsyncCallback<SecuredDTO> callback) {
        final EssentialSecuredDTO secureDTO = new EssentialSecuredDTO(permissionType, name, typeRelativeObjectIdentifier);
        userManagementService.addSecurityInformation(secureDTO, new AsyncCallback<SecuredDTO>() {
            @Override
            public void onSuccess(SecuredDTO result) {
                callback.onSuccess(result);
            }
            
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }
        });
    }

    /**
     * Checks whether the user has the permission to the given action for the ServerInfoDTO.
     */
    public boolean hasServerPermission(Action action) {
        return hasPermission(this.serverInfo, action);
    }
    
    /**
     * Checks whether the user has the permission to the given action for the ServerInfoDTO.
     */
    public boolean hasAnyServerPermission(Action... actions) {
        boolean hasAnyPermission = false;
        for (Action action : actions) {
            if (hasCurrentUserAnyPermission(this.serverInfo.getIdentifier().getPermission(action), this.serverInfo.getOwnership())) {
                hasAnyPermission = true;
                break;
            }
        }
        return hasAnyPermission;
    }

    public boolean hasPermission(WildcardPermission permission, OwnershipDTO ownership) {
        return hasPermission(permission, ownership, /* acl */ null);
    }
    
    public boolean hasPermission(WildcardPermission permission, OwnershipDTO ownership, AccessControlListDTO acl) {
        if (anonymousUser == null) {
            return false;
        }
        return PermissionChecker.isPermitted(permission, currentUser, anonymousUser, ownership, acl);
    }

    /**
     * Checks whether the user has permission to {@link DefaultActions#CREATE create} an object of the logical type
     * specified, assuming that it will be created with this user as the {@link Ownership#getUserOwner() user owner} and
     * this user's {@link #getServerGroup() default group} as the {@link Ownership#getTenantOwner() group owner}.
     */
    public boolean hasCreatePermission(HasPermissions logicalSecuredObjectType) {
        if (currentUser == null) {
            return false;
        }
        return hasPermission(logicalSecuredObjectType.getPermission(DefaultActions.CREATE),
                new OwnershipDTO(currentUser == null ? null : currentUser.asStrippedUser(), getCurrentTenant()));
    }

    public StrippedUserGroupDTO getCurrentTenant() {
        return currentUser == null ? null : currentUser.getDefaultTenant();
    }

    public String getCurrentTenantName() {
        final StrippedUserGroupDTO defaultTenant = getCurrentTenant();
        return defaultTenant == null ? null : defaultTenant.getName();
    }

    public UserDTO getAnonymousUser() {
        return anonymousUser;
    }
    
    public boolean hasCurrentUserAnyPermission(WildcardPermission permissionToCheck, OwnershipDTO ownership) {
        return PermissionChecker.hasUserAnyPermission(permissionToCheck, allKnownHasPermissions, getCurrentUser(),
                anonymousUser, ownership);
    }
    
    public Iterable<HasPermissions> getAllKnownPermissions() {
        return allKnownHasPermissions;
    }

    public boolean hasCurrentUserPermissionToCreateObjectOfTypeWithoutServerCreateObjectPermissionCheck(
            HasPermissions type) {
        final WildcardPermission createPermission = type.getPermission(DefaultActions.CREATE);
        final OwnershipDTO ownershipOfNewlyCreatedObject = new OwnershipDTO(
                currentUser == null ? null : currentUser.asStrippedUser(), getCurrentTenant());
        return this.hasCurrentUserAnyPermission(createPermission, ownershipOfNewlyCreatedObject);
    }

    public boolean hasCurrentUserPermissionToCreateObjectOfType(HasPermissions type) {
        return hasServerPermission(ServerActions.CREATE_OBJECT) && hasCurrentUserPermissionToCreateObjectOfTypeWithoutServerCreateObjectPermissionCheck(type);
    }
    
    public boolean hasCurrentUserPermissionToDeleteAnyObjectOfType(HasPermissions type) {
        final WildcardPermission createPermission = type.getPermission(DefaultActions.DELETE);
        return this.hasCurrentUserAnyPermission(createPermission, null);
    }
    
    public boolean hasCurrentUserPermissionToUpdateAnyObjectOfType(HasPermissions type) {
        final WildcardPermission createPermission = type.getPermission(DefaultActions.UPDATE);
        return this.hasCurrentUserAnyPermission(createPermission, null);
    }
    
    public ServerInfoDTO getServerInfo() {
        return serverInfo;
    }

    @Override
    public void fileTakedownNotice(TakedownNoticeRequestContext takedownNoticeRequestContext) {
        userManagementWriteService.fileTakedownNotice(takedownNoticeRequestContext, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                GWT.log(caught.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
                GWT.log("successfully filed takedown notice");
            }
        });
    }

    @Override
    public boolean isEmailAddressOfCurrentUserValidated() {
        return getCurrentUser() != null && getCurrentUser().isEmailValidated();
    }

    @Override
    public String getCurrentUserName() {
        return getCurrentUser() == null ? null : getCurrentUser().getName();
    }
}
