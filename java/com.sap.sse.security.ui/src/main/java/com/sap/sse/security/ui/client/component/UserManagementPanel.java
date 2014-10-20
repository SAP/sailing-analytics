package com.sap.sse.security.ui.client.component;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sse.security.ui.client.StringMessages;
import com.sap.sse.security.ui.client.UserChangeEventHandler;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.shared.SuccessInfo;
import com.sap.sse.security.ui.shared.UserDTO;
import com.sap.sse.security.ui.shared.UserManagementServiceAsync;

public class UserManagementPanel extends DockPanel {
    
    private List<UserCreatedEventHandler> userCreatedHandlers = new ArrayList<>();
    
    private List<UserDeletedEventHandler> userDeletedHandlers = new ArrayList<>();
    
    private SingleSelectionModel<UserDTO> singleSelectionModel;

    private UserListDataProvider userListDataProvider;
    
    public UserManagementPanel(final UserService userService, final StringMessages stringMessages) {
        final UserManagementServiceAsync userManagementService = userService.getUserManagementService();
        VerticalPanel west = new VerticalPanel();
        HorizontalPanel buttonPanel = new HorizontalPanel();
        west.add(buttonPanel);
        singleSelectionModel = new SingleSelectionModel<>();
        Button createButton = new Button(stringMessages.createUser(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                new CreateUserDialog(stringMessages, userManagementService, userCreatedHandlers).show();
            }
        });
        buttonPanel.add(createButton);
        final Button deleteButton = new Button(stringMessages.deleteUser(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final UserDTO userToDelete = singleSelectionModel.getSelectedObject();
                final String username = userToDelete.getName();
                if (Window.confirm(stringMessages.doYouReallyWantToDeleteUser(username))) {
                    userManagementService.deleteUser(username, new AsyncCallback<SuccessInfo>() {
                        @Override
                        public void onSuccess(SuccessInfo result) {
                            for (UserDeletedEventHandler userDeletedHandler : userDeletedHandlers) {
                                userDeletedHandler.onUserDeleted(userToDelete);
                            }
                            Window.alert(result.getMessage());
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            Window.alert(stringMessages.errorDeletingUser());
                        }
                    });
                }
            }
        });
        buttonPanel.add(deleteButton);
        deleteButton.setEnabled(singleSelectionModel.getSelectedObject() != null);
        final UserList userList = new UserList();
        userList.setSelectionModel(singleSelectionModel);
        final UserDetailsView userDetailsView = new UserDetailsView(userService, singleSelectionModel.getSelectedObject(), stringMessages);
        add(userDetailsView, DockPanel.CENTER);
        userDetailsView.addUserChangeEventHandler(new UserChangeEventHandler() {
            @Override
            public void onUserChange(UserDTO user) {
                userListDataProvider.updateDisplays();
            }
        });
        singleSelectionModel.addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                userDetailsView.updateUser(singleSelectionModel.getSelectedObject(), userManagementService);
                deleteButton.setEnabled(singleSelectionModel.getSelectedObject() != null);
            }
        });
        userList.setPageSize(20);
        TextBox filterBox = new TextBox();
        userListDataProvider = new UserListDataProvider(userManagementService, filterBox);
        userListDataProvider.addDataDisplay(userList);
        SimplePager pager = new SimplePager(TextLocation.CENTER, false, /* fast forward step size */ 50, true);
        pager.setDisplay(userList);
        ScrollPanel scrollPanel = new ScrollPanel(userList);
        filterBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                userListDataProvider.updateDisplays();
            }
        });
        filterBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                userListDataProvider.updateDisplays();
            }
        });
        filterBox.getElement().setPropertyString("placeholder", stringMessages.filterUsers());
        west.add(filterBox);
        west.add(pager);
        west.add(scrollPanel);
        add(west, DockPanel.WEST);
        addUserCreatedEventHandler(new UserCreatedEventHandler() {
            @Override
            public void onUserCreated(UserDTO user) {
                userListDataProvider.updateDisplays();
                if (user != null) {
                    singleSelectionModel.setSelected(user, true);
                }
            }
        });
        addUserDeletedEventHandler(new UserDeletedEventHandler() {
            @Override
            public void onUserDeleted(UserDTO user) {
                userListDataProvider.updateDisplays();
            }
        });
    }

    public void addUserCreatedEventHandler(UserCreatedEventHandler handler){
        this.userCreatedHandlers.add(handler);
    }
    
    public void removeUserCreatedEventHandler(UserCreatedEventHandler handler){
        this.userCreatedHandlers.remove(handler);
    }
    
    public static interface UserCreatedEventHandler extends EventHandler {
        void onUserCreated(UserDTO user);
    }

    public void addUserDeletedEventHandler(UserDeletedEventHandler handler){
        this.userDeletedHandlers.add(handler);
    }
    
    public void removeUserDeletedEventHandler(UserDeletedEventHandler handler){
        this.userDeletedHandlers.remove(handler);
    }
    
    public static interface UserDeletedEventHandler extends EventHandler {
        void onUserDeleted(UserDTO user);
    }
}
