package com.sap.sailing.gwt.managementconsole.places.event.edit;

import com.google.gwt.core.client.GWT;
import com.sap.sailing.gwt.managementconsole.app.ManagementConsoleClientFactory;
import com.sap.sse.gwt.client.mvp.AbstractActivityProxy;

public class EditEventActivityProxy extends AbstractActivityProxy {

    private final ManagementConsoleClientFactory clientFactory;
    private final EditEventPlace place;

    public EditEventActivityProxy(ManagementConsoleClientFactory clientFactory, EditEventPlace place) {
        this.clientFactory = clientFactory;
        this.place = place;
    }

    @Override
    protected void startAsync() {
        GWT.runAsync(new AbstractRunAsyncCallback() {
            @Override
            public void onSuccess() {
                super.onSuccess(new EditEventActivity(clientFactory, place));
            }
        });
    }

}
