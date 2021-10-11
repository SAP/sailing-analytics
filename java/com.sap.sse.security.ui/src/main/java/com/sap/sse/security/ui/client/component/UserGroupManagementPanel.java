package com.sap.sse.security.ui.client.component;

import static com.sap.sse.security.shared.impl.SecuredSecurityTypes.USER_GROUP;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.ParallelExecutionCallback;
import com.sap.sse.gwt.client.async.ParallelExecutionHolder;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.celltable.RefreshableSelectionModel;
import com.sap.sse.gwt.client.celltable.TableWrapper;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.dto.UserGroupDTO;
import com.sap.sse.security.ui.client.UserManagementServiceAsync;
import com.sap.sse.security.ui.client.UserManagementWriteServiceAsync;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.usergroup.roles.GroupRoleDefinitionPanel;
import com.sap.sse.security.ui.client.component.usergroup.users.UserGroupDetailPanel;
import com.sap.sse.security.ui.client.i18n.StringMessages;
import com.sap.sse.security.ui.shared.SuccessInfo;

/**
 * Shows a {@link UserGroupTableWrapper} with an overview over all existing user groups and allows them to be edited via
 * a {@link UserGroupDetailPanel}.
 */
public class UserGroupManagementPanel extends Composite {
    private final UserGroupListDataProvider userGroupListDataProvider;
    private UserGroupDetailPanel userGroupDetailPanel;
    private GroupRoleDefinitionPanel userGroupRoleDefinitionPanel;
    protected final UserGroupTableWrapper userGroupTableWrapper;

    public UserGroupManagementPanel(final UserService userService, final StringMessages stringMessages,
            ErrorReporter errorReporter, CellTableWithCheckboxResources tableResources) {
        // using write service to enforce round trip going to master for consistency purposes
        final UserManagementWriteServiceAsync userManagementWriteService = userService.getUserManagementWriteService();
        final VerticalPanel mainPanel = new VerticalPanel();
        userGroupListDataProvider = new UserGroupListDataProvider(userManagementWriteService, new TextBox());
        userGroupTableWrapper = new UserGroupTableWrapper(userService, stringMessages, errorReporter,
                /* enablePager */ true, tableResources, () -> updateUserGroups());
        mainPanel.add(createButtonPanel(userService, stringMessages, userManagementWriteService, userGroupTableWrapper.getSelectionModel()));
        final LabeledAbstractFilterablePanel<UserGroupDTO> userGroupfilterBox = userGroupTableWrapper.getFilterField();
        userGroupfilterBox.getElement().setPropertyString("placeholder", stringMessages.filterUserGroups());
        mainPanel.add(userGroupfilterBox);
        mainPanel.add(new ScrollPanel(userGroupTableWrapper.asWidget()));
        mainPanel.add(createUserGroupDetailsPanel(stringMessages, userManagementWriteService,
                userService, errorReporter, tableResources));
        initWidget(mainPanel);
        this.ensureDebugId(this.getClass().getSimpleName());
    }

