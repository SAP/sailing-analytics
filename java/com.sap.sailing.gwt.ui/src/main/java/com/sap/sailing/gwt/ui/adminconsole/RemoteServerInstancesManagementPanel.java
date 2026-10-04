package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_DELETE;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_UPDATE;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.MultiSelectionModel;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventBaseDTO;
import com.sap.sailing.gwt.ui.shared.RemoteSailingServerReferenceDTO;
import com.sap.sse.gwt.adminconsole.FilterablePanelProvider;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.AbstractFilterablePanel;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;
import com.sap.sse.security.ui.client.component.SelectedElementsCountingButton;

public class RemoteServerInstancesManagementPanel extends SimplePanel implements FilterablePanelProvider<RemoteSailingServerReferenceDTO>{
    private final SailingServiceWriteAsync sailingService;
    private final UserService userService;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private RemoteServerInstancesManagementTableWrapper remoteServersTable;
    private MultiSelectionModel<RemoteSailingServerReferenceDTO> refreshableServerSelectionModel;
    private final CaptionPanel remoteServersPanel;

    public RemoteServerInstancesManagementPanel(final Presenter presenter, StringMessages stringMessages, CellTableWithCheckboxResources tableResources) {
        this.sailingService = presenter.getSailingService();
        this.userService = presenter.getUserService();
        this.errorReporter = presenter.getErrorReporter();
        this.stringMessages = stringMessages;
        VerticalPanel mainPanel = new VerticalPanel();
        setWidget(mainPanel);
        mainPanel.setWidth("100%");
        remoteServersPanel = new CaptionPanel(stringMessages.registeredSailingServerInstances());
        mainPanel.add(remoteServersPanel);
        VerticalPanel remoteServersContentPanel = new VerticalPanel();
        remoteServersPanel.setContentWidget(remoteServersContentPanel);
        remoteServersTable = createRemoteServersTable(tableResources);
        remoteServersContentPanel.add(remoteServersTable);
        remoteServersContentPanel.add(createButtonToolbar());
        refreshSailingServerList();
    }

    private Panel createButtonToolbar() {
        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setSpacing(5);
        buttonPanel.add(new Button(stringMessages.add(), (ClickHandler) event -> addRemoteSailingServerReference()));
        buttonPanel.add(createRemoveButton(buttonPanel, event -> removeSelectedSailingServers()));
        buttonPanel.add(new Button(stringMessages.refresh(), (ClickHandler) event -> refreshSailingServerList()));
        return buttonPanel;
    }

    private Button createRemoveButton(HorizontalPanel buttonPanel, ClickHandler handler) {
        return new SelectedElementsCountingButton<RemoteSailingServerReferenceDTO>(stringMessages.remove(),
                refreshableServerSelectionModel, StringMessages.INSTANCE::doYouReallyWantToRemoveSelectedElements,
                (ClickHandler) event -> removeSelectedSailingServers());
    }

    private RemoteServerInstancesManagementTableWrapper createRemoteServersTable(
            CellTableWithCheckboxResources tableResources) {
        RemoteServerInstancesManagementTableWrapper wrapper = new RemoteServerInstancesManagementTableWrapper(
                stringMessages, errorReporter, tableResources);
        wrapper.addColumn(RemoteSailingServerReferenceDTO::getName, stringMessages.name());
        wrapper.addColumn(RemoteSailingServerReferenceDTO::getUrl, stringMessages.url());
        wrapper.addColumn(createEventsColumn(), stringMessages.events());
        wrapper.addColumn(createActionsColumn(), stringMessages.actions());
        wrapper.setEmptyTableWidget(new Label(stringMessages.noSailingServerInstancesYet()));
        refreshableServerSelectionModel = wrapper.getSelectionModel();
        return wrapper;
    }

