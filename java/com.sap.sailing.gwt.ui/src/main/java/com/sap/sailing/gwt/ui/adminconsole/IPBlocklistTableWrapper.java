package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.RefreshableSelectionModel;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.SelectedElementsCountingButton;
import com.sap.sse.security.ui.shared.IpToTimedLockDTO;

abstract class IPBlocklistTableWrapper
        extends TableWrapper<IpToTimedLockDTO, RefreshableSelectionModel<IpToTimedLockDTO>> {
    private final UserService userService;
    private final LabeledAbstractFilterablePanel<IpToTimedLockDTO> filterField;
    private final String errorMessageOnDataFailureString;

    protected abstract void fetchData(AsyncCallback<ArrayList<IpToTimedLockDTO>> callback);

    protected abstract void unlockIP(String ip, AsyncCallback<Void> asyncCallback);

    public IPBlocklistTableWrapper(final SailingServiceWriteAsync sailingServiceWrite, final UserService userService,
            final String errorMessageOnDataFailureString, final StringMessages stringMessages,
            final ErrorReporter errorReporter) {
        super(sailingServiceWrite, stringMessages, errorReporter, true, true,
                new EntityIdentityComparator<IpToTimedLockDTO>() {
                    @Override
                    public boolean representSameEntity(IpToTimedLockDTO dto1, IpToTimedLockDTO dto2) {
                        return dto1.getIp().equals(dto2.getIp());
                    }

                    @Override
                    public int hashCode(IpToTimedLockDTO t) {
                        return t.getIp().hashCode();
                    }
                });
        this.userService = userService;
        this.errorMessageOnDataFailureString = errorMessageOnDataFailureString;
        this.filterField = composeFilterField();
        this.asWidget().ensureDebugId("wrappedTable");
        this.table.ensureDebugId("cellTable");
        configureDataColumns();
        setButtonsAndFilterOnMainPanel();
        loadDataAndPopulateTable();
    }

    private void setButtonsAndFilterOnMainPanel() {
        final HorizontalPanel searchPanel = new HorizontalPanel();
        searchPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        searchPanel.setSpacing(5);
        final Label label = new Label(getStringMessages().filterIpAddresses() + ": ");
        searchPanel.add(label);
        searchPanel.add(filterField.getTextBox());
        // inserted with indices to put them above the table
        mainPanel.insert(searchPanel, 0);
        mainPanel.insert(composeButtonPanel(), 1);
        mainPanel.setSpacing(5);
    }

    private Widget composeButtonPanel() {
        final HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setSpacing(5);
        final Button refreshButton = new Button(getStringMessages().refresh(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                loadDataAndPopulateTable();
            }
        });
        refreshButton.ensureDebugId("refreshButton");
        buttonPanel.add(refreshButton);
        if (hasUnlockPermission()) {
            final Button unlockButton = composeUnlockButton();
            unlockButton.ensureDebugId("unlockButton");
            buttonPanel.add(unlockButton);
        }
        return buttonPanel;
    }

    private boolean hasUnlockPermission() {
        final WildcardPermission unlockIpPermission = SecuredSecurityTypes.LOCKED_IP
                .getPermission(DefaultActions.DELETE);
        return userService.hasPermission(unlockIpPermission, userService.getServerInfo().getOwnership());
    }

    private SelectedElementsCountingButton<IpToTimedLockDTO> composeUnlockButton() {
        return new SelectedElementsCountingButton<IpToTimedLockDTO>(getStringMessages().unlock(), getSelectionModel(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        for (IpToTimedLockDTO e : getSelectionModel().getSelectedSet()) {
                            unlockIP(e.getIp(), new AsyncCallback<Void>() {
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
    }

    private void loadDataAndPopulateTable() {
        final AsyncCallback<ArrayList<IpToTimedLockDTO>> dataInitializationCallback = new AsyncCallback<ArrayList<IpToTimedLockDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(errorMessageOnDataFailureString);
            }

            @Override
            public void onSuccess(ArrayList<IpToTimedLockDTO> result) {
                filterField.clear();
                clear();
                filterField.addAll(result);
            }
        };
        fetchData(dataInitializationCallback);
    }

    private void configureDataColumns() {
        final ListHandler<IpToTimedLockDTO> columnListHandler = getColumnSortHandler();
        addColumn(record -> record.getIp(), getStringMessages().ipAddress());
        final Comparator<IpToTimedLockDTO> expiryComparator = (o1, o2) -> {
            return o1.getTimedLock().getLockedUntil().compareTo(o2.getTimedLock().getLockedUntil());
        };
        addColumn(record -> record.getTimedLock().getLockedUntil().toString(), getStringMessages().lockedUntil(),
                expiryComparator);
        table.addColumnSortHandler(columnListHandler);
    }

    private LabeledAbstractFilterablePanel<IpToTimedLockDTO> composeFilterField() {
        final LabeledAbstractFilterablePanel<IpToTimedLockDTO> filterField = new LabeledAbstractFilterablePanel<IpToTimedLockDTO>(
                new Label(getStringMessages().filterIpAddresses()), new ArrayList<>(), getDataProvider(),
                getStringMessages()) {
            @Override
            public Iterable<String> getSearchableStrings(IpToTimedLockDTO dto) {
                final List<String> string = new ArrayList<String>();
                string.add(dto.getIp());
                return string;
            }

            @Override
            public AbstractCellTable<IpToTimedLockDTO> getCellTable() {
                return table;
            }
        };
        registerSelectionModelOnNewDataProvider(filterField.getAllListDataProvider());
        return filterField;
    }
}