    /** Creates the button bar with add/remove/refresh buttons. 
     * @param userGroupSelectionModel */
    private Widget createButtonPanel(final UserService userService, final StringMessages stringMessages,
            final UserManagementWriteServiceAsync userManagementWriteService,
            RefreshableSelectionModel<UserGroupDTO> userGroupSelectionModel) {
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService, USER_GROUP);
        buttonPanel.addUnsecuredAction(stringMessages.refresh(), () -> updateUserGroups());
        final Button createGroupButton = buttonPanel.addCreateActionWithoutServerCreateObjectPermissionCheck(stringMessages.createUserGroup(),
                () -> new CreateUserGroupDialog(stringMessages, userService, userManagementWriteService,
                        userGroupListDataProvider, () -> updateUserGroups()).show());
        createGroupButton.ensureDebugId("CreateGroupButton");
        final Button removeGroupButton = buttonPanel.addRemoveAction(stringMessages.removeUserGroup(), userGroupSelectionModel, false, () -> {
            Set<UserGroupDTO> userGroups = userGroupTableWrapper.getSelectionModel().getSelectedSet();
            if (userGroups == null || userGroups.isEmpty()) {
                Window.alert(stringMessages.youHaveToSelectAUserGroup());
            } else {
                final String userGroupNames = String.join(", ", Util.map(userGroups, g -> g.getName()));
                if (Window.confirm(stringMessages.doYouReallyWantToRemoveUserGroup(userGroupNames))) {
                    final Map<UserGroupDTO, ParallelExecutionCallback<SuccessInfo>> callbacks = new HashMap<>();
                    for (final UserGroupDTO userGroup : userGroups) {
                        callbacks.put(userGroup, new ParallelExecutionCallback<>());
                    }
                    new ParallelExecutionHolder(callbacks.values().toArray(new ParallelExecutionCallback<?>[0])) {
                        @Override
                        public void handleSuccess() {
                            userGroupListDataProvider.updateDisplays();
                            Window.alert(stringMessages.successMessageRemovedUserGroup(userGroupNames));
                            updateUserGroups();
                        }

                        @Override
                        public void handleFailure(Throwable caught) {
                            Window.alert(stringMessages.couldNotDeleteUserGroup());
                        }
                    };
                    for (final Entry<UserGroupDTO, ParallelExecutionCallback<SuccessInfo>> groupAndCallback : callbacks
                            .entrySet()) {
                        userManagementWriteService.deleteUserGroup(groupAndCallback.getKey().getId().toString(),
                                groupAndCallback.getValue());
                    }
                }
            }
        });
        removeGroupButton.ensureDebugId("DeleteUserGroupButton");
        return buttonPanel;
    }

    /**
     * Creates the UserGroupDetailsPanel which contains details about the selected user group
     */
    private HorizontalPanel createUserGroupDetailsPanel(final StringMessages stringMessages,
            final UserManagementServiceAsync userManagementService, UserService userService,
            ErrorReporter errorReporter, CellTableWithCheckboxResources tableResources) {
        userGroupDetailPanel = new UserGroupDetailPanel(userGroupTableWrapper.getSelectionModel(),
                userGroupListDataProvider, userService, stringMessages, errorReporter, tableResources);
        userGroupRoleDefinitionPanel = new GroupRoleDefinitionPanel(userService,
                stringMessages, errorReporter, tableResources, userGroupTableWrapper.getSelectionModel(),
                userGroupListDataProvider);
        final VerticalPanel userListWrapper = new VerticalPanel();
        userListWrapper.add(userGroupDetailPanel);
        final CaptionPanel userListCaption = new CaptionPanel(stringMessages.users());
        userListCaption.add(userListWrapper);
        final VerticalPanel roleWrapper = new VerticalPanel();
        roleWrapper.add(userGroupRoleDefinitionPanel);
        final CaptionPanel roleCaption = new CaptionPanel(stringMessages.roles());
        roleCaption.add(roleWrapper);
        final HorizontalPanel listsWrapper = new HorizontalPanel();
        listsWrapper.add(userListCaption);
        listsWrapper.add(roleCaption);
        listsWrapper.setVisible(false);
        userGroupTableWrapper.getSelectionModel().addSelectionChangeHandler(
                h -> listsWrapper.setVisible(TableWrapper.getSingleSelectedObjectOrNull(userGroupTableWrapper.getSelectionModel()) != null));
        return listsWrapper;
    }

    /** Updates the UserGroups. */
    public void updateUserGroups() {
        userGroupTableWrapper.refreshUserList(null);
        userGroupListDataProvider.updateDisplays();
        userGroupDetailPanel.updateUserList();
        userGroupRoleDefinitionPanel.updateUserGroups();
    }

    public void refreshSuggests() {
        userGroupRoleDefinitionPanel.refreshSuggest();
    }
}
