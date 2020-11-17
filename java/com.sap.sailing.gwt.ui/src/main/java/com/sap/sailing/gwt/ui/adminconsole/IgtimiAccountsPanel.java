package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_DELETE;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DialogBox.CaptionImpl;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.AccountWithSecurityDTO;
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
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
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

public class IgtimiAccountsPanel extends FlowPanel implements FilterablePanelProvider<AccountWithSecurityDTO> {

    private final StringMessages stringMessages;
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final ErrorReporter errorReporter;
    private final LabeledAbstractFilterablePanel<AccountWithSecurityDTO> filterAccountsPanel;
    private final RefreshableMultiSelectionModel<AccountWithSecurityDTO> refreshableAccountsSelectionModel;

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
    public IgtimiAccountsPanel(final SailingServiceWriteAsync sailingServiceWrite, final ErrorReporter errorReporter,
            final StringMessages stringMessages, final UserService userService) {
        this.sailingServiceWrite = sailingServiceWrite;
        this.errorReporter = errorReporter;
        this.stringMessages = stringMessages;

        AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);

        // setup table
        final FlushableCellTable<AccountWithSecurityDTO> cellTable = new FlushableCellTable<>(/* pageSize */ 50,
                tableRes);
        final ListDataProvider<AccountWithSecurityDTO> filteredAccounts = new ListDataProvider<>();
        filterAccountsPanel = new LabeledAbstractFilterablePanel<AccountWithSecurityDTO>(
                new Label(stringMessages.igtimiAccounts()), Collections.emptyList(), filteredAccounts, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(AccountWithSecurityDTO t) {
                Set<String> strings = Collections.singleton(t.getEmail());
                return strings;
            }

            @Override
            public AbstractCellTable<AccountWithSecurityDTO> getCellTable() {
                return null;
            }
        };
        createIgtimiAccountsTable(cellTable, tableRes, userService, filteredAccounts, filterAccountsPanel);
        // refreshableAccountsSelectionModel will be of correct type, see below in createIgtimiAccountsTable
        refreshableAccountsSelectionModel = (RefreshableMultiSelectionModel<AccountWithSecurityDTO>) cellTable
                .getSelectionModel();

