package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.UPDATE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_DELETE;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_UPDATE;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.expeditionconnector.ExpeditionDeviceConfiguration;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.adminconsole.FilterablePanelProvider;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.BaseCelltable;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.RefreshableSingleSelectionModel;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.panels.AbstractFilterablePanel;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.dto.OwnershipDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog.DialogConfig;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;

public class ExpeditionDeviceConfigurationsPanel extends FlowPanel implements FilterablePanelProvider<ExpeditionDeviceConfiguration>  {
    private final StringMessages stringMessages;
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final ErrorReporter errorReporter;
    private final CellTable<ExpeditionDeviceConfiguration> allDeviceConfigurations;
    private final LabeledAbstractFilterablePanel<ExpeditionDeviceConfiguration> filterDeviceConfigurationsPanel;
    private final RefreshableSingleSelectionModel<ExpeditionDeviceConfiguration> refreshableDeviceConfigurationsSelectionModel;
    private final UserService userService;

    public ExpeditionDeviceConfigurationsPanel(final Presenter presenter, final StringMessages stringMessages) {
        this.sailingServiceWrite = presenter.getSailingService();
        this.errorReporter = presenter.getErrorReporter();
        this.stringMessages = stringMessages;
        this.userService = presenter.getUserService();
        AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);
        allDeviceConfigurations = new BaseCelltable<>(/* pageSize */10000, tableRes);
        final ListDataProvider<ExpeditionDeviceConfiguration> filteredDeviceConfigurations = new ListDataProvider<>();
        ListHandler<ExpeditionDeviceConfiguration> deviceConfigurationColumnListHandler = new ListHandler<>(filteredDeviceConfigurations.getList());
        filteredDeviceConfigurations.addDataDisplay(allDeviceConfigurations);
        final List<ExpeditionDeviceConfiguration> emptyList = Collections.emptyList();
        filterDeviceConfigurationsPanel = new LabeledAbstractFilterablePanel<ExpeditionDeviceConfiguration>(new Label(stringMessages.expeditionDeviceConfigurations()),
                emptyList, filteredDeviceConfigurations, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(ExpeditionDeviceConfiguration t) {
                Set<String> strings = new HashSet<>();
                strings.add(t.getName());
                strings.add(t.getDeviceUuid().toString());
                strings.add(""+t.getExpeditionBoatId());
                return strings;
            }

            @Override
            public AbstractCellTable<ExpeditionDeviceConfiguration> getCellTable() {
                return allDeviceConfigurations;
            }
        };
        refreshableDeviceConfigurationsSelectionModel = new RefreshableSingleSelectionModel<>(new EntityIdentityComparator<ExpeditionDeviceConfiguration>() {
            @Override
            public boolean representSameEntity(ExpeditionDeviceConfiguration dto1, ExpeditionDeviceConfiguration dto2) {
                return dto1.getDeviceUuid().equals(dto2.getDeviceUuid());
            }

            @Override
            public int hashCode(ExpeditionDeviceConfiguration t) {
                return t.getDeviceUuid().hashCode();
            }
        }, filterDeviceConfigurationsPanel.getAllListDataProvider());
        allDeviceConfigurations.ensureDebugId("ExpeditionDeviceConfigurationsTable");
        allDeviceConfigurations.setSelectionModel(refreshableDeviceConfigurationsSelectionModel);
        final Panel controlsPanel = new HorizontalPanel();
        controlsPanel.add(filterDeviceConfigurationsPanel);
        add(controlsPanel);
        add(allDeviceConfigurations);
        Column<ExpeditionDeviceConfiguration, String> deviceConfigurationNameColumn = new TextColumn<ExpeditionDeviceConfiguration>() {
            @Override
            public String getValue(ExpeditionDeviceConfiguration object) {
                return object.getName();
            }
        };
        deviceConfigurationNameColumn.setSortable(true);
        deviceConfigurationColumnListHandler.setComparator(deviceConfigurationNameColumn, (c1, c2)->new NaturalComparator(/* caseSensitive */ false).compare(c1.getName(), c2.getName()));
        allDeviceConfigurations.addColumn(deviceConfigurationNameColumn, stringMessages.name());
        Column<ExpeditionDeviceConfiguration, String> deviceConfigurationUuidColumn = new TextColumn<ExpeditionDeviceConfiguration>() {
            @Override
            public String getValue(ExpeditionDeviceConfiguration object) {
                return object.getDeviceUuid().toString();
            }
        };
        deviceConfigurationUuidColumn.setSortable(true);
        deviceConfigurationColumnListHandler.setComparator(deviceConfigurationUuidColumn,
                (c1, c2)->new NaturalComparator(/* caseSensitive */ false).compare(c1.getDeviceUuid().toString(), c2.getDeviceUuid().toString()));
        allDeviceConfigurations.addColumn(deviceConfigurationUuidColumn, stringMessages.id());
        Column<ExpeditionDeviceConfiguration, String> deviceConfigurationBoatIdColumn = new TextColumn<ExpeditionDeviceConfiguration>() {
            @Override
            public String getValue(ExpeditionDeviceConfiguration object) {
                return object.getExpeditionBoatId() == null ? "" : object.getExpeditionBoatId().toString();
            }
        };
        deviceConfigurationBoatIdColumn.setSortable(true);
        deviceConfigurationColumnListHandler.setComparator(deviceConfigurationBoatIdColumn,
                Comparator.comparing(ExpeditionDeviceConfiguration::getExpeditionBoatId, Comparator.nullsLast(Comparator.naturalOrder())));
        allDeviceConfigurations.addColumn(deviceConfigurationBoatIdColumn, stringMessages.expeditionBoatId());
        final AccessControlledActionsColumn<ExpeditionDeviceConfiguration, DefaultActionsImagesBarCell> actionsColumn = create(
                new DefaultActionsImagesBarCell(stringMessages), userService);
        actionsColumn.addAction(ACTION_DELETE, DELETE, deviceConfiguration -> {
            if (Window.confirm(stringMessages
                    .doYouReallyWantToRemoveExpeditionDeviceConfiguration(deviceConfiguration.getName()))) {
                removeDeviceConfiguration(deviceConfiguration, filterDeviceConfigurationsPanel);
            }
        });
        actionsColumn.addAction(ACTION_UPDATE, UPDATE, deviceConfiguration -> {
            new EditDeviceConfigurationDialog(filterDeviceConfigurationsPanel, sailingServiceWrite, stringMessages,
                    errorReporter, deviceConfiguration).show();
        });
        final HasPermissions type = SecuredDomainType.EXPEDITION_DEVICE_CONFIGURATION;
        final DialogConfig<ExpeditionDeviceConfiguration> config = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), type, deviceConfiguration -> refresh(), stringMessages);
        actionsColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP,
                deviceConfiguration -> config.openOwnershipDialog(deviceConfiguration));
        final EditACLDialog.DialogConfig<ExpeditionDeviceConfiguration> configACL = EditACLDialog
                .create(userService.getUserManagementWriteService(), type, deviceConfiguration -> refresh(), stringMessages);
        actionsColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                deviceConfiguration -> configACL.openDialog(deviceConfiguration));
        SecuredDTOOwnerColumn.configureOwnerColumns(allDeviceConfigurations, deviceConfigurationColumnListHandler,
                stringMessages);
        allDeviceConfigurations.addColumn(actionsColumn, stringMessages.actions());
        allDeviceConfigurations.addColumnSortHandler(deviceConfigurationColumnListHandler);
        updateAllAccounts(sailingServiceWrite, filterDeviceConfigurationsPanel, stringMessages, errorReporter);
        Button addAccountButton = new Button(stringMessages.add());
        addAccountButton.ensureDebugId("addExpeditionDeviceConfiguration");
        addAccountButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addDeviceConfiguration();
            }
        });
        add(addAccountButton);
        Button refreshButton = new Button(stringMessages.refresh());
        refreshButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                refresh();
            }
        });
        add(refreshButton);
    }
    
    public void refresh() {
        updateAllAccounts(sailingServiceWrite, filterDeviceConfigurationsPanel, stringMessages, errorReporter);
    }

    private abstract class AbstractDeviceConfigurationDialog extends DataEntryDialog<ExpeditionDeviceConfiguration> {
        protected TextBox boatName;
        protected com.sap.sse.gwt.client.controls.IntegerBox boatId;
        
        public AbstractDeviceConfigurationDialog(final LabeledAbstractFilterablePanel<ExpeditionDeviceConfiguration> filterAccountsPanel,
                final SailingServiceAsync sailingService, final StringMessages stringMessages, final ErrorReporter errorReporter, String title,
                DialogCallback<ExpeditionDeviceConfiguration> callback) {
            super(title, title, stringMessages.ok(), stringMessages.cancel(),
                    new Validator<ExpeditionDeviceConfiguration>() {
                        @Override
                        public String getErrorMessage(ExpeditionDeviceConfiguration valueToValidate) {
                            final String errorMessage;
                            if (valueToValidate.getName() == null || valueToValidate.getName().isEmpty()) {
                                errorMessage = stringMessages.boatNameMustNotBeEmpty();
                            } else {
                                errorMessage = null;
                            }
                            return errorMessage;
                        }
                    }, /* animationEnabled */ true, callback);
        }

        @Override
        protected Widget getAdditionalWidget() {
            Grid grid = new Grid(2, 2);
            grid.setWidget(0, 0, new Label(stringMessages.name()));
            boatName = createTextBox("");
            boatName.ensureDebugId("expeditionBoatName");
            grid.setWidget(0, 1, boatName);
            grid.setWidget(1, 0, new Label(stringMessages.expeditionBoatId()));
            boatId = createIntegerBox(/* initial value */ 0, /* visibleLength */ 2);
            boatId.ensureDebugId("expeditionBoatId");
            grid.setWidget(1, 1, boatId);
            return grid;
        }
        
        @Override
        protected Focusable getInitialFocusWidget() {
            return boatName;
        }
    }

    private class AddDeviceConfigurationDialog extends AbstractDeviceConfigurationDialog {
        private final UserService userService;

        public AddDeviceConfigurationDialog(
                final LabeledAbstractFilterablePanel<ExpeditionDeviceConfiguration> filterAccountsPanel,
                final SailingServiceWriteAsync sailingServiceWrite, final UserService userService,
                final StringMessages stringMessages, final ErrorReporter errorReporter) {
            super(filterAccountsPanel, sailingServiceWrite,
                    stringMessages, errorReporter, stringMessages.addExpeditionDeviceConfiguration(),
                    new DialogCallback<ExpeditionDeviceConfiguration>() {
                @Override
                public void ok(final ExpeditionDeviceConfiguration editedObject) {
                    sailingServiceWrite.addOrReplaceExpeditionDeviceConfiguration(editedObject, new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(stringMessages.errorUpdatingExpeditionDeviceConfiguration(editedObject.getName(),
                                    caught.getMessage()));
                        }

                        @Override
                        public void onSuccess(Void result) {
                            filterAccountsPanel.add(editedObject);
                        }
                    });
                }

                @Override
                public void cancel() {
                }
            });
            this.userService = userService;
            ensureDebugId("AddExpeditionDeviceConfigurationDialog");
        }
        
        @Override
        protected ExpeditionDeviceConfiguration getResult() {
            final ExpeditionDeviceConfiguration result = new ExpeditionDeviceConfiguration(boatName.getText(), UUID.randomUUID(), boatId.getValue());
            result.setOwnership(new OwnershipDTO(userService.getCurrentUser().asStrippedUser(), userService.getCurrentTenant()));
            return result;
        }
    }

    private class EditDeviceConfigurationDialog extends AbstractDeviceConfigurationDialog {
        private final ExpeditionDeviceConfiguration valueToEdit;
        
        public EditDeviceConfigurationDialog(final LabeledAbstractFilterablePanel<ExpeditionDeviceConfiguration> filterAccountsPanel,
                final SailingServiceWriteAsync sailingServiceWrite, final StringMessages stringMessages, final ErrorReporter errorReporter,
                final ExpeditionDeviceConfiguration valueToEdit) {
            super(filterAccountsPanel, sailingServiceWrite, stringMessages, errorReporter, stringMessages.editExpeditionDeviceConfiguration(),
                    new DialogCallback<ExpeditionDeviceConfiguration>() {
                @Override
                public void ok(final ExpeditionDeviceConfiguration editedObject) {
                    sailingServiceWrite.addOrReplaceExpeditionDeviceConfiguration(editedObject, new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(stringMessages.errorUpdatingExpeditionDeviceConfiguration(editedObject.getName(),
                                    caught.getMessage()));
                        }

                        @Override
                        public void onSuccess(Void result) {
                            filterAccountsPanel.remove(valueToEdit);
                            filterAccountsPanel.add(editedObject);
                        }
                    });
                }

                @Override
                public void cancel() {
                }
            });
            this.valueToEdit = valueToEdit;
            ensureDebugId("EditExpeditionDeviceConfigurationDialog");
        }

        @Override
        protected Widget getAdditionalWidget() {
            final Widget result = super.getAdditionalWidget();
            boatName.setValue(valueToEdit.getName());
            boatId.setValue(valueToEdit.getExpeditionBoatId());
            return result;
        }

        @Override
        protected ExpeditionDeviceConfiguration getResult() {
            return new ExpeditionDeviceConfiguration(this.boatName.getValue(), valueToEdit.getDeviceUuid(), this.boatId.getValue());
        }
    }

    private static void updateAllAccounts(SailingServiceAsync sailingService, final LabeledAbstractFilterablePanel<ExpeditionDeviceConfiguration> filterAccountsPanel,
            final StringMessages stringMessages, final ErrorReporter errorReporter) {
        sailingService.getExpeditionDeviceConfigurations(new AsyncCallback<List<ExpeditionDeviceConfiguration>>() {
            @Override
            public void onSuccess(List<ExpeditionDeviceConfiguration> result) {
                filterAccountsPanel.updateAll(result);
            }
            
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorFetchingExpeditionDeviceConfigurations(caught.getMessage()));
            }
        });
    }

    private void addDeviceConfiguration() {
        new AddDeviceConfigurationDialog(filterDeviceConfigurationsPanel, sailingServiceWrite, userService, stringMessages, errorReporter).show();
    }
    
    private void removeDeviceConfiguration(final ExpeditionDeviceConfiguration expeditionDeviceConfiguration,
            final LabeledAbstractFilterablePanel<ExpeditionDeviceConfiguration> filterDeviceConfigurationsPanel) {
        sailingServiceWrite.removeExpeditionDeviceConfiguration(expeditionDeviceConfiguration, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorTryingToRemoveExpeditionDeviceConfiguration(expeditionDeviceConfiguration.getName(), caught.getMessage()));
            }

            @Override
            public void onSuccess(Void result) {
                filterDeviceConfigurationsPanel.remove(expeditionDeviceConfiguration);
            }
        });
    }
    
    @Override
    public AbstractFilterablePanel<ExpeditionDeviceConfiguration> getFilterablePanel() {
        return filterDeviceConfigurationsPanel;
    }
}
