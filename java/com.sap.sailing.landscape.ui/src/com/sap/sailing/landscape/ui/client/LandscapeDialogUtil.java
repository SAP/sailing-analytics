package com.sap.sailing.landscape.ui.client;

import java.util.ArrayList;
import java.util.Collections;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ListBox;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class LandscapeDialogUtil {
    public static ListBox createInstanceTypeListBox(DataEntryDialog<?> dialog,
            LandscapeManagementWriteServiceAsync landscapeManagementService, StringMessages stringMessages,
            String defaultInstanceType, ErrorReporter errorReporter) {
        return createInstanceTypeListBoxWithAdditionalDefaultEntry(dialog,
                /* additionalItem */ null, /* additionalValue */ null,
                landscapeManagementService, stringMessages, defaultInstanceType, errorReporter);
    }

    /**
     * @param additionalItem
     *            if not {@code null}, an item with this name is created, and then {@code additionalValue} must not be
     *            {@code null} because it is then used as that item's value. The item will then be set as the one
     *            selected.
     */
    public static ListBox createInstanceTypeListBoxWithAdditionalDefaultEntry(DataEntryDialog<?> dialog,
            String additionalItem, String additionalValue,
            LandscapeManagementWriteServiceAsync landscapeManagementService, StringMessages stringMessages,
            String defaultInstanceType, ErrorReporter errorReporter) {
        final ListBox instanceTypeBox = dialog.createListBox(/*isMultipleSelect*/false);
        if (additionalItem != null) {
            instanceTypeBox.addItem(additionalItem, additionalValue);
            instanceTypeBox.setSelectedIndex(0);
        }
        landscapeManagementService.getInstanceTypes(new AsyncCallback<ArrayList<String>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.getMessage());
            }
            
            @Override
            public void onSuccess(ArrayList<String> result) {
                Collections.sort(result, new NaturalComparator());
                int i=0;
                for (final String instanceType : result) {
                    instanceTypeBox.addItem(instanceType, instanceType);
                    if (additionalItem == null && instanceType.equals(defaultInstanceType)) {
                        instanceTypeBox.setSelectedIndex(i);
                    }
                    i++;
                }
            }
        });
        return instanceTypeBox;
    }
}
