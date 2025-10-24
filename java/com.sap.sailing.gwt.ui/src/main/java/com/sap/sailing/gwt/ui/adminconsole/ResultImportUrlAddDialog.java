package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.UrlDTO;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class ResultImportUrlAddDialog extends DataEntryDialog<UrlDTO> {
    private final SailingServiceAsync sailingService;
    private final StringMessages stringMessages;
    private final TextBox textBox;
    private final String urlProvider;
    
    protected class UrlDTOValidator implements Validator<UrlDTO> {
        @Override
        public String getErrorMessage(UrlDTO valueToValidate) {
            if (valueToValidate == null || valueToValidate.getUrl() == null || valueToValidate.getUrl().isEmpty()) {
                return stringMessages.pleaseEnterNonEmptyUrl();
            }
            return null;
        }

        @Override
        public void validate(UrlDTO valueToValidate, AsyncCallback<String> callback,
                AsyncActionsExecutor validationExecutor) {
            validationExecutor.execute(cb -> {
                String clientValidation = getErrorMessage(valueToValidate);
                if (clientValidation != null) {
                    cb.onSuccess(clientValidation);
                } else {
                    sailingService.validateResultImportUrl(urlProvider, valueToValidate, cb);
                }
            }, new AsyncCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    callback.onSuccess(result);
                }
                @Override
                public void onFailure(Throwable caught) {
                    callback.onSuccess(caught.getMessage());
                }
            });
        }
    }

    public ResultImportUrlAddDialog(String urlProvider, SailingServiceAsync sailingService,
            StringMessages stringMessages, DialogCallback<UrlDTO> callback) {
        super(stringMessages.addResultImportUrl(), urlProvider, stringMessages.add(),
                stringMessages.cancel(), null, callback);
        this.ensureDebugId("ResultImportUrlAddDialog");
        this.urlProvider = urlProvider;
        this.sailingService = sailingService;
        this.stringMessages = stringMessages;
        this.setValidator(new UrlDTOValidator());
        textBox = super.createTextBox(null, 120);
        textBox.getElement().setPropertyString("placeholder", stringMessages.eventIdOrUrl());
        textBox.ensureDebugId("ResultImportUrlAddDialogTextBox");
        textBox.setFocus(true);
    }

    @Override
    protected UrlDTO getResult() {
        return new UrlDTO(urlProvider, textBox.getValue().trim());
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return textBox;
    }

    @Override
    protected Widget getAdditionalWidget() {
        return textBox;
    }
}