    private Column<RemoteSailingServerReferenceDTO, SafeHtml> createEventsColumn() {
        return new Column<RemoteSailingServerReferenceDTO, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(RemoteSailingServerReferenceDTO server) {
                SafeHtmlBuilder builder = new SafeHtmlBuilder();
                final Iterable<EventBaseDTO> events = server.getEvents();
                if (events != null) {
                    for (EventBaseDTO event : events) {
                        builder.appendEscaped(event.getName());
                        builder.appendHtmlConstant("<br>");
                    }
                } else {
                    builder.appendEscaped(RemoteServerInstancesManagementPanel.this.stringMessages
                            .errorAddingSailingServer(server.getLastErrorMessage()));
                }
                return builder.toSafeHtml();
            }
        };
    }

    private ServerActionsColumn<RemoteSailingServerReferenceDTO, DefaultActionsImagesBarCell> createActionsColumn() {
        final ServerActionsColumn<RemoteSailingServerReferenceDTO, DefaultActionsImagesBarCell> actionsColumn = ServerActionsColumn
                .create(new DefaultActionsImagesBarCell(stringMessages) {
                    @Override
                    protected ImageSpec getUpdateImageSpec() {
                        return new ImageSpec(ACTION_UPDATE, stringMessages.actionExcludeEvents(),
                                IconResources.INSTANCE.editIcon());
                    }

                }, userService);
        actionsColumn.addAction(ACTION_DELETE, ServerActions.CONFIGURE_REMOTE_INSTANCES, e -> {
            if (Window.confirm(StringMessages.INSTANCE.doYouReallyWantToRemoveSelectedElements(e.getName()))) {
                Set<String> toDelete = new HashSet<>();
                toDelete.add(e.getName());
                removeSailingServers(toDelete);
            }
        });
        actionsColumn.addAction(ACTION_UPDATE, ServerActions.CONFIGURE_REMOTE_INSTANCES, loadedSalingServer -> {
            sailingService.getCompleteRemoteServerReference(loadedSalingServer.getName(),
                    new AsyncCallback<RemoteSailingServerReferenceDTO>() {
                        @Override
                        public void onSuccess(RemoteSailingServerReferenceDTO completeServerReference) {
                            new RemoteSailingServerEventsSelectionDialog(completeServerReference, loadedSalingServer,
                                    stringMessages, new DialogCallback<RemoteSailingServerReferenceDTO>() {
                                        @Override
                                        public void ok(RemoteSailingServerReferenceDTO editedObject) {
                                            sailingService.updateRemoteSailingServerReference(editedObject,
                                                    new AsyncCallback<RemoteSailingServerReferenceDTO>() {
                                                        @Override
                                                        public void onSuccess(RemoteSailingServerReferenceDTO result) {
                                                            refreshSailingServerList();
                                                        }

                                                        @Override
                                                        public void onFailure(Throwable caught) {
                                                            errorReporter.reportError(
                                                                    "Error trying to update remote server with selected events: "
                                                                            + caught.getMessage());
                                                        }
                                                    });
                                        }

                                        @Override
                                        public void cancel() {
                                        }
                                    }).show();
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(
                                    "Error trying to load complete remote server reference: " + caught.getMessage());
                        }
                    });
        });
        return actionsColumn;
    }

    private void refreshSailingServerList() {
        sailingService.getRemoteSailingServerReferences(createCallback(stringMessages::errorRefreshingSailingServers,
                remoteServersTable.getFilterPanel()::updateAll, false));
    }

    private void removeSelectedSailingServers() {
        Set<String> toRemove = new HashSet<String>();
        for (RemoteSailingServerReferenceDTO selectedServer : refreshableServerSelectionModel.getSelectedSet()) {
            toRemove.add(selectedServer.getName());
        }
        removeSailingServers(toRemove);
    }

    private void removeSailingServers(Set<String> toRemove) {
        sailingService.removeSailingServers(toRemove, createCallback(stringMessages::errorRemovingSailingServers,
                (result) -> refreshSailingServerList(), true));
    }

    private void addRemoteSailingServerReference() {
        new SailingServerCreateOrEditDialog(remoteServersTable.getFilterPanel().getAll(), stringMessages,
                new DialogCallback<RemoteSailingServerReferenceDTO>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(final RemoteSailingServerReferenceDTO server) {
                        sailingService.addRemoteSailingServerReference(server,
                                createCallback(stringMessages::errorAddingSailingServer,
                                        remoteServersTable.getFilterPanel()::add, true));
                    }
                }).show();
    }

    private <T> AsyncCallback<T> createCallback(Function<String, String> errorMapper, Consumer<T> resultConsumer,
            boolean notify) {
        return new AsyncCallback<T>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(errorMapper.apply(caught.getMessage()));
            }

            @Override
            public void onSuccess(T result) {
                resultConsumer.accept(result);
                if (notify) {
                    Notification.notify(stringMessages.successfullyUpdatedSailingServers(), NotificationType.INFO);
                }
            }
        };
    }

    @Override
    public AbstractFilterablePanel<RemoteSailingServerReferenceDTO> getFilterablePanel() {
        return remoteServersTable.getFilterPanel();
    }
}
