package com.sap.sse.security.ui.client.component;

import static com.sap.sse.security.shared.impl.SecuredSecurityTypes.USER_GROUP;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.dto.RoleDefinitionDTO;
import com.sap.sse.security.shared.dto.StrippedRoleDefinitionDTO;
import com.sap.sse.security.shared.dto.UserGroupDTO;
import com.sap.sse.security.ui.client.UserManagementServiceAsync;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.UserGroupListDataProvider.UserGroupListDataProviderChangeHandler;
import com.sap.sse.security.ui.client.i18n.StringMessages;

public class UserGroupRoleDefinitionPanel extends HorizontalPanel
        implements Handler, ChangeHandler, KeyUpHandler, UserGroupListDataProviderChangeHandler {

    private final RoleDefinitionTableWrapper roleDefinitionTableWrapper;

    private final SingleSelectionModel<UserGroupDTO> userGroupSelectionModel;

    private final SuggestBox suggestRole;

    public UserGroupRoleDefinitionPanel(final UserService userService, final StringMessages stringMessages,
            Iterable<HasPermissions> additionalPermissions, ErrorReporter errorReporter,
            CellTableWithCheckboxResources tableResources,
            final SingleSelectionModel<UserGroupDTO> userGroupSelectionModel,
            UserGroupListDataProvider userGroupListDataProvider) {
        this.userGroupSelectionModel = userGroupSelectionModel;
        final UserManagementServiceAsync userManagementService = userService.getUserManagementService();
        final VerticalPanel west = new VerticalPanel();

        suggestRole = new SuggestBox(new RoleDefinitionSuggestOracle(userManagementService));

        // create button bar
        west.add(createButtonPanel(userService, stringMessages, userManagementService));
        west.add(suggestRole);

        // create UserGroup Table
        userGroupListDataProvider.addChangeHandler(this);
        roleDefinitionTableWrapper = new RoleDefinitionTableWrapper(userService, additionalPermissions, stringMessages,
                errorReporter, /* enablePager */ true, tableResources, () -> updateUserGroups(),
                userGroupSelectionModel);

        final ScrollPanel scrollPanel = new ScrollPanel(roleDefinitionTableWrapper.asWidget());
        final LabeledAbstractFilterablePanel<Pair<StrippedRoleDefinitionDTO, Boolean>> userGroupfilterBox = roleDefinitionTableWrapper
                .getFilterField();
        userGroupfilterBox.getElement().setPropertyString("placeholder", stringMessages.filterUserGroups());

        west.add(userGroupfilterBox);
        west.add(scrollPanel);

        add(west);
    }

    /** Creates the button bar with add/remove/refresh buttons. */
    private Widget createButtonPanel(final UserService userService, final StringMessages stringMessages,
            final UserManagementServiceAsync userManagementService) {
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService, USER_GROUP);
        buttonPanel.addUnsecuredAction(stringMessages.refresh(), () -> updateUserGroups());
        buttonPanel.addCreateAction(stringMessages.addRole(), () -> {
            final UserGroupDTO selectedObject = userGroupSelectionModel.getSelectedObject();
            if (selectedObject != null) {
                RoleDefinitionDTO role = ((RoleDefinitionSuggestOracle) suggestRole.getSuggestOracle())
                        .fromString(suggestRole.getValue());
                if (role != null) {
                    userManagementService.putRoleDefintionToUserGroup(selectedObject.getId().toString(),
                            role.getId().toString(), false, new AsyncCallback<Void>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    Window.alert(stringMessages.couldNotAddRoleToGroup(role.getName(),
                                            selectedObject.getName()));
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    roleDefinitionTableWrapper.refreshRoleList();
                                    // TODO maybe add role to list + refresh without backend call
                                }
                            });
                }
            }
        });
        buttonPanel.addRemoveAction(stringMessages.removeRole(), () -> {
            Pair<StrippedRoleDefinitionDTO, Boolean> selectedRole = roleDefinitionTableWrapper.getSelectionModel()
                    .getSelectedObject();
            if (selectedRole == null) {
                Window.alert(stringMessages.youHaveToSelectAUserGroup());
            } else if (Window.confirm(stringMessages.doYouReallyWantToRemoveRole(selectedRole.getA().getName()))) {
                userManagementService.removeRoleDefintionFromUserGroup(
                        userGroupSelectionModel.getSelectedObject().getId().toString(),
                        selectedRole.getA().getId().toString(), new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                Window.alert(stringMessages.couldNotDeleteRole(selectedRole.getA().getName()));
                            }

                            @Override
                            public void onSuccess(Void result) {
                                updateUserGroups();
                            }
                        });
            }
        });
        return buttonPanel;
    }

    /** Updates the UserGroups. */
    public void updateUserGroups() {
        roleDefinitionTableWrapper.refreshRoleList();
    }

    @Override
    public void onChange() {
        updateUserGroups();
    }

    @Override
    public void onKeyUp(KeyUpEvent event) {
        updateUserGroups();
    }

    @Override
    public void onChange(ChangeEvent event) {
        updateUserGroups();
    }

    @Override
    public void onSelectionChange(SelectionChangeEvent event) {
        updateUserGroups();
    }
}
