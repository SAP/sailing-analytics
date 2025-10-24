package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.UPDATE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_DELETE;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_UPDATE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.DataEntryDialogWithDateTimeBox;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.IgtimiDataAccessWindowWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.IgtimiDeviceWithSecurityDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.adminconsole.FilterablePanelProvider;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.celltable.AbstractSortableTextColumn;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.FlushableCellTable;
import com.sap.sse.gwt.client.celltable.ImagesBarCell;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn;
import com.sap.sse.gwt.client.controls.busyindicator.BusyIndicator;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;
import com.sap.sse.gwt.client.controls.datetime.DateAndTimeInput;
import com.sap.sse.gwt.client.controls.datetime.DateTimeInput.Accuracy;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.AbstractFilterablePanel;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog.DialogConfig;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;

/**
 * @author Axel Uhl (d043530)
 */
public class IgtimiDevicesPanel extends FlowPanel implements FilterablePanelProvider<IgtimiDeviceWithSecurityDTO> {
    private final StringMessages stringMessages;
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final ErrorReporter errorReporter;
    private final LabeledAbstractFilterablePanel<IgtimiDeviceWithSecurityDTO> filterDevicesPanel;
    private final RefreshableMultiSelectionModel<IgtimiDeviceWithSecurityDTO> refreshableDevicesSelectionModel;
    private final LabeledAbstractFilterablePanel<IgtimiDataAccessWindowWithSecurityDTO> filterDataAccessWindowPanel;
    private final RefreshableMultiSelectionModel<IgtimiDataAccessWindowWithSecurityDTO> refreshableDataAccessWindowsSelectionModel;
    private final BusyIndicator busyIndicator;

    public static class AccountImagesBarCell extends ImagesBarCell {
        public static final String ACTION_REMOVE = "ACTION_REMOVE";
        private final StringMessages stringMessages;

        public AccountImagesBarCell(StringMessages stringMessages) {
            super();
            this.stringMessages = stringMessages;
        }

        public AccountImagesBarCell(SafeHtmlRenderer<String> renderer, StringMessages stringConstants) {
            super();
            this.stringMessages = stringConstants;
        }

        @Override
        protected Iterable<ImageSpec> getImageSpecs() {
            return Arrays.asList(new ImageSpec(ACTION_REMOVE, stringMessages.actionRemove(),
                    makeImagePrototype(IconResources.INSTANCE.removeIcon())));
        }
    }

