package com.sap.sailing.gwt.ui.adminconsole;

import java.util.UUID;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.sap.sailing.domain.common.impl.DeviceConfigurationQRCodeUtils;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;

public class DeviceConfigurationQRIdentifierDialog extends DialogBox {
    public static final String rcAppApkPath = "/apps/com.sap.sailing.racecommittee.app.apk";
    private static final int qrCodeSize = 320;

    private class DeviceConfigurationQRIdentifierWidget extends BaseQRIdentifierWidget {
        private final TextBox configNameBox;
        private final TextBox configIdBox;
        private final CheckBox addAccessTokenCheckBox;
        private final StringMessages stringMessages;
        private final String accessToken;
        private final UUID eventId;
        private final UUID courseAreaId;
        private final Integer priority;
        
        public DeviceConfigurationQRIdentifierWidget(String uuidAsString, String configName, String accessToken,
                StringMessages stringMessages, UUID eventId, UUID courseAreaId, Integer priority) {
            super(qrCodeSize, stringMessages);
            this.accessToken = accessToken;
            this.eventId = eventId;
            this.courseAreaId = courseAreaId;
            this.priority = priority;
            configNameBox = new TextBox();
            configNameBox.setValue(configName);
            configNameBox.setReadOnly(true);
            configNameBox.setVisibleLength(40);
            inputGrid.resize(4, 2);
            inputGrid.setWidget(1, 0, new Label(stringMessages.name()));
            inputGrid.setWidget(1, 1, configNameBox);
            configIdBox = new TextBox();
            configIdBox.setValue(uuidAsString);
            configIdBox.setReadOnly(true);
            configIdBox.setVisibleLength(40);
            inputGrid.setWidget(2, 0, new Label(stringMessages.id()));
            inputGrid.setWidget(2, 1, configIdBox);
            addAccessTokenCheckBox = new CheckBox(stringMessages.includeAccessTokenForYourAccount());
            addAccessTokenCheckBox.setValue(false);
            addAccessTokenCheckBox.addValueChangeHandler(e->generateQRCode());
            inputGrid.setWidget(3, 0, addAccessTokenCheckBox);
            this.stringMessages = stringMessages;
        }
        
        @Override
        protected String generateEncodedQRCodeContent() {
            if (configNameBox.getValue().contains("#")) {
                Notification.notify(stringMessages.notCapableOfGeneratingACodeForIdentifier(), NotificationType.ERROR);
            } else if (!configNameBox.getValue().isEmpty() && !serverBox.getValue().isEmpty()) {
                return DeviceConfigurationQRCodeUtils.composeQRContent(getServerUrlWithoutFinalSlash(), URL.encodeQueryString(configNameBox.getValue()),
                        URL.encodeQueryString(configIdBox.getValue()), eventId, courseAreaId, priority,
                        addAccessTokenCheckBox.getValue() ? URL.encodeQueryString(accessToken) : null);
            }
            return null;
        }   
    }
    
    private final DeviceConfigurationQRIdentifierWidget widget;
    
    public DeviceConfigurationQRIdentifierDialog(String uuidAsString, String configName, UUID eventId,
            UUID courseAreaId, Integer priority, String accessToken, StringMessages stringMessages) {
        widget = new DeviceConfigurationQRIdentifierWidget(uuidAsString, configName, accessToken, stringMessages,
                eventId, courseAreaId, priority);
        Button exitButton = new Button(stringMessages.close());
        exitButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hide();
            }
        });
        HorizontalPanel actionPanel = new HorizontalPanel();
        actionPanel.add(exitButton);
        VerticalPanel panel = new VerticalPanel();
        panel.add(widget);
        panel.add(actionPanel);
        setWidget(panel);
        this.ensureDebugId("DeviceConfigurationQRIdentifierDialog");
    }
    
    @Override
    protected void onAttach() {
        super.onAttach();
        widget.generateQRCode();
    }   
}
