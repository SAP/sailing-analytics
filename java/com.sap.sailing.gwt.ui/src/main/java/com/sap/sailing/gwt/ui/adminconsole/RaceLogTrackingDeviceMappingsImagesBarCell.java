package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.components.ImagesBarCell;
import com.sap.sailing.gwt.ui.shared.DeviceMappingDTO;

public class RaceLogTrackingDeviceMappingsImagesBarCell extends ImagesBarCell {
    public static final String ACTION_CLOSE = "CLOSE";
    public static final String ACTION_REMOVE = "REMOVE";
    private static AdminConsoleResources resources = GWT.create(AdminConsoleResources.class);
    private final StringMessages stringMessages;
    
    public RaceLogTrackingDeviceMappingsImagesBarCell(StringMessages stringMessages) {
        super();
        this.stringMessages = stringMessages;
    }

    @Override
    protected Iterable<ImageSpec> getImageSpecs() {
        ArrayList<ImageSpec> result = new ArrayList<ImageSpec>();
        DeviceMappingDTO mapping = (DeviceMappingDTO) getContext().getKey();
        
        result.add(new ImageSpec(ACTION_REMOVE, stringMessages.remove(), makeImagePrototype(resources.removeIcon())));
        
        if (mapping.from == null || mapping.to == null) {
            result.add(new ImageSpec(ACTION_CLOSE, stringMessages.closeTimeRange(),
                    makeImagePrototype(resources.closeTimeRange())));
        }

        return result;
    }
}