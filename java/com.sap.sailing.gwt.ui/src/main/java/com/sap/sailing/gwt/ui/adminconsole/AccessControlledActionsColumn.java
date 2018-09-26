package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.sap.sailing.domain.common.dto.NamedSecuredObjectDTO;
import com.sap.sse.gwt.client.celltable.ImagesBarCell;
import com.sap.sse.gwt.client.celltable.ImagesBarColumn;
import com.sap.sse.security.shared.AccessControlList;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.shared.Ownership;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.shared.UserDTO;

public class AccessControlledActionsColumn<T extends NamedSecuredObjectDTO, S extends ImagesBarCell>
        extends ImagesBarColumn<T, S> {

    private final Map<String, Consumer<T>> nameToCallbackMap = new HashMap<>();
    private final Map<String, Action> nameToActionMap = new HashMap<>();

    private final UserService userService;
    private final BiFunction<Action, T, WildcardPermission> permissionFactory;

    public AccessControlledActionsColumn(final S imagesBarCell, final UserService userService,
            final HasPermissions type, final Function<T, String> idFactory) {
        super(imagesBarCell);
        this.userService = userService;
        this.permissionFactory = (action, object) -> type.getPermissionForObjects(action, idFactory.apply(object));
        this.setFieldUpdater((index, object, value) -> nameToCallbackMap.get(value).accept(object));
    }

    /**
     * Adds an action identified by the provided name which will always be accessible.
     * 
     * @param name
     *            {@link String} to identify the action
     * @param callback
     *            {@link Consumer} to execute when the action is triggered
     */
    public void addAction(final String name, final Consumer<T> callback) {
        this.nameToCallbackMap.put(name, callback);
    }

    /**
     * Adds an action identified by the provided name which will only be accessible, if the current user has the
     * required permission specified by the provided {@link Action action}.
     * 
     * @param name
     *            {@link String} to identify the action
     * @param action
     *            {@link Action} specified the permission which is required to access the action
     * @param callback
     *            {@link Consumer} to execute when the action is triggered
     */
    public void addAction(final String name, final Action action, final Consumer<T> callback) {
        this.nameToActionMap.put(name, action);
        this.addAction(name, callback);
    }

    public final Iterable<String> getAllowedActions1(final T object) {
        final ArrayList<String> allowedActions = new ArrayList<>();
        final UserDTO user = userService.getCurrentUser();
        for (final String name : nameToCallbackMap.keySet()) {
            final Action action = nameToActionMap.get(name);
            if (action == null || user.hasPermission(permissionFactory.apply(action, object),
                    object.getOwnership(), object.getAccessControlList())) {
                allowedActions.add(name);
            }
        }
        return allowedActions;
    }

    @Override
    public final String getValue(final T object) {
        final ArrayList<String> allowedActions = new ArrayList<>();
        final UserDTO user = userService.getCurrentUser();
        for (final String name : nameToCallbackMap.keySet()) {
            final Action action = nameToActionMap.get(name);
            if (isNotRestrictedOrHasPermission(user, action, object)) {
                name.replace("\\", "\\\\");
                name.replace(",", "\\,");
                allowedActions.add(name);
            }
        }
        return String.join(",", allowedActions);
    }

    private boolean isNotRestrictedOrHasPermission(final UserDTO user, final Action action, final T object) {
        final boolean result;
        if (action == null) {
            result = true;
        } else {
            final Ownership ownership = object.getOwnership();
            final AccessControlList accessControlList = object.getAccessControlList();
            result = user.hasPermission(permissionFactory.apply(action, object), ownership, accessControlList);
        }
        return result;
    }
}
