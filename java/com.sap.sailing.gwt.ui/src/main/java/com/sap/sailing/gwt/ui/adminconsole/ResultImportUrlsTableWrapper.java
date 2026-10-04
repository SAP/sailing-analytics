package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.UrlDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.AbstractSortableTextColumn;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.RefreshableSelectionModel;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog.DialogConfig;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;

public class ResultImportUrlsTableWrapper<S extends RefreshableSelectionModel<UrlDTO>> extends TableWrapper<UrlDTO, S> {

    protected static final class UrlDTOEntityIdentityComparator implements EntityIdentityComparator<UrlDTO> {
        @Override
        public boolean representSameEntity(UrlDTO dto1, UrlDTO dto2) {
            return dto1.getUrl().equals(dto2.getUrl());
        }

        @Override
        public int hashCode(UrlDTO t) {
            return t.getUrl().hashCode();
        }
    }

    private String lastUsedProviderName = null;

    public ResultImportUrlsTableWrapper(SailingServiceWriteAsync sailingServiceWrite, UserService userService,
            StringMessages stringMessages, ErrorReporter errorReporter) {
        super(sailingServiceWrite, stringMessages, errorReporter, true, false, new UrlDTOEntityIdentityComparator());
        ListHandler<UrlDTO> listHandler = super.getColumnSortHandler();
        TextColumn<UrlDTO> urlColumn = new AbstractSortableTextColumn<UrlDTO>(url -> url.getUrl(), listHandler);
        urlColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        AccessControlledActionsColumn<UrlDTO, DefaultActionsImagesBarCell> actionsColumn = AccessControlledActionsColumn
                .create(new DefaultActionsImagesBarCell(stringMessages), userService);
        DialogConfig<UrlDTO> config = EditOwnershipDialog.create(userService.getUserManagementWriteService(),
                SecuredDomainType.RESULT_IMPORT_URL, event -> update(), stringMessages);
        actionsColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP, DefaultActions.CHANGE_OWNERSHIP,
                config::openOwnershipDialog);
        EditACLDialog.DialogConfig<UrlDTO> configACL = EditACLDialog.create(userService.getUserManagementWriteService(),
                SecuredDomainType.RESULT_IMPORT_URL, event -> update(), stringMessages);
        actionsColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                e -> configACL.openDialog(e));
        actionsColumn.addAction(DefaultActions.DELETE.name(), DefaultActions.DELETE, url -> {
            if (Window.confirm(stringMessages
                    .doYouReallyWantToRemoveSelectedElements(url.getName()))) {
                removeUrl(url); 
            }
        });
        super.table.addColumn(urlColumn, stringMessages.url());
        SecuredDTOOwnerColumn.configureOwnerColumns(super.table, listHandler, stringMessages);
        super.table.addColumn(actionsColumn, stringMessages.actions());
    }

    private void update() {
        update(lastUsedProviderName);
    }

    public void update(String providerName) {
        lastUsedProviderName = providerName;
        if (providerName != null) {
            sailingServiceWrite.getResultImportUrls(providerName,
                    new MarkedAsyncCallback<List<UrlDTO>>(new AsyncCallback<List<UrlDTO>>() {
                @Override
                public void onSuccess(List<UrlDTO> result) {
                    ResultImportUrlsTableWrapper.super.refresh(result);
                }

                @Override
                public void onFailure(Throwable caught) {
                    ResultImportUrlsTableWrapper.super.refresh(Collections.emptyList());
                    ResultImportUrlsTableWrapper.super.errorReporter
                            .reportError(ResultImportUrlsTableWrapper.super.getStringMessages()
                                    .errorRefreshingResultImportUrlList(caught.getMessage()));
                }
            }));
        } else {
            ResultImportUrlsTableWrapper.super.refresh(Collections.emptyList());
        }
    }

    private void removeUrl(UrlDTO url) {
        final Set<UrlDTO> urls = new HashSet<>();
        urls.add(url);
        if (lastUsedProviderName != null) {
            sailingServiceWrite.removeResultImportURLs(lastUsedProviderName, urls,
                    new AsyncCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            update(lastUsedProviderName);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            update(lastUsedProviderName);
                            ResultImportUrlsTableWrapper.super.errorReporter
                                    .reportError(ResultImportUrlsTableWrapper.super.getStringMessages()
                                            .errorRemovingResultImportUrls(caught.getMessage()));
                        }
                    });
        }
    }
}
