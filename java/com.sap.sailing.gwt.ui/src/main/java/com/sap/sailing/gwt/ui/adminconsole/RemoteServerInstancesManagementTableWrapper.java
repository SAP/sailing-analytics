package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sap.sailing.gwt.ui.shared.EventBaseDTO;
import com.sap.sailing.gwt.ui.shared.RemoteSailingServerReferenceDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.TableWrapperWithMultiSelectionAndFilter;
import com.sap.sse.security.ui.client.i18n.StringMessages;

public class RemoteServerInstancesManagementTableWrapper extends
        TableWrapperWithMultiSelectionAndFilter<RemoteSailingServerReferenceDTO, StringMessages, CellTableWithCheckboxResources> {
    public RemoteServerInstancesManagementTableWrapper(StringMessages stringMessages, ErrorReporter errorReporter,
            CellTableWithCheckboxResources tableResources) {
        super(stringMessages, errorReporter, /* enablePager */ true,
                Optional.of(new EntityIdentityComparator<RemoteSailingServerReferenceDTO>() {
                    @Override
                    public boolean representSameEntity(RemoteSailingServerReferenceDTO dto1,
                            RemoteSailingServerReferenceDTO dto2) {
                        return dto1.getUrl().equals(dto2.getUrl());
                    }

                    @Override
                    public int hashCode(RemoteSailingServerReferenceDTO t) {
                        return t.getUrl().hashCode();
                    }
                }), tableResources, /* updatePermissionFilterForCheckbox */ Optional.empty(),
                /* use default filter-by text */ Optional.empty(),
                /* filterCheckboxLabel */ null);
    }

    @Override
    public List<String> getSearchableStrings(RemoteSailingServerReferenceDTO t) {
        List<String> strings = new ArrayList<String>();
        strings.add(t.getName());
        strings.add(t.getUrl());
        if (t.getEvents() != null) {
            for (EventBaseDTO e : t.getEvents()) {
                strings.add(e.getName());
            }
        }
        return strings;
    }
}
