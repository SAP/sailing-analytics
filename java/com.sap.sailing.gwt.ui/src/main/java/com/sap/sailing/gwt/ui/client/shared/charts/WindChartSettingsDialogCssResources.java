package com.sap.sailing.gwt.ui.client.shared.charts;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface WindChartSettingsDialogCssResources extends ClientBundle {
    public static final WindChartSettingsDialogCssResources INSTANCE = GWT.create(WindChartSettingsDialogCssResources.class);

    @Source("com/sap/sailing/gwt/ui/client/shared/charts/WindChartSettingsDialog.css")
    DialogCss css();

    public interface DialogCss extends CssResource {
        String statButton();
        String statButtonActive();
        String statButtonIndeterminate();
        String statButtonBulkActive();
        String statButtonBulkIndeterminate();
        String statButtonSectionDisabled();
        String toAllSelectedLabel();
        String sourceCheckboxIndent();
        String sourceFillerLabel();
        String accentCheckbox();
        String sectionDivider();
    }
}
