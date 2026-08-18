package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sailing.domain.common.security.SecuredDomainType.BOAT;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.Displayer;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.adminconsole.FilterablePanelProvider;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.panels.AbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;

/**
 * Allows an administrator to view and edit the set of boats currently maintained by the server.
 * 
 * @author Frank Mittag (c5163874)
 * 
 */
public class BoatPanel extends SimplePanel implements FilterablePanelProvider<BoatDTO> {
    private final BoatTableWrapper<RefreshableMultiSelectionModel<BoatDTO>> boatTable;
    private final RefreshableMultiSelectionModel<BoatDTO> refreshableBoatSelectionModel;
    private Button allowReloadButton;
    private final Displayer<BoatDTO> boatsDisplayer;

    public BoatPanel(final Presenter presenter, final StringMessages stringMessages) {
        super();
        this.boatsDisplayer = result -> refreshBoatList();
        this.boatTable = new BoatTableWrapper<>(presenter.getSailingService(), presenter.getUserService(),
                presenter.getBoatsRefresher(), presenter.getCompetitorsRefresher(), stringMessages,
                presenter.getErrorReporter(), /* multiSelection */ true, /* enablePager */ true, 100, true);
        this.refreshableBoatSelectionModel = (RefreshableMultiSelectionModel<BoatDTO>) boatTable.getSelectionModel();
        VerticalPanel mainPanel = new VerticalPanel();
        this.setWidget(mainPanel);
        mainPanel.setWidth("100%");
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(presenter.getUserService(), BOAT);
        mainPanel.add(buttonPanel);
        final Button refreshButton = buttonPanel.addUnsecuredAction(stringMessages.refresh(), presenter.getBoatsRefresher()::reloadAndCallFillAll);
        refreshButton.ensureDebugId("RefreshButton");
        allowReloadButton = buttonPanel.addUnsecuredAction(stringMessages.allowReload(),
                () -> boatTable.allowUpdate(refreshableBoatSelectionModel.getSelectedSet()));
        refreshableBoatSelectionModel.addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                boolean allUpdateable = true;
                for (BoatDTO boat : refreshableBoatSelectionModel.getSelectedSet()) {
                    if (!presenter.getUserService().hasPermission(boat, DefaultActions.UPDATE)) {
                        allUpdateable = false;
                    }
                }
                allowReloadButton
                        .setEnabled(allUpdateable && !refreshableBoatSelectionModel.getSelectedSet().isEmpty());
            }
        });
        allowReloadButton.setEnabled(!refreshableBoatSelectionModel.getSelectedSet().isEmpty());
        final Button addBoatButton = buttonPanel.addCreateAction(stringMessages.add(), this::openAddBoatDialog);
        addBoatButton.ensureDebugId("AddBoatButton");
        mainPanel.add(boatTable);
    }
    
    public Displayer<BoatDTO> getBoatsDisplayer() {
        return boatsDisplayer;
    }

    private void openAddBoatDialog() {
        boatTable.openEditBoatDialog(new BoatDTO(), null);
    }

    public void refreshBoatList() {
        boatTable.refreshBoatList(/* loadOnlyStandaloneBoats */ false, /* callback */ null);
    }

    @Override
    public AbstractFilterablePanel<BoatDTO> getFilterablePanel() {
        return boatTable.getFilterField();
    }
}