    @SuppressWarnings("unchecked")
    public IgtimiDevicesPanel(final Presenter presenter,
            final StringMessages stringMessages) {
        this.sailingServiceWrite = presenter.getSailingService();
        this.errorReporter = presenter.getErrorReporter();
        this.stringMessages = stringMessages;
        final AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);
        final Label igtimiConnectionFactoryParams = new Label();
        final Anchor linkToRemoteRiotAdminPanel = new Anchor(stringMessages.configuration(), (String) null, /* target */ "_blank");
        final HorizontalPanel remoteRiotConfig = new HorizontalPanel();
        remoteRiotConfig.setSpacing(5);
        remoteRiotConfig.add(igtimiConnectionFactoryParams);
        remoteRiotConfig.add(linkToRemoteRiotAdminPanel);
        add(remoteRiotConfig);
        add(new Label(stringMessages.localServer()));
        sailingServiceWrite.getIgtimiConnectionFactoryBaseUrl(new AsyncCallback<Pair<String, Boolean>>() {
            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(stringMessages.errorGettingIgtimiConnectionFactoryParams(caught.getMessage()), NotificationType.ERROR);
            }

            @Override
            public void onSuccess(Pair<String, Boolean> result) {
                igtimiConnectionFactoryParams.setText(stringMessages.igtimiConnectionFactoryParams(result.getA(),
                        result.getB() ? stringMessages.yes() : stringMessages.no()));
                linkToRemoteRiotAdminPanel.setHref(result.getA()+"/gwt/AdminConsole.html#IgtimiDevicesPlace:");
            }
        });
        // setup devices table
        final CaptionPanel devicesCaptionPanel = new CaptionPanel(stringMessages.igtimiDevices());
        final VerticalPanel devicesCaptionPanelContents = new VerticalPanel();
        devicesCaptionPanel.add(devicesCaptionPanelContents);
        final FlushableCellTable<IgtimiDeviceWithSecurityDTO> devicesTable = new FlushableCellTable<>(/* pageSize */ 50, tableRes);
        final ListDataProvider<IgtimiDeviceWithSecurityDTO> filteredDevices = new ListDataProvider<>();
        filterDevicesPanel = new LabeledAbstractFilterablePanel<IgtimiDeviceWithSecurityDTO>(
                new Label(stringMessages.filterBy()), Collections.emptyList(), filteredDevices, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(IgtimiDeviceWithSecurityDTO t) {
                final Set<String> strings = new HashSet<>();
                strings.add(""+t.getId());
                if (t.getName() != null) {
                    strings.add(t.getName());
                }
                if (t.getSerialNumber() != null) {
                    strings.add(t.getSerialNumber());
                }
                return strings;
            }

            @Override
            public AbstractCellTable<IgtimiDeviceWithSecurityDTO> getCellTable() {
                return devicesTable;
            }
        };
        createIgtimiDevicesTable(devicesTable, tableRes, presenter.getUserService(), filteredDevices, filterDevicesPanel);
        // refreshableAccountsSelectionModel will be of correct type, see below in createIgtimiAccountsTable
        refreshableDevicesSelectionModel = (RefreshableMultiSelectionModel<IgtimiDeviceWithSecurityDTO>) devicesTable.getSelectionModel();
        final Panel devicesControlsPanel = new HorizontalPanel();
        filterDevicesPanel.setUpdatePermissionFilterForCheckbox(device -> presenter.getUserService().hasPermission(device, DefaultActions.UPDATE));
        devicesControlsPanel.add(filterDevicesPanel);
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(presenter.getUserService(), SecuredDomainType.IGTIMI_DEVICE);
        devicesControlsPanel.add(buttonPanel);
        busyIndicator = new SimpleBusyIndicator();
        devicesControlsPanel.add(busyIndicator);
        buttonPanel.addUnsecuredAction(stringMessages.refresh(), () -> refreshDevices());
        // setup controls
        final Button removeDeviceButton = buttonPanel.addRemoveAction(stringMessages.remove(), refreshableDevicesSelectionModel,
                /* with confirmation */ true, () -> {
            if (refreshableDevicesSelectionModel.getSelectedSet().size() > 0) {
                if (Window.confirm(stringMessages.doYouReallyWantToRemoveTheSelectedIgtimiDevices())) {
                    for (IgtimiDeviceWithSecurityDTO device : refreshableDevicesSelectionModel.getSelectedSet()) {
                        removeDevice(device, filteredDevices);
                    }
                }
            }
        });
        removeDeviceButton.setEnabled(false);
        devicesCaptionPanelContents.add(devicesControlsPanel);
        devicesCaptionPanelContents.add(devicesTable);
        add(devicesCaptionPanel);
        // set up Data Access Window table
        final CaptionPanel dataAccessWindowsCaptionPanel = new CaptionPanel(stringMessages.igtimiDataAccessWindows());
        final VerticalPanel dataAccessWindowsCaptionPanelContents = new VerticalPanel();
        dataAccessWindowsCaptionPanel.add(dataAccessWindowsCaptionPanelContents);
        final FlushableCellTable<IgtimiDataAccessWindowWithSecurityDTO> dawTable = new FlushableCellTable<>(/* pageSize */ 50, tableRes);
        final ListDataProvider<IgtimiDataAccessWindowWithSecurityDTO> filteredDAWs = new ListDataProvider<>();
        filterDataAccessWindowPanel = new LabeledAbstractFilterablePanel<IgtimiDataAccessWindowWithSecurityDTO>(
                new Label(stringMessages.filterBy()), Collections.emptyList(), filteredDAWs, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(IgtimiDataAccessWindowWithSecurityDTO t) {
                final Set<String> strings = new HashSet<>();
                strings.add(""+t.getId());
                if (t.getSerialNumber() != null) {
                    strings.add(t.getSerialNumber());
                }
                return strings;
            }

            @Override
            public AbstractCellTable<IgtimiDataAccessWindowWithSecurityDTO> getCellTable() {
                return dawTable;
            }
        };
        createIgtimiDataAccessWindowsTable(dawTable, tableRes, presenter.getUserService(), filteredDAWs, filterDataAccessWindowPanel);
        // refreshableAccountsSelectionModel will be of correct type, see below in createIgtimiAccountsTable
        refreshableDataAccessWindowsSelectionModel = (RefreshableMultiSelectionModel<IgtimiDataAccessWindowWithSecurityDTO>) dawTable.getSelectionModel();
        final Panel dawControlsPanel = new HorizontalPanel();
        filterDataAccessWindowPanel.setUpdatePermissionFilterForCheckbox(daw -> presenter.getUserService().hasPermission(daw, DefaultActions.UPDATE));
        dawControlsPanel.add(filterDataAccessWindowPanel);
        final AccessControlledButtonPanel dawButtonPanel = new AccessControlledButtonPanel(presenter.getUserService(), SecuredDomainType.IGTIMI_DATA_ACCESS_WINDOW);
        dawControlsPanel.add(dawButtonPanel);
        dawButtonPanel.addUnsecuredAction(stringMessages.refresh(), () -> refreshDataAccessWindows());
        // setup controls
        final Button removeDAWButton = dawButtonPanel.addRemoveAction(stringMessages.remove(), refreshableDataAccessWindowsSelectionModel,
                /* with confirmation */ true, () -> {
            if (refreshableDataAccessWindowsSelectionModel.getSelectedSet().size() > 0) {
                if (Window.confirm(stringMessages.doYouReallyWantToRemoveTheSelectedIgtimiDataAccessWindows())) {
                    for (IgtimiDataAccessWindowWithSecurityDTO daw : refreshableDataAccessWindowsSelectionModel.getSelectedSet()) {
                        removeDataAccessWindow(daw, filteredDAWs);
                    }
                }
            }
        });
        removeDAWButton.setEnabled(false);
        refreshableDevicesSelectionModel.addSelectionChangeHandler(
                e -> {
                    removeDeviceButton.setEnabled(refreshableDevicesSelectionModel.getSelectedSet().size() > 0);
                    final boolean exactlyOneDeviceSelected = refreshableDevicesSelectionModel.getSelectedSet().size() == 1;
                    dawTable.setVisible(exactlyOneDeviceSelected);
                    dawControlsPanel.setVisible(exactlyOneDeviceSelected);
                    if (exactlyOneDeviceSelected) {
                        filterDataAccessWindowPanel.search(refreshableDevicesSelectionModel.getSelectedSet().iterator().next().getSerialNumber());
                    }
                });
        refreshableDataAccessWindowsSelectionModel.addSelectionChangeHandler(
                e -> {
                    removeDAWButton.setEnabled(refreshableDataAccessWindowsSelectionModel.getSelectedSet().size() > 0);
                });
        dataAccessWindowsCaptionPanelContents.add(dawControlsPanel);
        dataAccessWindowsCaptionPanelContents.add(dawTable);
        add(dataAccessWindowsCaptionPanel);
        // "Add" button for DAWs
        final Button addDataAccessWindoweButton = dawButtonPanel.addCreateAction(stringMessages.addIgtimiDataAccessWindow(), () -> addDataAccessWindow());
        addDataAccessWindoweButton.ensureDebugId("addIgtimiDataAccessWindow");
        dawTable.setVisible(false); // make visible if and only if a single device is selected in the devices table
    }
    
    private static class DevicesImagesBarCell extends DefaultActionsImagesBarCell {
        public static final String ACTION_GPS_OFF = "ACTION_GPS_OFF";
        public static final String ACTION_GPS_ON = "ACTION_GPS_ON";
        public static final String ACTION_RESTART = "ACTION_RESTART";
        public static final String ACTION_POWER_OFF = "ACTION_POWER_OFF";
        public static final String ACTION_CALIBRATE = "ACTION_CALIBRATE";
        public static final String ACTION_SEND_FREESTYLE_COMMAND = "ACTION_SEND_FREESTYLE_COMMAND";
        private final StringMessages stringMessages;

        public DevicesImagesBarCell(StringMessages stringMessages) {
            super(stringMessages);
            this.stringMessages = stringMessages;
        }

        @Override
        protected Iterable<ImageSpec> getImageSpecs() {
            return Arrays.asList(getUpdateImageSpec(),
                    new ImageSpec(ACTION_GPS_OFF, stringMessages.turnGPSOff(), IconResources.INSTANCE.noGpsSymbol()),
                    new ImageSpec(ACTION_GPS_ON, stringMessages.turnGPSOn(), IconResources.INSTANCE.gpsSymbol()),
                    new ImageSpec(ACTION_RESTART, stringMessages.restart(), IconResources.INSTANCE.restartSymbol()),
                    new ImageSpec(ACTION_POWER_OFF, stringMessages.powerOff(), IconResources.INSTANCE.powerButton()),
                    new ImageSpec(ACTION_CALIBRATE, stringMessages.calibrateIMU(), IconResources.INSTANCE.compassSymbol()),
                    new ImageSpec(ACTION_SEND_FREESTYLE_COMMAND, stringMessages.sendFreestyleCommand(), IconResources.INSTANCE.commandSymbol()),
                    getDeleteImageSpec(), getChangeOwnershipImageSpec(), getChangeACLImageSpec());
        }
    }


    private void createIgtimiDevicesTable(
            final FlushableCellTable<IgtimiDeviceWithSecurityDTO> table, final CellTableWithCheckboxResources tableResources,
            final UserService userService, final ListDataProvider<IgtimiDeviceWithSecurityDTO> filteredDevices,
            final LabeledAbstractFilterablePanel<IgtimiDeviceWithSecurityDTO> filterDevicesPanel) {
        filteredDevices.addDataDisplay(table);
        final SelectionCheckboxColumn<IgtimiDeviceWithSecurityDTO> devicesSelectionCheckboxColumn = new SelectionCheckboxColumn<IgtimiDeviceWithSecurityDTO>(
                tableResources.cellTableStyle().cellTableCheckboxSelected(),
                tableResources.cellTableStyle().cellTableCheckboxDeselected(),
                tableResources.cellTableStyle().cellTableCheckboxColumnCell(),
                new EntityIdentityComparator<IgtimiDeviceWithSecurityDTO>() {
                    @Override
                    public boolean representSameEntity(IgtimiDeviceWithSecurityDTO a1, IgtimiDeviceWithSecurityDTO a2) {
                        return a1.getId() == a2.getId();
                    }

                    @Override
                    public int hashCode(IgtimiDeviceWithSecurityDTO t) {
                        return 7482 ^ (int) t.getId();
                    }
                }, filterDevicesPanel.getAllListDataProvider(), table);
        final ListHandler<IgtimiDeviceWithSecurityDTO> columnSortHandler = new ListHandler<>(filteredDevices.getList());
        table.addColumnSortHandler(columnSortHandler);
        columnSortHandler.setComparator(devicesSelectionCheckboxColumn, devicesSelectionCheckboxColumn.getComparator());
        final TextColumn<IgtimiDeviceWithSecurityDTO> deviceIdColumn = new AbstractSortableTextColumn<>(
                device -> ""+device.getId(), columnSortHandler);
        final TextColumn<IgtimiDeviceWithSecurityDTO> deviceNameColumn = new AbstractSortableTextColumn<>(
                device -> device.getName(), columnSortHandler);
        final TextColumn<IgtimiDeviceWithSecurityDTO> deviceSerialNumberColumn = new AbstractSortableTextColumn<>(
                device -> device.getSerialNumber(), columnSortHandler);
        final TextColumn<IgtimiDeviceWithSecurityDTO> lastHeartBeatColumn = new AbstractSortableTextColumn<>(
                device -> device.getLastHeartBeat()==null||device.getLastHeartBeat().getA()==null?"":device.getLastHeartBeat().getA().toString(), columnSortHandler,
                (a,b)->Util.compareToWithNull(a.getLastHeartBeat()==null?null:a.getLastHeartBeat().getA(), b.getLastHeartBeat()==null?null:b.getLastHeartBeat().getA(), /* null is less */ true));
        final TextColumn<IgtimiDeviceWithSecurityDTO> remoteAddressColumn = new AbstractSortableTextColumn<>(
                device -> device.getLastHeartBeat() == null ? null : device.getLastHeartBeat().getB(), columnSortHandler);
        final SafeHtmlCell lastKnownPositionCell = new SafeHtmlCell();
        final Column<IgtimiDeviceWithSecurityDTO, SafeHtml> lastKnownPositionColumn = new Column<IgtimiDeviceWithSecurityDTO, SafeHtml>(lastKnownPositionCell) {
            @Override
            public SafeHtml getValue(IgtimiDeviceWithSecurityDTO device) {
                final SafeHtmlBuilder builder = new SafeHtmlBuilder();
                if (device.getLastKnownPosition() != null) {
                    final String mapsUrl = "https://maps.google.com/maps?t=k&q=loc:"
                            +device.getLastKnownPosition().getLatDeg()
                            +","
                            +device.getLastKnownPosition().getLngDeg()
                            +"&ll="
                            +device.getLastKnownPosition().getLatDeg()
                            +","
                            +device.getLastKnownPosition().getLngDeg()
                            +"&z=14";
                    builder.appendHtmlConstant("<a href=\""+mapsUrl+"\" target=\"_blank\">");
                    builder.appendEscaped(device.getLastKnownPosition().toString());
                    builder.appendHtmlConstant("</a>");
                }
                return builder.toSafeHtml();
            }
        };
        final TextColumn<IgtimiDeviceWithSecurityDTO> lastKnownBatteryPercentColumn = new AbstractSortableTextColumn<>(
                device -> Double.isNaN(device.getLastKnownBatteryPercent()) ? "?" :
                    NumberFormat.getFormat("0.0").format(device.getLastKnownBatteryPercent()), columnSortHandler);
        final HasPermissions type = SecuredDomainType.IGTIMI_DEVICE;
        final AccessControlledActionsColumn<IgtimiDeviceWithSecurityDTO, DevicesImagesBarCell> actionColumn = create(
                new DevicesImagesBarCell(stringMessages), userService);
        actionColumn.addAction(ACTION_UPDATE, UPDATE, device -> {
            editDevice(device, filteredDevices);
        });
        actionColumn.addAction(DevicesImagesBarCell.ACTION_GPS_OFF, UPDATE, this::sendGPSOff);
        actionColumn.addAction(DevicesImagesBarCell.ACTION_GPS_ON, UPDATE, this::sendGPSOn);
        actionColumn.addAction(DevicesImagesBarCell.ACTION_RESTART, UPDATE, this::sendRestart);
        actionColumn.addAction(DevicesImagesBarCell.ACTION_POWER_OFF, UPDATE, this::sendPowerOff);
        actionColumn.addAction(DevicesImagesBarCell.ACTION_CALIBRATE, UPDATE, this::sendIMUCalibrationCommandSequence);
        actionColumn.addAction(DevicesImagesBarCell.ACTION_SEND_FREESTYLE_COMMAND, UPDATE, this::sendFreestyleCommands);
        actionColumn.addAction(ACTION_DELETE, DELETE, device -> {
            if (Window.confirm(stringMessages.doYouReallyWantToRemoveIgtimiDevice(device.getSerialNumber()))) {
                removeDevice(device, filteredDevices);
            }
        });
        final DialogConfig<IgtimiDeviceWithSecurityDTO> config = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), type, roleDefinition -> refreshDevices(), stringMessages);
        actionColumn.addAction(ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP, config::openOwnershipDialog);
        final EditACLDialog.DialogConfig<IgtimiDeviceWithSecurityDTO> configACL = EditACLDialog
                .create(userService.getUserManagementWriteService(), type, roleDefinition -> refreshDevices(), stringMessages);
        actionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                configACL::openDialog);
        // add columns to table:
        table.addColumn(devicesSelectionCheckboxColumn, devicesSelectionCheckboxColumn.getHeader());
        table.addColumn(deviceIdColumn, stringMessages.id());
        table.addColumn(deviceNameColumn, stringMessages.name());
        table.addColumn(deviceSerialNumberColumn, stringMessages.serialNumber());
        table.addColumn(lastHeartBeatColumn, stringMessages.lastHeartBeat());
        table.addColumn(remoteAddressColumn, stringMessages.remoteAddress());
        table.addColumn(lastKnownPositionColumn, stringMessages.position());
        table.addColumn(lastKnownBatteryPercentColumn, stringMessages.batteryPercent());
        SecuredDTOOwnerColumn.configureOwnerColumns(table, columnSortHandler, stringMessages);
        table.addColumn(actionColumn, stringMessages.actions());
        table.setSelectionModel(devicesSelectionCheckboxColumn.getSelectionModel(),
                devicesSelectionCheckboxColumn.getSelectionManager());
    }
    
    private void editDevice(IgtimiDeviceWithSecurityDTO device, ListDataProvider<IgtimiDeviceWithSecurityDTO> filteredDevices) {
        new DataEntryDialog<IgtimiDeviceWithSecurityDTO>(stringMessages.editDevice(), stringMessages.editDevice(),
                stringMessages.ok(), stringMessages.cancel(),
                /* validator */ null, new DialogCallback<IgtimiDeviceWithSecurityDTO>() {
                    @Override
                    public void ok(IgtimiDeviceWithSecurityDTO editedObject) {
                        filteredDevices.getList().remove(device);
                        filteredDevices.getList().add(editedObject);
                        sailingServiceWrite.updateIgtimiDevice(editedObject, new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                Notification.notify(stringMessages.errorUpdatingIgtimiDevice(caught.getMessage()), NotificationType.ERROR);
                            }

                            @Override
                            public void onSuccess(Void result) {
                                Notification.notify(stringMessages.successfullyUpdatedIgtimiDevice(editedObject.getSerialNumber()), NotificationType.INFO);
                            }
                            
                        });
                    }

                    @Override
                    public void cancel() {
                    }
            
        }) {
            private TextBox nameField = createTextBox(device.getName());
            
            @Override
            protected Widget getAdditionalWidget() {
                final HorizontalPanel result = new HorizontalPanel();
                result.setSpacing(3);
                result.add(new Label(stringMessages.name()));
                result.add(nameField);
                return result;
            }

            @Override
            protected Focusable getInitialFocusWidget() {
                return nameField;
            }

            @Override
            protected IgtimiDeviceWithSecurityDTO getResult() {
                final IgtimiDeviceWithSecurityDTO result = new IgtimiDeviceWithSecurityDTO(
                        device.getId(), device.getSerialNumber(),
                        nameField.getText(), device.getLastHeartBeat(),
                        device.getLastKnownPosition(), device.getLastKnownBatteryPercent());
                result.setAccessControlList(device.getAccessControlList());
                result.setOwnership(device.getOwnership());
                return result;
            }
        }.show();
    }

    private FlushableCellTable<IgtimiDataAccessWindowWithSecurityDTO> createIgtimiDataAccessWindowsTable(
            final FlushableCellTable<IgtimiDataAccessWindowWithSecurityDTO> table, final CellTableWithCheckboxResources tableResources,
            final UserService userService, final ListDataProvider<IgtimiDataAccessWindowWithSecurityDTO> filteredDAWs,
            final LabeledAbstractFilterablePanel<IgtimiDataAccessWindowWithSecurityDTO> filterDataAccessWindowsPanel) {
        filteredDAWs.addDataDisplay(table);
        final SelectionCheckboxColumn<IgtimiDataAccessWindowWithSecurityDTO> dawsSelectionCheckboxColumn = new SelectionCheckboxColumn<IgtimiDataAccessWindowWithSecurityDTO>(
                tableResources.cellTableStyle().cellTableCheckboxSelected(),
                tableResources.cellTableStyle().cellTableCheckboxDeselected(),
                tableResources.cellTableStyle().cellTableCheckboxColumnCell(),
                new EntityIdentityComparator<IgtimiDataAccessWindowWithSecurityDTO>() {
                    @Override
                    public boolean representSameEntity(IgtimiDataAccessWindowWithSecurityDTO a1, IgtimiDataAccessWindowWithSecurityDTO a2) {
                        return a1.getId() == a2.getId();
                    }

                    @Override
                    public int hashCode(IgtimiDataAccessWindowWithSecurityDTO t) {
                        return 7482 ^ (int) t.getId();
                    }
                }, filterDataAccessWindowsPanel.getAllListDataProvider(), table);
        final ListHandler<IgtimiDataAccessWindowWithSecurityDTO> columnSortHandler = new ListHandler<>(filteredDAWs.getList());
        table.addColumnSortHandler(columnSortHandler);
        columnSortHandler.setComparator(dawsSelectionCheckboxColumn, dawsSelectionCheckboxColumn.getComparator());
        final TextColumn<IgtimiDataAccessWindowWithSecurityDTO> dawIdColumn = new AbstractSortableTextColumn<>(
                daw -> ""+daw.getId(), columnSortHandler);
        final TextColumn<IgtimiDataAccessWindowWithSecurityDTO> dawSerialNumberColumn = new AbstractSortableTextColumn<>(
                daw -> daw.getSerialNumber(), columnSortHandler);
        final TextColumn<IgtimiDataAccessWindowWithSecurityDTO> dawFromColumn = new AbstractSortableTextColumn<>(
                daw -> daw.getFrom().toString(), columnSortHandler, (daw1, daw2)->Util.compareToWithNull(daw1.getFrom(), daw2.getFrom(), /* null is less */ true));
        final TextColumn<IgtimiDataAccessWindowWithSecurityDTO> dawToColumn = new AbstractSortableTextColumn<>(
                daw -> daw.getTo().toString(), columnSortHandler, (daw1, daw2)->Util.compareToWithNull(daw1.getTo(), daw2.getTo(), /* null is less */ false));
        final HasPermissions type = SecuredDomainType.IGTIMI_DATA_ACCESS_WINDOW;
        final AccessControlledActionsColumn<IgtimiDataAccessWindowWithSecurityDTO, DefaultActionsImagesBarCell> actionColumn = create(
                new DefaultActionsImagesBarCell(stringMessages), userService);
        actionColumn.addAction(ACTION_DELETE, DELETE, daw -> {
            if (Window.confirm(stringMessages.doYouReallyWantToRemoveIgtimiDataAccessWindow(daw.getSerialNumber(), daw.getFrom().toString(), daw.getTo().toString()))) {
                removeDataAccessWindow(daw, filteredDAWs);
            }
        });
        final DialogConfig<IgtimiDataAccessWindowWithSecurityDTO> config = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), type, roleDefinition -> refreshDataAccessWindows(), stringMessages);
        actionColumn.addAction(ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP, config::openOwnershipDialog);
        final EditACLDialog.DialogConfig<IgtimiDataAccessWindowWithSecurityDTO> configACL = EditACLDialog
                .create(userService.getUserManagementWriteService(), type, roleDefinition -> refreshDataAccessWindows(), stringMessages);
        actionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                configACL::openDialog);
        // add columns to table:
        table.addColumn(dawsSelectionCheckboxColumn, dawsSelectionCheckboxColumn.getHeader());
        table.addColumn(dawIdColumn, stringMessages.id());
        table.addColumn(dawSerialNumberColumn, stringMessages.serialNumber());
        table.addColumn(dawFromColumn, stringMessages.from());
        table.addColumn(dawToColumn, stringMessages.to());
        SecuredDTOOwnerColumn.configureOwnerColumns(table, columnSortHandler, stringMessages);
        table.addColumn(actionColumn, stringMessages.actions());
        table.setSelectionModel(dawsSelectionCheckboxColumn.getSelectionModel(),
                dawsSelectionCheckboxColumn.getSelectionManager());
        return table;
    }

    public void refreshDevices() {
        busyIndicator.setBusy(true);
        sailingServiceWrite.getAllIgtimiDevicesWithSecurity(new AsyncCallback<ArrayList<IgtimiDeviceWithSecurityDTO>>() {
            @Override
            public void onSuccess(ArrayList<IgtimiDeviceWithSecurityDTO> result) {
                busyIndicator.setBusy(false);
                filterDevicesPanel.updateAll(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                busyIndicator.setBusy(false);
                errorReporter.reportError(stringMessages.errorFetchingIgtimiDevices(caught.getMessage()));
            }
        });
    }

    public void refreshDataAccessWindows() {
        sailingServiceWrite.getAllIgtimiDataAccessWindowsWithSecurity(new AsyncCallback<ArrayList<IgtimiDataAccessWindowWithSecurityDTO>>() {
            @Override
            public void onSuccess(ArrayList<IgtimiDataAccessWindowWithSecurityDTO> result) {
                filterDataAccessWindowPanel.updateAll(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorFetchingIgtimiDataAccessWindows(caught.getMessage()));
            }
        });
    }

    private static class DataAccessWindowData {
        private final String deviceSerialNumber;
        private final Date from;
        private final Date to;

        protected DataAccessWindowData(String deviceSerialNumber, Date from, Date to) {
            super();
            this.deviceSerialNumber = deviceSerialNumber;
            this.from = from;
            this.to = to;
        }

        protected String getDeviceSerialNumber() {
            return deviceSerialNumber;
        }

        protected Date getFrom() {
            return from;
        }

        protected Date getTo() {
            return to;
        }
    }

    private class AddDataAccessWindowDialog extends DataEntryDialogWithDateTimeBox<DataAccessWindowData> {
        private SuggestBox deviceSerialNumber;
        private DateAndTimeInput from;
        private DateAndTimeInput to;

        public AddDataAccessWindowDialog(final Runnable refresher, final SailingServiceWriteAsync sailingServiceWrite,
                final StringMessages stringMessages, final ErrorReporter errorReporter) {
            super(stringMessages.addIgtimiDataAccessWindow(), stringMessages.addIgtimiDataAccessWindow(), stringMessages.ok(),
                    stringMessages.cancel(), new Validator<DataAccessWindowData>() {
                        @Override
                        public String getErrorMessage(DataAccessWindowData valueToValidate) {
                            final String errorMessage;
                            if (!Util.hasLength(valueToValidate.getDeviceSerialNumber())) {
                                errorMessage = stringMessages.deviceSerialNumberMustNotBeEmpty();
                            } else if (valueToValidate.getFrom() == null) {
                                errorMessage = stringMessages.fromTimeNotSet();
                            } else if (valueToValidate.getTo() == null) {
                                errorMessage = stringMessages.toTimeNotSet();
                            } else {
                                errorMessage = null;
                            }
                            return errorMessage;
                        }
                    }, /* animationEnabled */ true, new DialogCallback<DataAccessWindowData>() {
                        @Override
                        public void ok(final DataAccessWindowData editedObject) {
                            sailingServiceWrite.addIgtimiDataAccessWindow(editedObject.getDeviceSerialNumber(),
                                    editedObject.getFrom(), editedObject.getTo(), new AsyncCallback<IgtimiDataAccessWindowWithSecurityDTO>() {
                                        @Override
                                        public void onFailure(Throwable caught) {
                                            errorReporter.reportError(stringMessages.errorCreatingDataAccessWindow(
                                                    editedObject.getDeviceSerialNumber(), caught.getMessage()));
                                        }

                                        @Override
                                        public void onSuccess(IgtimiDataAccessWindowWithSecurityDTO result) {
                                            if (result != null) {
                                                Notification
                                                        .notify(stringMessages.successfullyCreatedIgtimiDataAccessWindow(
                                                                editedObject.getDeviceSerialNumber()), NotificationType.SUCCESS);
                                                refresher.run();
                                            } else {
                                                Notification.notify(stringMessages.couldNotAuthorizedAccessToIgtimiUser(
                                                        editedObject.getDeviceSerialNumber()), NotificationType.ERROR);
                                            }
                                        }
                                    });
                        }

                        @Override
                        public void cancel() {
                        }

                    });
            ensureDebugId("AddIgtimiDeviceDialog");
        }

        @Override
        protected Widget getAdditionalWidget() {
            Grid grid = new Grid(3, 2);
            grid.setWidget(0, 0, new Label(stringMessages.serialNumber()));
            deviceSerialNumber = createSuggestBox(
                    Util.stream(filterDevicesPanel.getAll()).map(d->d.getSerialNumber()).sorted()::iterator);
            deviceSerialNumber.ensureDebugId("igtimiDeviceSerialNumber");
            if (refreshableDevicesSelectionModel.getSelectedSet().size() == 1) {
                deviceSerialNumber.setText(refreshableDevicesSelectionModel.getSelectedSet().iterator().next().getSerialNumber());
            }
            grid.setWidget(0, 1, deviceSerialNumber);
            grid.setWidget(1, 0, new Label(stringMessages.from()));
            from = createDateTimeBox(/* initial value */ null, Accuracy.SECONDS);
            from.ensureDebugId("igtimiDataAccessWindowFrom");
            grid.setWidget(1, 1, from);
            grid.setWidget(2, 0, new Label(stringMessages.to()));
            to = createDateTimeBox(/* initial value */ null, Accuracy.SECONDS);
            to.ensureDebugId("igtimiDataAccessWindowTo");
            grid.setWidget(2, 1, to);
            return grid;
        }

        @Override
        protected Focusable getInitialFocusWidget() {
            return deviceSerialNumber;
        }

        @Override
        protected DataAccessWindowData getResult() {
            return new DataAccessWindowData(deviceSerialNumber.getText(), from.getValue(), to.getValue());
        }
    }

    private void addDataAccessWindow() {
        new AddDataAccessWindowDialog(this::refreshDataAccessWindows, sailingServiceWrite, stringMessages, errorReporter).show();
    }

    private void removeDevice(final IgtimiDeviceWithSecurityDTO device,
            final ListDataProvider<IgtimiDeviceWithSecurityDTO> removeFrom) {
        sailingServiceWrite.removeIgtimiDevice(device.getSerialNumber(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorTryingToRemoveIgtimiDevice(device.getSerialNumber(), caught.getMessage()));
            }

            @Override
            public void onSuccess(Void result) {
                Notification.notify(stringMessages.successfullyRemoveIgtimiDevice(device.getSerialNumber()),
                        NotificationType.INFO);
                removeFrom.getList().remove(device);
            }
        });
    }

    private void removeDataAccessWindow(final IgtimiDataAccessWindowWithSecurityDTO daw,
            final ListDataProvider<IgtimiDataAccessWindowWithSecurityDTO> removeFrom) {
        sailingServiceWrite.removeIgtimiDataAccessWindow(daw.getId(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorTryingToRemoveIgtimiDataAccessWindow(Long.toString(daw.getId()), caught.getMessage()));
            }

            @Override
            public void onSuccess(Void result) {
                Notification.notify(stringMessages.successfullyRemovedIgtimiDataAccessWindow(Long.toString(daw.getId())), NotificationType.INFO);
                removeFrom.getList().remove(daw);
            }
        });
    }

    @Override
    public AbstractFilterablePanel<IgtimiDeviceWithSecurityDTO> getFilterablePanel() {
        return filterDevicesPanel;
    }

    private void sendGPSOff(IgtimiDeviceWithSecurityDTO device) {
        if (Window.confirm(stringMessages.reallyTurnGPSOffForIgtimiDevice(device.getSerialNumber()))) {
            sailingServiceWrite.sendGPSOffCommandToIgtimiDevice(device.getSerialNumber(), new AsyncCallback<Boolean>() {
                @Override
                public void onFailure(Throwable caught) {
                    Notification.notify(stringMessages.errorTurningGPSOffForIgtimiDevice(device.getSerialNumber(), caught.getMessage()), NotificationType.ERROR);
                }

                @Override
                public void onSuccess(Boolean result) {
                    if (result) {
                        Notification.notify(stringMessages.successfullyTurnedGPSOffForIgtimiDevice(device.getSerialNumber()), NotificationType.INFO);
                    } else {
                        Notification.notify(stringMessages.noLiveConnectionFoundForIgtimiDevice(device.getSerialNumber()), NotificationType.WARNING);
                    }
                }
            });
        }
    }

    private void sendGPSOn(IgtimiDeviceWithSecurityDTO device) {
        if (Window.confirm(stringMessages.reallyTurnGPSOnForIgtimiDevice(device.getSerialNumber()))) {
            sailingServiceWrite.sendGPSOnCommandToIgtimiDevice(device.getSerialNumber(), new AsyncCallback<Boolean>() {
                @Override
                public void onFailure(Throwable caught) {
                    Notification.notify(stringMessages.errorTurningGPSOnForIgtimiDevice(device.getSerialNumber(), caught.getMessage()), NotificationType.ERROR);
                }

                @Override
                public void onSuccess(Boolean result) {
                    if (result) {
                        Notification.notify(stringMessages.successfullyTurnedGPSOnForIgtimiDevice(device.getSerialNumber()), NotificationType.INFO);
                    } else {
                        Notification.notify(stringMessages.noLiveConnectionFoundForIgtimiDevice(device.getSerialNumber()), NotificationType.WARNING);
                    }
                }
            });
        }
    }

    private void sendPowerOff(IgtimiDeviceWithSecurityDTO device) {
        if (Window.confirm(stringMessages.reallyPowerOffIgtimiDevice(device.getSerialNumber()))) {
            sailingServiceWrite.sendPowerOffCommandToIgtimiDevice(device.getSerialNumber(), new AsyncCallback<Boolean>() {
                @Override
                public void onFailure(Throwable caught) {
                    Notification.notify(stringMessages.errorPoweringOffIgtimiDevice(device.getSerialNumber(), caught.getMessage()), NotificationType.ERROR);
                }

                @Override
                public void onSuccess(Boolean result) {
                    if (result) {
                        Notification.notify(stringMessages.successfullyPoweredOffIgtimiDevice(device.getSerialNumber()), NotificationType.INFO);
                    } else {
                        Notification.notify(stringMessages.noLiveConnectionFoundForIgtimiDevice(device.getSerialNumber()), NotificationType.WARNING);
                    }
                }
            });
        }
    }

    private void sendRestart(IgtimiDeviceWithSecurityDTO device) {
        if (Window.confirm(stringMessages.reallyRestartIgtimiDevice(device.getSerialNumber()))) {
            sailingServiceWrite.sendRestartCommandToIgtimiDevice(device.getSerialNumber(), new AsyncCallback<Boolean>() {
                @Override
                public void onFailure(Throwable caught) {
                    Notification.notify(stringMessages.errorRestartingIgtimiDevice(device.getSerialNumber(), caught.getMessage()), NotificationType.ERROR);
                }

                @Override
                public void onSuccess(Boolean result) {
                    if (result) {
                        Notification.notify(stringMessages.successfullyRestartedIgtimiDevice(device.getSerialNumber()), NotificationType.INFO);
                    } else {
                        Notification.notify(stringMessages.noLiveConnectionFoundForIgtimiDevice(device.getSerialNumber()), NotificationType.WARNING);
                    }
                }
            });
        }
    }
    
    private void sendIMUCalibrationCommandSequence(IgtimiDeviceWithSecurityDTO device) {
        if (Window.confirm(stringMessages.reallyRunCalibrationOnIgtimiDevice(device.getSerialNumber()))) {
            sailingServiceWrite.sendIMUCalibrationCommandSequenceToIgtimiDevice(device.getSerialNumber(), new AsyncCallback<Boolean>() {
                @Override
                public void onFailure(Throwable caught) {
                    Notification.notify(stringMessages.errorCalibratingIgtimiDevice(device.getSerialNumber(), caught.getMessage()), NotificationType.ERROR);
                }

                @Override
                public void onSuccess(Boolean result) {
                    if (result) {
                        Notification.notify(stringMessages.successfullyCalibratedIgtimiDevice(device.getSerialNumber()), NotificationType.INFO);
                    } else {
                        Notification.notify(stringMessages.noLiveConnectionFoundForIgtimiDevice(device.getSerialNumber()), NotificationType.WARNING);
                    }
                }
            });
        }
    }
    
    private void sendFreestyleCommands(IgtimiDeviceWithSecurityDTO device) {
        new FreestyleCommandDialog(stringMessages, sailingServiceWrite, device.getSerialNumber()).show();
    }
}
