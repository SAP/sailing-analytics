package com.sap.sse.security.ui.client.usermanagement.permissions;

import java.util.function.Function;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.shared.dto.WildcardPermissionWithSecurityDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.UserGroupListDataProvider.UserGroupListDataProviderChangeHandler;
import com.sap.sse.security.ui.client.i18n.StringMessages;
import com.sap.sse.security.ui.client.usermanagement.RoleAndPermissionDetailsResources;
import com.sap.sse.security.ui.shared.SuccessInfo;

/**
 * Details panel for displaying the permissions associated with a user. The panel contains an input field for adding a
 * permission to a user and a table with all permissions the user currently has.
 */
public class WildcardPermissionPanel extends HorizontalPanel
        implements Handler, ChangeHandler, KeyUpHandler, UserGroupListDataProviderChangeHandler {

    private final WildcardPermissionWithSecurityDTOTableWrapper wildcardPermissionWithSecurityDTOTableWrapper;
    private final SingleSelectionModel<UserDTO> userSelectionModel;
    private final SuggestBox suggestPermission;
    private final RoleAndPermissionDetailsResources roleAndPermissionResources = GWT
            .create(RoleAndPermissionDetailsResources.class);

    public WildcardPermissionPanel(final UserService userService, final StringMessages stringMessages,
            final ErrorReporter errorReporter, final CellTableWithCheckboxResources tableResources,
            final MultiSelectionModel<UserDTO> userSelectionModel, final Runnable updateUsers,
            Function<SuggestOracle, SuggestBox> suggestBoxConstructor) {
        // create multi to single selection adapter
        final SingleSelectionModel<UserDTO> multiToSingleSelectionModelAdapter = new SingleSelectionModel<>();
        this.ensureDebugId(this.getClass().getSimpleName());
        final MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
        userSelectionModel.addSelectionChangeHandler(event -> {
            multiToSingleSelectionModelAdapter.clear();
            if (userSelectionModel.getSelectedSet().size() != 1) {
                this.setVisible(false);
            } else {
                // has exactly one element in the set
                multiToSingleSelectionModelAdapter.setSelected(userSelectionModel.getSelectedSet().iterator().next(), true);
                this.setVisible(true);
                updatePermissionOracleWithSecuredTypes(userService, oracle);
                updatePermissionList();
            }
        });
        this.userSelectionModel = multiToSingleSelectionModelAdapter;
        // create suggest for permission
        updatePermissionOracleWithSecuredTypes(userService, oracle);
        suggestPermission = suggestBoxConstructor.apply(oracle);
        suggestPermission.ensureDebugId("suggestPermission");
        roleAndPermissionResources.css().ensureInjected();
        suggestPermission.addStyleName(roleAndPermissionResources.css().enterPermissionSuggest());
        suggestPermission.getElement().setPropertyString("placeholder", stringMessages.enterPermissionName());
        this.initPlaceholder(suggestPermission, stringMessages.enterPermissionName());
        // create permission input panel + add controls
        final HorizontalPanel permissionInputPanel = new HorizontalPanel();
        permissionInputPanel.add(suggestPermission);
        final Button addPermissionButton = new Button(stringMessages.add(), (ClickHandler) event -> {
            WildcardPermission selectedPermission = new WildcardPermission(suggestPermission.getText());
            if (selectedPermission != null) {
                UserDTO selectedUser = this.userSelectionModel.getSelectedObject();
                if (selectedUser != null) {
                    userService.getUserManagementWriteService().addPermissionForUser(selectedUser.getName(),
                            selectedPermission, new MarkedAsyncCallback<SuccessInfo>(new AsyncCallback<SuccessInfo>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    Window.alert(caught.getMessage());
                                }

                                @Override
                                public void onSuccess(SuccessInfo result) {
                                    if (result.isSuccessful()) {
                                        updateUsers.run();
                                    } else {
                                        Window.alert(result.getMessage());
                                    }
                                }
                            }));
                }
            }
            suggestPermission.setText("");
        });
        addPermissionButton.ensureDebugId("addPermissionButton");
        final Command addPermissionButtonUpdater = () -> addPermissionButton
                .setEnabled(!suggestPermission.getValue().isEmpty());
        suggestPermission.addKeyUpHandler(event -> addPermissionButtonUpdater.execute());
        suggestPermission.addSelectionHandler(event -> addPermissionButtonUpdater.execute());
        permissionInputPanel.add(addPermissionButton);
        // create permission table
        wildcardPermissionWithSecurityDTOTableWrapper = new WildcardPermissionWithSecurityDTOTableWrapper(userService,
                stringMessages, errorReporter, /* enablePager */ true, tableResources, this.userSelectionModel,
                updateUsers);
        final ScrollPanel scrollPanel = new ScrollPanel(wildcardPermissionWithSecurityDTOTableWrapper.asWidget());
        final LabeledAbstractFilterablePanel<WildcardPermissionWithSecurityDTO> userFilterbox = wildcardPermissionWithSecurityDTOTableWrapper
                .getFilterField();
        userFilterbox.getElement().setPropertyString("placeholder", stringMessages.filterUserGroups());
        // add elements to permission panel + add caption
        final VerticalPanel permissionPanel = new VerticalPanel();
        permissionPanel.add(permissionInputPanel);
        permissionPanel.add(userFilterbox);
        permissionPanel.add(scrollPanel);
        final CaptionPanel captionPanel = new CaptionPanel(stringMessages.permissions());
        captionPanel.add(permissionPanel);
        this.setVisible(false);
        add(captionPanel);
    }

    private void updatePermissionOracleWithSecuredTypes(final UserService userService,
            final MultiWordSuggestOracle oracle) {
        for (HasPermissions permission : userService.getAllKnownPermissions()) {
            for (HasPermissions.Action action : permission.getAvailableActions()) {
                oracle.add(permission.getStringPermission(action));
            }
        }
    }

    private void initPlaceholder(final UIObject target, final String placeholder) {
        target.getElement().setAttribute("placeholder", placeholder);
    }

    /** Refreshes the permission list in the table. */
    public void updatePermissionList() {
        wildcardPermissionWithSecurityDTOTableWrapper.refreshPermissionList();
    }

    @Override
    public void onChange() {
        updatePermissionList();
    }

    @Override
    public void onKeyUp(KeyUpEvent event) {
        updatePermissionList();
    }

    @Override
    public void onChange(ChangeEvent event) {
        updatePermissionList();
    }

    @Override
    public void onSelectionChange(SelectionChangeEvent event) {
        updatePermissionList();
    }
}
