package com.sap.sse.security.ui.oauth.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.gwt.client.EntryPointHelper;
import com.sap.sse.security.ui.client.RemoteServiceMappingConstants;
import com.sap.sse.security.ui.client.StringMessages;
import com.sap.sse.security.ui.client.UserChangeEventHandler;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.oauth.client.component.OAuthLoginPanel;
import com.sap.sse.security.ui.oauth.client.util.ClientUtils;
import com.sap.sse.security.ui.shared.UserDTO;
import com.sap.sse.security.ui.shared.UserManagementService;
import com.sap.sse.security.ui.shared.UserManagementServiceAsync;

public class OAuthLoginEntryPoint implements EntryPoint, UserChangeEventHandler {
    private final UserManagementServiceAsync userManagementService = GWT.create(UserManagementService.class);
    private final StringMessages stringMessages = GWT.create(StringMessages.class);
    private final UserService userService = new UserService();

    private OAuthLoginPanel loginScreen = new OAuthLoginPanel(userManagementService);
    private FlowPanel content = new FlowPanel();

    public void onModuleLoad() {
        EntryPointHelper.registerASyncService((ServiceDefTarget) userManagementService,
                RemoteServiceMappingConstants.userManagementServiceRemotePath);
        RootPanel.get().add(content);
        setContentMessage(stringMessages.loading());
        try {
            handleRedirect();
        } catch (Exception e) {
            log(e.getMessage());
        }
    }

    private void handleRedirect() throws Exception {
        if (ClientUtils.redirected()) {
            if (!ClientUtils.alreadyLoggedIn()) {
                setContentMessage(stringMessages.tryingToVerifyUser());
                userService.verifySocialUser(new AsyncCallback<UserDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(UserDTO result) {
                        ClientUtils.saveUsername(result.getName());
                    }
                });
            } else {
                updateUserContent();
            }
        } else {
            updateUserContent();
        }
    }

    private void updateUserContent() {
        UserDTO result = userService.getCurrentUser();
        if (result == null) {
            setContent(loginScreen);
        } else {
            setLoggedInContent(result);
            closeWindow();
        }
    }

    @Override
    public void onUserChange(UserDTO result) {
        if (result == null) {
            setContent(loginScreen);
        } else {
            String name = result.getName();
            log("User '" + name + "' is verified!\n");
            ClientUtils.saveUsername(name);
            setLoggedInContent(result);
            closeWindow();
        }
    }

    public void log(String msg) {
        GWT.log(msg);
    }
    
    private void setContentMessage(String msg){
        content.clear();
        content.add(new Label(msg));
    }
    
    private void setContent(Widget w){
        content.clear();
        content.add(w);
    }
    
    private void setLoggedInContent(UserDTO user){
        content.clear();
        content.add(new Label(stringMessages.signedInAs(user.getName())));
        content.add(new Button(stringMessages.close(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                closeWindow();
            }
        }));
    }
    
    public static native void closeWindow()/*-{
        $doc.windowCloser();
    }-*/;
}
