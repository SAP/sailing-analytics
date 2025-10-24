package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.adminconsole.AdminConsolePanelSupplier;

public class IgtimiDevicesPanelSupplier extends AdminConsolePanelSupplier<IgtimiDevicesPanel> {

    private final StringMessages stringMessages;
    private final Presenter presenter;

    public IgtimiDevicesPanelSupplier(final StringMessages stringMessages, final Presenter presenter) {
        super();
        this.stringMessages = stringMessages;
        this.presenter = presenter;
    }

    @Override
    public IgtimiDevicesPanel init() {
        IgtimiDevicesPanel igtimiAccountsPanel = new IgtimiDevicesPanel(presenter, stringMessages);
        igtimiAccountsPanel.ensureDebugId("IgtimiDevices");
        igtimiAccountsPanel.refreshDevices();
        igtimiAccountsPanel.refreshDataAccessWindows();
        return igtimiAccountsPanel;
    }

    @Override
    public void getAsync(RunAsyncCallback callback) {
        GWT.runAsync(new RunAsyncCallback() {

            @Override
            public void onSuccess() {
                widget = init();
                callback.onSuccess();
            }

            @Override
            public void onFailure(Throwable reason) {
                callback.onFailure(reason);
            }
        });
    }

}