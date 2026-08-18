package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sailing.domain.common.security.SecuredDomainType.REGATTA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.gwt.common.client.help.HelpButton;
import com.sap.sailing.gwt.common.client.help.HelpButtonResources;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.Displayer;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sse.gwt.adminconsole.FilterablePanelProvider;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.panels.AbstractFilterablePanel;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;

/**
 * Allows administrators to manage the structure of a regatta. Each regatta consists of several substructures like
 * races, series and groups (big fleets divided into racing groups).
 * 
 * @author Frank Mittag (C5163974)
 */
public class RegattaManagementPanel extends SimplePanel implements FilterablePanelProvider<RegattaDTO> {

    private final SailingServiceWriteAsync sailingServiceWrite;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private final Presenter presenter;
    private final RefreshableMultiSelectionModel<RegattaDTO> refreshableRegattaMultiSelectionModel;
    private final RegattaListComposite regattaListComposite;
    private final RegattaDetailsComposite regattaDetailsComposite;
    private final UserService userService;
    
    private final Displayer<RegattaDTO> regattasDisplayer;
    
    public RegattaManagementPanel(StringMessages stringMessages, Presenter presenter) {
        this.sailingServiceWrite = presenter.getSailingService();
        this.userService = presenter.getUserService();
        this.stringMessages = stringMessages;
        this.errorReporter = presenter.getErrorReporter();
        this.presenter = presenter;
        final VerticalPanel mainPanel = new VerticalPanel();
        setWidget(mainPanel);
        mainPanel.setWidth("100%");
        final CaptionPanel regattasPanel = new CaptionPanel(stringMessages.regattas());
        mainPanel.add(regattasPanel);
        final VerticalPanel regattasContentPanel = new VerticalPanel();
        regattasPanel.setContentWidget(regattasContentPanel);
        regattaListComposite = new RegattaListComposite(presenter, stringMessages);
        regattaListComposite.ensureDebugId("RegattaListComposite");
        regattasDisplayer = result->regattaListComposite.fillRegattas(result);
        refreshableRegattaMultiSelectionModel = regattaListComposite.getRefreshableMultiSelectionModel();
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService, REGATTA);
        final Button update = buttonPanel.addCreateAction(stringMessages.refresh(), new Command() {
            @Override
            public void execute() {
                presenter.getRegattasRefresher().reloadAndCallFillAll();
            }
        });
        update.ensureDebugId("UpdateRegattaButton");
        final Button create = buttonPanel.addCreateAction(stringMessages.addRegatta(), this::openCreateRegattaDialog);
        create.ensureDebugId("AddRegattaButton");
        buttonPanel.addRemoveAction(stringMessages.remove(),
                refreshableRegattaMultiSelectionModel, true, () -> {
                    // unmodifiable collection can't be sent to the server.
                    final Collection<RegattaIdentifier> regattas = createModifiableCollection();
                    removeRegattas(regattas);
                });
        buttonPanel.addUnsecuredWidget(new HelpButton(HelpButtonResources.INSTANCE,
                stringMessages.videoGuide(), "https://sapsailing-documentation.s3-eu-west-1.amazonaws.com/adminconsole/Advanced+Topics/Setting+up+Events+with+multiple+Regattas+or+Classes.mp4"));
        regattasContentPanel.add(buttonPanel);
        refreshableRegattaMultiSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                final List<RegattaDTO> selectedRegattas = new ArrayList<>(
                        refreshableRegattaMultiSelectionModel.getSelectedSet());
                final RegattaIdentifier selectedRegatta;
                if (selectedRegattas.size() == 1) {
                    selectedRegatta = selectedRegattas.iterator().next().getRegattaIdentifier();
                    if (selectedRegatta != null && regattaListComposite.getAllRegattas() != null) {
                        for (RegattaDTO regattaDTO : regattaListComposite.getAllRegattas()) {
                            if (regattaDTO.getRegattaIdentifier().equals(selectedRegatta)) {
                                regattaDetailsComposite.setRegatta(regattaDTO);
                                regattaDetailsComposite.setVisible(true);
                                break;
                            }
                        }
                    }
                } else {
                    regattaDetailsComposite.setRegatta(null);
                    regattaDetailsComposite.setVisible(false);
                }
            }
        });
        regattasContentPanel.add(regattaListComposite);
        regattaDetailsComposite = new RegattaDetailsComposite(presenter, stringMessages);
        regattaDetailsComposite.ensureDebugId("RegattaDetailsComposite");
        regattaDetailsComposite.setVisible(false);
        mainPanel.add(regattaDetailsComposite);
    }
    
    public Displayer<RegattaDTO> getRegattasDisplayer() {
        return regattasDisplayer;
    }

    protected void removeRegattas(Collection<RegattaIdentifier> regattas) {
        if (!regattas.isEmpty()) {
            sailingServiceWrite.removeRegattas(regattas, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Error trying to remove the regattas:" + caught.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    presenter.getRegattasRefresher().reloadAndCallFillAll();
                    presenter.getLeaderboardsRefresher().reloadAndCallFillAll();
                }
            });
        }
    }

    private void openCreateRegattaDialog() {
        final Collection<RegattaDTO> existingRegattas = Collections.unmodifiableCollection(regattaListComposite.getAllRegattas());
        @SuppressWarnings("unchecked")
        final Displayer<EventDTO>[] eventDisplayer = (Displayer<EventDTO>[]) new Displayer<?>[1];
        eventDisplayer[0] = new Displayer<EventDTO>() {
            @Override
            public void fill(Iterable<EventDTO> events) {
                openCreateRegattaDialog(existingRegattas, events);
                presenter.getEventsRefresher().removeDisplayer(eventDisplayer[0]);
            }
        };
        presenter.getEventsRefresher().addDisplayerAndCallFillOnInit(eventDisplayer[0]);
    }

    private void openCreateRegattaDialog(Collection<RegattaDTO> existingRegattas, final Iterable<EventDTO> existingEvents) {
        RegattaWithSeriesAndFleetsCreateDialog dialog = new RegattaWithSeriesAndFleetsCreateDialog(existingRegattas,
                existingEvents, /* eventToSelect */ null, sailingServiceWrite, userService,
                stringMessages, new CreateRegattaCallback(stringMessages, presenter, existingEvents));
        dialog.ensureDebugId("RegattaCreateDialog");
        dialog.show();
    }

    private Collection<RegattaIdentifier> createModifiableCollection() {
        Collection<RegattaIdentifier> regattas = new HashSet<RegattaIdentifier>();
        for (RegattaDTO regatta : refreshableRegattaMultiSelectionModel.getSelectedSet()) {
            regattas.add(regatta.getRegattaIdentifier());
        }
        return regattas;
    }
    
    @Override
    public AbstractFilterablePanel<RegattaDTO> getFilterablePanel() {
        return regattaListComposite.filterablePanelRegattas;
    }
}
