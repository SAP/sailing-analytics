package com.sap.sailing.landscape.ui.client;

import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.fileupload.FileUploadWithLocalFileContent;

/**
 * Supports adding an SSH key, either by pasting the public and private key in Base64-encoded
 * form into text fields, or by providing the public and private key files. The result of this
 * dialog is the public and the (hopefully encrypted) private key string.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class AddSshKeyDialog extends DataEntryDialog<Triple<String, String, String>> {
    private final TextBox keyPairNameBox;
    private final TextArea publicKeyArea;
    private final TextArea privateKeyArea;
    private final FileUpload publicKeyFileChooser;
    private final FileUpload privateKeyFileChooser;
    private final StringMessages stringMessages;
    
    public AddSshKeyDialog(StringMessages stringMessages,
            DialogCallback<Triple<String, String, String>> callback) {
        super(stringMessages.sshKeys(), stringMessages.sshKeys(), stringMessages.ok(), stringMessages.cancel(), new Validator<Triple<String, String, String>>() {
            @Override
            public String getErrorMessage(Triple<String, String, String> valueToValidate) {
                if (!Util.hasLength(valueToValidate.getA())) {
                    return stringMessages.pleaseProvideKeyPairName();
                } else if (!Util.hasLength(valueToValidate.getB())) {
                    return stringMessages.pleaseProvidePublicKey();
                } else if (!Util.hasLength(valueToValidate.getC())) {
                    return stringMessages.pleaseProvidePrivateKey();
                } else {
                    return null;
                }
            }
        }, callback);
        this.stringMessages = stringMessages;
        keyPairNameBox = createTextBox("", 20);
        publicKeyArea = createTextArea("");
        publicKeyArea.setCharacterWidth(80);
        publicKeyArea.setHeight("5em");
        privateKeyArea = createTextArea("");
        privateKeyArea.setCharacterWidth(80);
        privateKeyArea.setHeight("5em");
        publicKeyFileChooser = new FileUpload();
        copyFileContentsToTextAreaUponSelection(publicKeyFileChooser, publicKeyArea);
        privateKeyFileChooser = new FileUpload();
        copyFileContentsToTextAreaUponSelection(privateKeyFileChooser, privateKeyArea);
    }
    
    private void copyFileContentsToTextAreaUponSelection(FileUpload fileUploadToTakeFileFrom, TextArea textAreaToCopyFileContentsTo) {
        final FileUploadWithLocalFileContent fuwlfc = new FileUploadWithLocalFileContent(fileUploadToTakeFileFrom);
        fileUploadToTakeFileFrom.addChangeHandler(e->{
            if (fuwlfc.getFileList().length == 1) {
                fuwlfc.getFileContents(0, contents->{
                    textAreaToCopyFileContentsTo.setText(new String(contents));
                    AddSshKeyDialog.this.validateAndUpdate();
                });
            }
        });
    }

    @Override
    protected Widget getAdditionalWidget() {
        final FormPanel result = new FormPanel();
        final VerticalPanel verticalPanel = new VerticalPanel();
        result.add(verticalPanel);
        verticalPanel.add(new Label(stringMessages.name()));
        verticalPanel.add(keyPairNameBox);
        verticalPanel.add(new Label(stringMessages.publicKey()));
        verticalPanel.add(publicKeyFileChooser);
        verticalPanel.add(publicKeyArea);
        verticalPanel.add(new Label(stringMessages.privateKey()));
        verticalPanel.add(privateKeyFileChooser);
        verticalPanel.add(privateKeyArea);
        return result;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return keyPairNameBox;
    }

    @Override
    protected Triple<String, String, String> getResult() {
        return new Triple<>(keyPairNameBox.getValue(), publicKeyArea.getValue(), privateKeyArea.getValue());
    }

}
