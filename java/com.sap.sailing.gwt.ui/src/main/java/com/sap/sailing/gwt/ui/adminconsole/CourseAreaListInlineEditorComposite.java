package com.sap.sailing.gwt.ui.adminconsole;

import java.util.UUID;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MeterDistance;
import com.sap.sse.gwt.client.controls.listedit.ExpandedListEditorUi;
import com.sap.sse.gwt.client.controls.listedit.GenericStringListEditorComposite;
import com.sap.sse.gwt.client.controls.listedit.GenericStringListInlineEditorComposite;
import com.sap.sse.gwt.client.controls.listedit.ListEditorComposite;
import com.sap.sse.gwt.client.controls.listedit.ListEditorUiStrategy;
import com.sap.sse.gwt.client.dialog.DoubleBox;

public class CourseAreaListInlineEditorComposite extends GenericStringListInlineEditorComposite<CourseAreaDTO> {
    public CourseAreaListInlineEditorComposite(Iterable<CourseAreaDTO> initialValues, ListEditorUiStrategy<CourseAreaDTO> activeUi) {
        super(initialValues, activeUi);
    }

    public CourseAreaListInlineEditorComposite(Iterable<CourseAreaDTO> initialValues, com.sap.sailing.gwt.ui.client.StringMessages stringMessages,
            ImageResource removeImage, Iterable<String> suggestValues, int textBoxSize) {
        super(initialValues, stringMessages, removeImage, suggestValues, textBoxSize);
    }

    public static class CollapsedUi extends GenericStringListEditorComposite.CollapsedUi<CourseAreaDTO> {
        public CollapsedUi(com.sap.sailing.gwt.ui.client.StringMessages stringMessages, String dialogTitle, ExpandedListEditorUi<CourseAreaDTO> expandedUi) {
            super(stringMessages, dialogTitle, expandedUi);
        }
        
        @Override
        protected com.sap.sailing.gwt.ui.client.StringMessages getStringMessages() {
            return (com.sap.sailing.gwt.ui.client.StringMessages) super.getStringMessages();
        }

        @Override
        protected ListEditorComposite<CourseAreaDTO> createExpandedUi(Iterable<CourseAreaDTO> initialValues, ExpandedListEditorUi<CourseAreaDTO> ui) {
            return new CourseAreaListInlineEditorComposite(initialValues, ui);
        }
    }
    
    static class ExpandedUi extends GenericStringListInlineEditorComposite.ExpandedUi<CourseAreaDTO> {
        private DoubleBox latitudeBox;
        private DoubleBox longitudeBox;
        private DoubleBox radiusBox;
        
        public ExpandedUi(com.sap.sailing.gwt.ui.client.StringMessages stringMessages, ImageResource removeImage, Iterable<String> suggestValues, int textBoxSize) {
            super(stringMessages, removeImage, suggestValues, textBoxSize);
        }
        
        public ExpandedUi(com.sap.sailing.gwt.ui.client.StringMessages stringMessages, ImageResource removeImage, Iterable<String> suggestValues, String placeholderTextForAddTextbox, int textBoxSize) {
            super(stringMessages, removeImage, suggestValues, placeholderTextForAddTextbox, textBoxSize);
        }

        @Override
        protected com.sap.sailing.gwt.ui.client.StringMessages getStringMessages() {
            return (com.sap.sailing.gwt.ui.client.StringMessages) super.getStringMessages();
        }

        @Override
        protected Widget createAddWidget() {
            createAndWireAddButtonAndSuggestBox();
            HorizontalPanel result = new HorizontalPanel();
            result.setSpacing(3);
            result.add(suggestBox);
            result.add(new Label(getStringMessages().latitude()));
            latitudeBox = new DoubleBox();
            latitudeBox.setVisibleLength(10);
            result.add(latitudeBox);
            result.add(new Label(getStringMessages().longitude()));
            longitudeBox = new DoubleBox();
            longitudeBox.setVisibleLength(10);
            result.add(longitudeBox);
            result.add(new Label(getStringMessages().radiusInMeters()));
            radiusBox = new DoubleBox();
            radiusBox.setVisibleLength(5);
            result.add(radiusBox);
            result.add(addButton);
            return result;
        }

        @Override
        protected CourseAreaDTO createNewValue() {
            final Double latitude = latitudeBox.getValue();
            final Double longitude = longitudeBox.getValue();
            final Position centerPosition = latitude == null || longitude == null ? null : new DegreePosition(latitude, longitude);
            final Distance radius = radiusBox.getValue() == null ? null : new MeterDistance(radiusBox.getValue());
            final CourseAreaDTO proxyWithNameAndId = super.createNewValue();
            return new CourseAreaDTO(proxyWithNameAndId.getId(), proxyWithNameAndId.getName(), centerPosition, radius);
        }

        @Override
        protected Widget createValueWidget(int rowIndex, CourseAreaDTO newValue) {
            final HorizontalPanel result = new HorizontalPanel();
            result.setSpacing(3);
            result.add(super.createValueWidget(rowIndex, newValue));
            result.add(new Label(getStringMessages().latitude()));
            final DoubleBox latitudeBox = new DoubleBox();
            latitudeBox.setVisibleLength(10);
            latitudeBox.setEnabled(false);
            if (newValue.getCenterPosition() != null) {
                latitudeBox.setValue(newValue.getCenterPosition().getLatDeg());
            }
            result.add(latitudeBox);
            result.add(new Label(getStringMessages().longitude()));
            final DoubleBox longitudeBox = new DoubleBox();
            longitudeBox.setVisibleLength(10);
            longitudeBox.setEnabled(false);
            if (newValue.getCenterPosition() != null) {
                longitudeBox.setValue(newValue.getCenterPosition().getLngDeg());
            }
            result.add(longitudeBox);
            result.add(new Label(getStringMessages().radiusInMeters()));
            final DoubleBox radiusBox = new DoubleBox();
            radiusBox.setVisibleLength(5);
            radiusBox.setEnabled(false);
            if (newValue.getRadius() != null) {
                radiusBox.setValue(newValue.getRadius().getMeters());
            }
            result.add(radiusBox);
            return result;
        }
    }
    
    @Override
    protected CourseAreaDTO parse(String s) {
        return new CourseAreaDTO(UUID.randomUUID(), s);
    }

    @Override
    protected CourseAreaDTO parse(String s, CourseAreaDTO valueToUpdate) {
        final CourseAreaDTO result;
        if (Util.equalsWithNull(valueToUpdate.getName(), s)) {
            result = valueToUpdate;
        } else {
            result = new CourseAreaDTO(valueToUpdate.getId(), parse(s).getName(), valueToUpdate.getCenterPosition(), valueToUpdate.getRadius());
        }
        return result;
    }

    @Override
    protected String toString(CourseAreaDTO value) {
        return value.getName();
    }
}
