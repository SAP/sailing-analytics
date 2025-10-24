package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public abstract class AbstractChooseNameDenoteEventDialog<T> extends DataEntryDialog<T> {
    protected StringMessages stringMessages;
    
    public AbstractChooseNameDenoteEventDialog(String title, StringMessages stringMessages,
            Validator<T> validator, DialogCallback<T> callback) {
        super(title, null, stringMessages.ok(), stringMessages.cancel(), validator, callback);
        this.stringMessages = stringMessages;
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        return new VerticalPanel();
    }

    @Override
    protected T getResult() {
        return null;
    }
}
