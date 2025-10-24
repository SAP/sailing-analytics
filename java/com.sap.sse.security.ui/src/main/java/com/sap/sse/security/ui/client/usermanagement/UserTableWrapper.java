package com.sap.sse.security.ui.client.usermanagement;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.UPDATE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_DELETE;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_UPDATE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.Callback;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.AbstractSortableTextColumn;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.RefreshableSelectionModel;
import com.sap.sse.gwt.client.celltable.TableWrapper;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.dto.RoleWithSecurityDTO;
import com.sap.sse.security.shared.dto.StrippedUserGroupDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.ui.client.EntryPointLinkFactory;
import com.sap.sse.security.ui.client.UserManagementServiceAsync;
import com.sap.sse.security.ui.client.UserManagementWriteServiceAsync;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;
import com.sap.sse.security.ui.client.i18n.StringMessages;
import com.sap.sse.security.ui.shared.SuccessInfo;

/**
 * A filterable user table. The data model is managed by the {@link #getFilterField() filter field}. In order to set an
 * initial set of users to display by this table, use {@link #refreshUserList(Iterable)}. The selected users can be
 * obtained from the {@link #getSelectionModel() selection model}. The users currently in the table (regardless of the
 * current filter settings) are returned by {@link #getAllUsers()}.
 * 
 * @author Axel Uhl (D043530)
 *
 * @param <S>
 */