        final Panel controlsPanel = new HorizontalPanel();
        filterAccountsPanel
                .setUpdatePermissionFilterForCheckbox(account -> userService.hasPermission(account, DefaultActions.UPDATE));
        controlsPanel.add(filterAccountsPanel);

        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService,
                SecuredDomainType.IGTIMI_ACCOUNT);
        controlsPanel.add(buttonPanel);
        buttonPanel.addUnsecuredAction(stringMessages.refresh(), () -> refresh());

        // setup controls
        final Button removeAccountButton = buttonPanel.addRemoveAction(stringMessages.remove(), () -> {
            if (refreshableAccountsSelectionModel.getSelectedSet().size() > 0) {
                if (Window.confirm(stringMessages.doYouReallyWantToRemoveTheSelectedIgtimiAccounts())) {
                    for (AccountWithSecurityDTO account : refreshableAccountsSelectionModel.getSelectedSet()) {
                        removeAccount(account, filteredAccounts);
                    }
                }
            }
        });
        removeAccountButton.setEnabled(false);
        refreshableAccountsSelectionModel.addSelectionChangeHandler(
                e -> removeAccountButton.setEnabled(refreshableAccountsSelectionModel.getSelectedSet().size() > 0));
        add(controlsPanel);
        add(cellTable);

        // add button
        final Button addAccountButton = buttonPanel.addCreateAction(stringMessages.addIgtimiAccount(),
                () -> addAccount());
        addAccountButton.ensureDebugId("addIgtimiAccount");

        final String protocol = Window.Location.getProtocol().endsWith(":")
                ? Window.Location.getProtocol().substring(0, Window.Location.getProtocol().length() - 1)
                : Window.Location.getProtocol();
        final String hostname = Window.Location.getHostName();
        final String port = Window.Location.getPort();
        this.sailingServiceWrite.getIgtimiAuthorizationUrl(protocol, hostname, port, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(
                        IgtimiAccountsPanel.this.stringMessages.errorGettingIgtimiAuthorizationUrl(caught.getMessage()),
                        /* silentMode */ true);
            }

            @Override
            public void onSuccess(String result) {
                buttonPanel.addCreateAction(stringMessages.addIgtimiUser() + " (OAuth)", () -> {
                    Frame frame = new Frame(UriUtils.fromString(result).asString());
                    frame.addLoadHandler(loadEvent -> refresh());
                    frame.setPixelSize(520, 770);
                    final CaptionImpl caption = new CaptionImpl();
                    caption.setText(stringMessages.addIgtimiUser());
                    final DialogBox dialogBox = new DialogBox(/* autoHide */ false, /* modal */ true);
                    Button closeButton = new Button(stringMessages.close());
                    closeButton.addClickHandler(new ClickHandler() {
                        public void onClick(ClickEvent event) {
                            dialogBox.hide();
                        }
                    });
                    dialogBox.setText(stringMessages.addIgtimiUser());
                    dialogBox.setGlassEnabled(true);
                    final VerticalPanel panel = new VerticalPanel();
                    dialogBox.setWidget(panel);
                    panel.add(frame);
                    panel.add(closeButton);
                    dialogBox.addCloseHandler(event -> {
                        refresh();
                    });
                    dialogBox.center();
                        });
            }
        });
    }

    private FlushableCellTable<AccountWithSecurityDTO> createIgtimiAccountsTable(
            final FlushableCellTable<AccountWithSecurityDTO> table, final CellTableWithCheckboxResources tableResources,
            final UserService userService, final ListDataProvider<AccountWithSecurityDTO> filteredAccounts,
            final LabeledAbstractFilterablePanel<AccountWithSecurityDTO> filterAccountsPanel) {
        filteredAccounts.addDataDisplay(table);
        final SelectionCheckboxColumn<AccountWithSecurityDTO> accountSelectionCheckboxColumn = new SelectionCheckboxColumn<AccountWithSecurityDTO>(
                tableResources.cellTableStyle().cellTableCheckboxSelected(),
                tableResources.cellTableStyle().cellTableCheckboxDeselected(),
                tableResources.cellTableStyle().cellTableCheckboxColumnCell(),
                new EntityIdentityComparator<AccountWithSecurityDTO>() {
                    @Override
                    public boolean representSameEntity(AccountWithSecurityDTO a1, AccountWithSecurityDTO a2) {
                        return a1.getEmail().equals(a2.getEmail());
                    }

                    @Override
                    public int hashCode(AccountWithSecurityDTO t) {
                        return t.getEmail().hashCode();
                    }
                }, filterAccountsPanel.getAllListDataProvider(), table);
        final ListHandler<AccountWithSecurityDTO> columnSortHandler = new ListHandler<>(filteredAccounts.getList());
        table.addColumnSortHandler(columnSortHandler);
        columnSortHandler.setComparator(accountSelectionCheckboxColumn, accountSelectionCheckboxColumn.getComparator());
        final TextColumn<AccountWithSecurityDTO> accountNameColumn = new AbstractSortableTextColumn<AccountWithSecurityDTO>(
                account -> account.getName(), columnSortHandler);
        final TextColumn<AccountWithSecurityDTO> accountEmailColumn = new AbstractSortableTextColumn<AccountWithSecurityDTO>(
                account -> account.getEmail(), columnSortHandler);
        final TextColumn<AccountWithSecurityDTO> creatorNameColumn = new AbstractSortableTextColumn<AccountWithSecurityDTO>(
                account -> account.getCreatorName(), columnSortHandler);

        final HasPermissions type = SecuredDomainType.IGTIMI_ACCOUNT;
        final AccessControlledActionsColumn<AccountWithSecurityDTO, DefaultActionsImagesBarCell> roleActionColumn = create(
                new DefaultActionsImagesBarCell(stringMessages), userService);
        roleActionColumn.addAction(ACTION_DELETE, DELETE, account -> {
            if (Window.confirm(stringMessages.doYouReallyWantToRemoveIgtimiAccount(account.getEmail()))) {
                removeAccount(account, filteredAccounts);
            }
        });
        final DialogConfig<AccountWithSecurityDTO> config = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), type, roleDefinition -> refresh(), stringMessages);
        roleActionColumn.addAction(ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP, config::openOwnershipDialog);
        final EditACLDialog.DialogConfig<AccountWithSecurityDTO> configACL = EditACLDialog
                .create(userService.getUserManagementWriteService(), type, roleDefinition -> refresh(), stringMessages);
        roleActionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                configACL::openDialog);

        table.addColumn(accountSelectionCheckboxColumn, accountSelectionCheckboxColumn.getHeader());
        table.addColumn(accountNameColumn, stringMessages.name());
        table.addColumn(accountEmailColumn, stringMessages.email());
        table.addColumn(creatorNameColumn, stringMessages.creatorName());
        SecuredDTOOwnerColumn.configureOwnerColumns(table, columnSortHandler, stringMessages);
        table.addColumn(roleActionColumn, stringMessages.actions());
        table.setSelectionModel(accountSelectionCheckboxColumn.getSelectionModel(),
                accountSelectionCheckboxColumn.getSelectionManager());
        return table;
    }

    public void refresh() {
        sailingServiceWrite.getAllIgtimiAccountsWithSecurity(new AsyncCallback<Iterable<AccountWithSecurityDTO>>() {
            @Override
            public void onSuccess(Iterable<AccountWithSecurityDTO> result) {
                filterAccountsPanel.updateAll(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorFetchingIgtimiAccounts(caught.getMessage()));
            }
        });
    }

    private static class UserData {
        private final String eMail;
        private final String password;

        protected UserData(String eMail, String password) {
            super();
            this.eMail = eMail;
            this.password = password;
        }

        protected String geteMail() {
            return eMail;
        }

        protected String getPassword() {
            return password;
        }
    }

    private class AddAccountDialog extends DataEntryDialog<UserData> {
        private TextBox eMail;
        private PasswordTextBox password;

        public AddAccountDialog(final Runnable refresher, final SailingServiceWriteAsync sailingServiceWrite,
                final StringMessages stringMessages, final ErrorReporter errorReporter) {
            super(stringMessages.addIgtimiUser(), stringMessages.addIgtimiUser(), stringMessages.ok(),
                    stringMessages.cancel(), new Validator<UserData>() {
                        @Override
                        public String getErrorMessage(UserData valueToValidate) {
                            final String errorMessage;
                            if (valueToValidate.geteMail() == null || valueToValidate.geteMail().isEmpty()) {
                                errorMessage = stringMessages.eMailMustNotBeEmpty();
                            } else {
                                errorMessage = null;
                            }
                            return errorMessage;
                        }
                    }, /* animationEnabled */ true, new DialogCallback<UserData>() {
                        @Override
                        public void ok(final UserData editedObject) {
                            sailingServiceWrite.authorizeAccessToIgtimiUser(editedObject.geteMail(),
                                    editedObject.getPassword(), new AsyncCallback<Boolean>() {
                                        @Override
                                        public void onFailure(Throwable caught) {
                                            errorReporter.reportError(stringMessages.errorAuthorizingAccessToIgtimiUser(
                                                    editedObject.geteMail(), caught.getMessage()));
                                        }

                                        @Override
                                        public void onSuccess(Boolean result) {
                                            if (result) {
                                                Notification
                                                        .notify(stringMessages.successfullyAuthorizedAccessToIgtimiUser(
                                                                editedObject.geteMail()), NotificationType.SUCCESS);
                                                refresher.run();
                                            } else {
                                                Notification.notify(stringMessages.couldNotAuthorizedAccessToIgtimiUser(
                                                        editedObject.geteMail()), NotificationType.ERROR);
                                            }
                                        }
                                    });
                        }

                        @Override
                        public void cancel() {
                        }

                    });
            ensureDebugId("AddIgtimiAccountDialog");
        }

        @Override
        protected Widget getAdditionalWidget() {
            Grid grid = new Grid(2, 2);
            grid.setWidget(0, 0, new Label(stringMessages.emailAddress()));
            eMail = createTextBox("");
            eMail.ensureDebugId("igtimiAccountEmail");
            grid.setWidget(0, 1, eMail);
            grid.setWidget(1, 0, new Label(stringMessages.password()));
            password = createPasswordTextBox("");
            password.ensureDebugId("igtimiAccountPassword");
            grid.setWidget(1, 1, password);
            return grid;
        }

        @Override
        protected FocusWidget getInitialFocusWidget() {
            return eMail;
        }

        @Override
        protected UserData getResult() {
            return new UserData(eMail.getText(), password.getText());
        }
    }

    private void addAccount() {
        new AddAccountDialog(this::refresh, sailingServiceWrite, stringMessages, errorReporter).show();
    }

    private void removeAccount(final AccountWithSecurityDTO account,
            final ListDataProvider<AccountWithSecurityDTO> removeFrom) {
        sailingServiceWrite.removeIgtimiAccount(account.getEmail(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorTryingToRemoveIgtimiAccount(account.getEmail()));
            }

            @Override
            public void onSuccess(Void result) {
                Notification.notify(stringMessages.successfullyRemoveIgtimiAccount(account.getEmail()),
                        NotificationType.INFO);
                removeFrom.getList().remove(account);
            }
        });
    }
    
    @Override
    public AbstractFilterablePanel<AccountWithSecurityDTO> getFilterablePanel() {
        return filterAccountsPanel;
    }
}
