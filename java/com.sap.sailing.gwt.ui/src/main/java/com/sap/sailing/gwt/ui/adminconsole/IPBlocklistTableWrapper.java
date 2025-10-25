package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.TimedLock;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.RefreshableSelectionModel;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;

abstract class IPBlocklistTableWrapper
        extends TableWrapper<IpToTimedLockDTO, RefreshableSelectionModel<IpToTimedLockDTO>> {
    private final UserService userService;
    private final LabeledAbstractFilterablePanel<IpToTimedLockDTO> filterField;
    private final HasPermissions securedDomainType;
    private final String errorMessageOnDataFailureString;

    protected abstract void fetchData(AsyncCallback<HashMap<String, TimedLock>> callback);

    protected abstract void unlockIP(String ip, AsyncCallback<Void> asyncCallback);

    public IPBlocklistTableWrapper(final SailingServiceWriteAsync sailingServiceWrite, final UserService userService,
            final HasPermissions securedDomainType, final String errorMessageOnDataFailureString,
            final StringMessages stringMessages, final ErrorReporter errorReporter) {
        super(sailingServiceWrite, stringMessages, errorReporter, true, true,
                new EntityIdentityComparator<IpToTimedLockDTO>() {
                    @Override
                    public boolean representSameEntity(IpToTimedLockDTO dto1, IpToTimedLockDTO dto2) {
                        return dto1.ip.equals(dto2.ip);
                    }

                    @Override
                    public int hashCode(IpToTimedLockDTO t) {
                        return t.ip.hashCode();
                    }
                });
        this.securedDomainType = securedDomainType;
        this.userService = userService;
        this.errorMessageOnDataFailureString = errorMessageOnDataFailureString;
        this.asWidget().ensureDebugId("wrappedTable");
        this.table.ensureDebugId("cellTable");
        filterField = composeFilterField();
        mainPanel.insert(filterField.asWidget(), 0);
        mainPanel.insert(composeButtonPanel(), 1);
        configureColumns();
        loadDataAndPopulateTable();
    }

    private AccessControlledButtonPanel composeButtonPanel() {
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService, securedDomainType);
        final Button refreshbutton = buttonPanel.addAction(getStringMessages().refresh(), () -> true, new Command() {
            @Override
            public void execute() {
                loadDataAndPopulateTable();
            }
        });
        refreshbutton.ensureDebugId("refreshButton");
        final Button unlockbutton = buttonPanel.addAction(getStringMessages().unlock(), () -> true, new Command() {
            @Override
            public void execute() {
                for (IpToTimedLockDTO e : getSelectionModel().getSelectedSet()) {
                    unlockIP(e.ip, new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(errorMessageOnDataFailureString);
                        }

                        @Override
                        public void onSuccess(Void result) {
                            filterField.remove(e);
                        }
                    });
                }
            }
        });
        unlockbutton.ensureDebugId("unlockButton");
        return buttonPanel;
    }

    private void loadDataAndPopulateTable() {
        final AsyncCallback<HashMap<String, TimedLock>> dataInitializationCallback = new AsyncCallback<HashMap<String, TimedLock>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(errorMessageOnDataFailureString);
            }

            @Override
            public void onSuccess(HashMap<String, TimedLock> result) {
                filterField.clear();
                clear();
                final ArrayList<IpToTimedLockDTO> iterable = new ArrayList<IpToTimedLockDTO>();
                for (Entry<String, TimedLock> e : result.entrySet()) {
                    if (e.getValue().isLocked()) {
                        iterable.add(new IpToTimedLockDTO(e.getKey(), e.getValue()));
                    }
                }
                filterField.addAll(iterable);
            }
        };
        fetchData(dataInitializationCallback);
    }

    private void configureColumns() {
        final ListHandler<IpToTimedLockDTO> columnListHandler = getColumnSortHandler();
        addColumn(record -> record.ip, getStringMessages().ipAddress());
        final Comparator<IpToTimedLockDTO> expiryComparator = (o1, o2) -> {
            return o1.timedLock.getLockedUntil().compareTo(o2.timedLock.getLockedUntil());
        };
        addColumn(record -> record.timedLock.getLockedUntil().toString(), getStringMessages().lockedUntil(),
                expiryComparator);
        table.addColumnSortHandler(columnListHandler);
    }

    private LabeledAbstractFilterablePanel<IpToTimedLockDTO> composeFilterField() {
        final LabeledAbstractFilterablePanel<IpToTimedLockDTO> filterField = new LabeledAbstractFilterablePanel<IpToTimedLockDTO>(
                new Label(getStringMessages().filterIpAddresses()), new ArrayList<>(), getDataProvider(),
                getStringMessages()) {
            @Override
            public Iterable<String> getSearchableStrings(IpToTimedLockDTO dto) {
                List<String> string = new ArrayList<String>();
                string.add(dto.ip);
                return string;
            }

            @Override
            public AbstractCellTable<IpToTimedLockDTO> getCellTable() {
                return table;
            }
        };
        final CheckBox filterCheckbox = new CheckBox(getStringMessages().filterIpAddresses());
        filterCheckbox.addValueChangeHandler(checked -> filterField.filter());
        registerSelectionModelOnNewDataProvider(filterField.getAllListDataProvider());
        return filterField;
    }
}