public class UserTableWrapper<S extends RefreshableSelectionModel<UserDTO>, TR extends CellTableWithCheckboxResources>
extends TableWrapper<UserDTO, S, StringMessages, TR> {
    private final LabeledAbstractFilterablePanel<UserDTO> filterField;
    private final UserService userService;
    
    public UserTableWrapper(UserService userService,
            StringMessages stringMessages, ErrorReporter errorReporter,
            boolean multiSelection, boolean enablePager, TR tableResources) {
        super(stringMessages, errorReporter, multiSelection, enablePager,
                new EntityIdentityComparator<UserDTO>() {
                    @Override
                    public boolean representSameEntity(UserDTO dto1, UserDTO dto2) {
                        return dto1.getId().toString().equals(dto2.getId().toString());
                    }
                    @Override
                    public int hashCode(UserDTO t) {
                        return t.getId().hashCode();
                    }
                }, tableResources);
        this.userService = userService;
        ListHandler<UserDTO> userColumnListHandler = getColumnSortHandler();
        // users table
        TextColumn<UserDTO> usernameColumn = new AbstractSortableTextColumn<UserDTO>(user->user.getName(), userColumnListHandler);
        TextColumn<UserDTO> fullNameColumn = new AbstractSortableTextColumn<UserDTO>(user->user.getFullName(), userColumnListHandler);
        TextColumn<UserDTO> emailColumn = new AbstractSortableTextColumn<UserDTO>(user->user.getEmail(), userColumnListHandler);
        TextColumn<UserDTO> emailValidatedColumn = new AbstractSortableTextColumn<UserDTO>(user->user.isEmailValidated() ? stringMessages.yes() : stringMessages.no(), userColumnListHandler);
        TextColumn<UserDTO> companyColumn = new AbstractSortableTextColumn<UserDTO>(user->user.getCompany(), userColumnListHandler);
        Column<UserDTO, SafeHtml> groupsColumn = new Column<UserDTO, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(UserDTO user) {
                SafeHtmlBuilder builder = new SafeHtmlBuilder();
                for (Iterator<StrippedUserGroupDTO> groupsIter = user.getUserGroups()
                        .iterator(); groupsIter.hasNext();) {
                    final StrippedUserGroupDTO group = groupsIter.next();
                    builder.appendEscaped(group.getName());
                    if (groupsIter.hasNext()) {
                        builder.appendHtmlConstant("<br>");
                    }
                }
                return builder.toSafeHtml();
            }
        };
        groupsColumn.setSortable(true);
        userColumnListHandler.setComparator(groupsColumn, new Comparator<UserDTO>() {
            @Override
            public int compare(UserDTO r1, UserDTO r2) {
                return new NaturalComparator().compare(r1.getUserGroups().toString(), r2.getUserGroups().toString());
            }
        });
        Column<UserDTO, SafeHtml> permissionsColumn = new Column<UserDTO, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(UserDTO user) {
                SafeHtmlBuilder builder = new SafeHtmlBuilder();
                for (Iterator<WildcardPermission> permissionIter=user.getPermissions().iterator(); permissionIter.hasNext(); ) {
                    final WildcardPermission permission = permissionIter.next();
                    builder.appendEscaped(permission.toString());
                    if (permissionIter.hasNext()) {
                        builder.appendHtmlConstant("<br>");
                    }
                }
                return builder.toSafeHtml();
            }
        };
        permissionsColumn.setSortable(true);
        userColumnListHandler.setComparator(permissionsColumn, new Comparator<UserDTO>() {
            @Override
            public int compare(UserDTO r1, UserDTO r2) {
                return new NaturalComparator().compare(r1.getPermissions().toString(), r2.getPermissions().toString());
            }
        });
        Column<UserDTO, SafeHtml> rolesColumn = new Column<UserDTO, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(UserDTO user) {
                SafeHtmlBuilder builder = new SafeHtmlBuilder();
                for (Iterator<RoleWithSecurityDTO> roleIter = user.getRoles().iterator(); roleIter.hasNext();) {
                    final RoleWithSecurityDTO role = roleIter.next();
                    builder.appendEscaped(role.toString());
                    if (roleIter.hasNext()) {
                        builder.appendHtmlConstant("<br>");
                    }
                }
                return builder.toSafeHtml();
            }
        };
        rolesColumn.setSortable(true);
        userColumnListHandler.setComparator(rolesColumn, new Comparator<UserDTO>() {
            @Override
            public int compare(UserDTO r1, UserDTO r2) {
                return new NaturalComparator().compare(r1.getRoles().toString(), r2.getRoles().toString());
            }
        });
        final TextColumn<UserDTO> lockedUntilColumn = new AbstractSortableTextColumn<UserDTO>(
                user->user.getLockedUntil() != null && user.getLockedUntil().after(TimePoint.now()) ?
                        DateAndTimeFormatterUtil.dateTimeMedium.render(user.getLockedUntil().asDate()) : "",
                userColumnListHandler);
        final HasPermissions type = SecuredSecurityTypes.USER;
        final AccessControlledActionsColumn<UserDTO, DefaultActionsImagesBarCell> userActionColumn = create(
                new DefaultActionsImagesBarCell(stringMessages), userService);
        userActionColumn.addAction(ACTION_UPDATE, UPDATE, user -> editUser(user));
        userActionColumn.addAction(ACTION_DELETE, DELETE, user -> {
            if (Window.confirm(stringMessages.doYouReallyWantToRemoveUser(user.getName()))) {
                getUserManagementWriteService().deleteUser(user.getName(), new AsyncCallback<SuccessInfo>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        deletingUserFailed(user, caught.getMessage());
                    }

                    @Override
                    public void onSuccess(SuccessInfo result) {
                        if (result.isSuccessful()) {
                            filterField.remove(user);
                        } else {
                            deletingUserFailed(user, result.getMessage());
                        }
                    }
                    
                    private void deletingUserFailed(UserDTO user, String message) {
                        errorReporter.reportError(stringMessages.errorDeletingUser(user.getName(), message));
                    }
                });
            }
        });
        final EditOwnershipDialog.DialogConfig<UserDTO> configOwnership = EditOwnershipDialog.create(
                userService.getUserManagementWriteService(), type,
                user -> refreshUserList((Callback<Iterable<UserDTO>, Throwable>) null), stringMessages);
        final EditACLDialog.DialogConfig<UserDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type,
                user -> user.getAccessControlList(), stringMessages);
        userActionColumn.addAction(ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP, configOwnership::openOwnershipDialog);
        userActionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                u -> configACL.openDialog(u));
        filterField = new LabeledAbstractFilterablePanel<UserDTO>(new Label(stringMessages.filterUsers()),
                new ArrayList<UserDTO>(), dataProvider, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(UserDTO t) {
                List<String> strings = new ArrayList<>();
                strings.add(t.getName());
                strings.add(t.getFullName());
                strings.add(t.getEmail());
                strings.add(t.getCompany());
                Util.addAll(Util.map(t.getRoles(), RoleWithSecurityDTO::getName), strings);
                Util.addAll(Util.map(t.getUserGroups(), StrippedUserGroupDTO::getName), strings);
                return strings;
            }

            @Override
            public AbstractCellTable<UserDTO> getCellTable() {
                return table;
            }
        };
        registerSelectionModelOnNewDataProvider(filterField.getAllListDataProvider());
        filterField.setUpdatePermissionFilterForCheckbox(user -> userService.hasPermission(user, DefaultActions.UPDATE));
        mainPanel.insert(filterField, 0);
        table.addColumnSortHandler(userColumnListHandler);
        table.addColumn(usernameColumn, getStringMessages().username());
        table.addColumn(fullNameColumn, stringMessages.name());
        table.addColumn(emailColumn, stringMessages.email());
        table.addColumn(emailValidatedColumn, stringMessages.validated());
        table.addColumn(companyColumn, stringMessages.company());
        table.addColumn(groupsColumn, stringMessages.groups());
        table.addColumn(rolesColumn, stringMessages.roles());
        table.addColumn(permissionsColumn, stringMessages.permissions());
        table.addColumn(lockedUntilColumn, stringMessages.lockedUntil());
        SecuredDTOOwnerColumn.configureOwnerColumns(table, userColumnListHandler, stringMessages);
        table.addColumn(userActionColumn, stringMessages.actions());
        table.ensureDebugId("UsersTable");
    }
    
    public Iterable<UserDTO> getAllUsers() {
        return filterField.getAll();
    }
    
    public LabeledAbstractFilterablePanel<UserDTO> getFilterField() {
        return filterField;
    }
    
    public void refreshUserList(Iterable<UserDTO> competitors) {
        getFilteredUsers(competitors);
    }
    
    /**
     * @param callback optional; may be {@code null}
     */
    public void refreshUserList(final Callback<Iterable<UserDTO>, Throwable> callback, boolean clearSelection) {
        if (clearSelection) {
            getSelectionModel().clear();
        }
        final AsyncCallback<Collection<UserDTO>> myCallback = new AsyncCallback<Collection<UserDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Remote Procedure Call getUserList() - Failure: " + caught.getMessage());
                if (callback != null) {
                    callback.onFailure(caught);
                }
            }

            @Override
            public void onSuccess(Collection<UserDTO> result) {
                getFilteredUsers(result);
                refreshUserList(result);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }
        };
        getUserManagementService().getUserList(myCallback);
    }

    public void refreshUserList(final Callback<Iterable<UserDTO>, Throwable> callback) {
        refreshUserList(callback, true);
    }

    private void getFilteredUsers(Iterable<UserDTO> result) {
        filterField.updateAll(result);
    }
    
    private void editUser(final UserDTO originalUser) {
        final UserEditDialog dialog = new UserEditDialog(originalUser, new DialogCallback<UserDTO>() {
            @Override
            public void ok(final UserDTO user) {
                getUserManagementWriteService().updateUserProperties(user.getName(), user.getFullName(), user.getCompany(),
                        user.getLocale(),
                        user.getDefaultTenant() != null ? user.getDefaultTenant().getId().toString() : null,
                        new AsyncCallback<UserDTO>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError(getStringMessages().errorTryingToUpdateUser(user.getName(),
                                        caught.getMessage()));
                            }

                            @Override
                            public void onSuccess(final UserDTO updatedUser) {
                                int editedUserIndex = getFilterField().indexOf(originalUser);
                                getFilterField().remove(originalUser);
                                if (editedUserIndex >= 0) {
                                    getFilterField().add(editedUserIndex, updatedUser);
                                } else {
                                    // in case competitor was not present --> not edit, but create
                                    getFilterField().add(updatedUser);
                                }
                                getFilterField().remove(updatedUser);
                                getFilterField().add(updatedUser);
                                if (userService.getCurrentUser().getName().equals(updatedUser.getName())) {
                                    // if the current user's roles changed, update the user object in the user service
                                    // and notify others
                                    userService.updateUser(/* notify other instances */ true);
                                }
                            }
                        });
                if (!originalUser.getEmail().equals(user.getEmail())) {
                    getUserManagementWriteService().updateSimpleUserEmail(user.getName(), user.getEmail(),
                            EntryPointLinkFactory.createEmailValidationLink(new HashMap<String, String>()),
                            new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    errorReporter.reportError(
                                            getStringMessages().errorChangingPassword(caught.getMessage()));
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    userService.updateUser(true);
                                }
                            });
                }
            }

            @Override
            public void cancel() {
            }
        }, userService, errorReporter);
        dialog.ensureDebugId("UserEditDialog");
        dialog.show();
    }

    private UserManagementServiceAsync getUserManagementService() {
        return userService.getUserManagementService();
    }
    
    private UserManagementWriteServiceAsync getUserManagementWriteService() {
        return userService.getUserManagementWriteService();
    }
}
