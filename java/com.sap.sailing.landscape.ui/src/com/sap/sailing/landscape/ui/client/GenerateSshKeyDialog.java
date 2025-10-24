package com.sap.sailing.landscape.ui.client;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

/**
 * Supports generating an SSH key. Result is the key pair name, the passphrase and the repeated passphrase which
 * is expected to equal the passphrase.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class GenerateSshKeyDialog extends DataEntryDialog<Triple<String, String, String>> {
    private final TextBox keyPairNameBox;
    private final PasswordTextBox passphraseBox;
    private final PasswordTextBox passphraseConfirmBox;
    private final StringMessages stringMessages;
    
    public GenerateSshKeyDialog(StringMessages stringMessages,
            DialogCallback<Triple<String, String, String>> callback) {
        super(stringMessages.sshKeys(), stringMessages.sshKeys(), stringMessages.ok(), stringMessages.cancel(), new Validator<Triple<String, String, String>>() {
            @Override
            public String getErrorMessage(Triple<String, String, String> valueToValidate) {
                if (!Util.hasLength(valueToValidate.getA())) {
                    return stringMessages.pleaseProvideKeyPairName();
                } else if (!Util.hasLength(valueToValidate.getB())) {
                    return stringMessages.pleaseProvidePassphrase();
                } else if (!Util.hasLength(valueToValidate.getC())) {
                    return stringMessages.pleaseRepeatPassphrase();
                } else if (!valueToValidate.getB().equals(valueToValidate.getC())) {
                    return stringMessages.passphrasesDontMatch();
                } else {
                    return null;
                }
            }
        }, callback);
        this.stringMessages = stringMessages;
        keyPairNameBox = createTextBox("", 20);
        passphraseBox = createPasswordTextBox("");
        passphraseConfirmBox = createPasswordTextBox("");
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        final FormPanel result = new FormPanel();
        final VerticalPanel verticalPanel = new VerticalPanel();
        result.add(verticalPanel);
        verticalPanel.add(new Label(stringMessages.name()));
        verticalPanel.add(keyPairNameBox);
        verticalPanel.add(new Label(stringMessages.password()));
        verticalPanel.add(passphraseBox);
        verticalPanel.add(new Label(stringMessages.passwordRepeat()));
        verticalPanel.add(passphraseConfirmBox);
        return result;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return keyPairNameBox;
    }

    @Override
    protected Triple<String, String, String> getResult() {
        return new Triple<>(keyPairNameBox.getValue(), passphraseBox.getValue(), passphraseConfirmBox.getValue());
    }

}
