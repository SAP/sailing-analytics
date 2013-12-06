package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.sap.sailing.domain.common.impl.QRCodeUtils;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class DeviceConfigurationQRIdentifierDialog extends DialogBox {
    
    public static final String rcAppApkPath = "/apps/com.sap.sailing.racecommittee.app.apk";
    
    private static final String qrCodeDivId = "qr-code";
    private static final int qrCodeSize = 320;
    
    private final TextBox identifierBox;
    private final TextBox serverBox;

    protected static String getApkPath(String serverUrl) {
        if (serverUrl.endsWith("/")) {
            return serverUrl.substring(0, serverUrl.length() - 1) + rcAppApkPath;
        }
        return serverUrl + rcAppApkPath;
    }

    public DeviceConfigurationQRIdentifierDialog(String identifier, StringMessages stringMessages) {
        setText("Synchronize device with server");
        
        identifierBox = new TextBox();
        identifierBox.setValue(identifier);
        identifierBox.setReadOnly(true);
        identifierBox.setVisibleLength(40);
        
        serverBox = new TextBox();
        serverBox.setVisibleLength(40);
        serverBox.setValue(Window.Location.getProtocol() + "//" + Window.Location.getHost());
        serverBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                generateQRCode();
            }
        });
        serverBox.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                generateQRCode();
            }
        });
        
        Grid inputGrid = new Grid(2, 2);
        inputGrid.setWidget(0, 0, new Label("Identifier:"));
        inputGrid.setWidget(0, 1, identifierBox);
        inputGrid.setWidget(1, 0, new Label("Server URL:"));
        inputGrid.setWidget(1, 1, serverBox);
        
        SimplePanel qrCodeContainer = new SimplePanel();
        qrCodeContainer.getElement().setId(qrCodeDivId);
        qrCodeContainer.setHeight(qrCodeSize + "px");
        qrCodeContainer.setWidth(qrCodeSize + "px");
        
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
        panel.add(inputGrid);
        panel.add(qrCodeContainer);
        panel.setCellHorizontalAlignment(qrCodeContainer, HasHorizontalAlignment.ALIGN_CENTER);
        panel.add(actionPanel);
        setWidget(panel);
    }
    
    @Override
    protected void onAttach() {
        super.onAttach();
        createQRCode(qrCodeDivId, qrCodeSize);
        generateQRCode();
    }
    
    public static native void createQRCode(String id, int size) /*-{
        $wnd.qrcode = new $wnd.QRCode(id, {
            text: "http://jindo.dev.naver.com/collie",
            width: size,
            height: size,
            colorDark : "#000000",
            colorLight : "#ffffff",
            correctLevel : $wnd.QRCode.CorrectLevel.H
        });
        $wnd.qrcode.clear();
    }-*/;

    protected void generateQRCode() {
        if (identifierBox.getValue().contains("#")) {
            Window.alert("I'm not capable of generating a code for this identifier.");
        } else if (!identifierBox.getValue().isEmpty() && !serverBox.getValue().isEmpty()) {
            String encoded = QRCodeUtils.composeQRContent(identifierBox.getValue(), getApkPath(serverBox.getValue()));
            generateQRImage(encoded);
        }
    }
    

    public static native void generateQRImage(String content) /*-{
        $wnd.qrcode.clear();
        $wnd.qrcode.makeCode(content);
}-*/;
    
}
