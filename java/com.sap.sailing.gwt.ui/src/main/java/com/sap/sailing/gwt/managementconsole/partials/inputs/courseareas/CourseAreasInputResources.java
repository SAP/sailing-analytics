package com.sap.sailing.gwt.managementconsole.partials.inputs.courseareas;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface CourseAreasInputResources extends ClientBundle {

    CourseAreasInputResources INSTANCE = GWT.create(CourseAreasInputResources.class);

    @Source({ "CourseAreasInput.gss" })
    Style style();

    public interface Style extends CssResource {
        @ClassName("course-area-row")
        String courseAreaRow();
        @ClassName("geometry-toggle")
        String geometryToggle();
        @ClassName("geometry-box")
        String geometryBox();
        @ClassName("copy-panel")
        String copyPanel();
        @ClassName("copy-row")
        String copyRow();
        @ClassName("credentials-toggle")
        String credentialsToggle();
        @ClassName("button-row")
        String buttonRow();
        @ClassName("picker-content")
        String pickerContent();
        @ClassName("picker-select-all")
        String pickerSelectAll();
        @ClassName("picker-list")
        String pickerList();
        @ClassName("picker-row")
        String pickerRow();
        @ClassName("picker-details")
        String pickerDetails();
        @ClassName("picker-name")
        String pickerName();
        @ClassName("picker-meta")
        String pickerMeta();
        @ClassName("picker-buttons")
        String pickerButtons();
    }
}
