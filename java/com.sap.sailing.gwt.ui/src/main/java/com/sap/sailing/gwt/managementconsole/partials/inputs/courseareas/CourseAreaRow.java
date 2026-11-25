package com.sap.sailing.gwt.managementconsole.partials.inputs.courseareas;

import java.util.UUID;
import java.util.function.Consumer;

import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DoubleBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.managementconsole.resources.ManagementConsoleResources;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MeterDistance;

/**
 * A single editable course-area row for {@link CourseAreasInput}: shows the course area's name and, behind a small
 * disclosure toggle, its optional geometry (center latitude/longitude and radius in meters). The row preserves the
 * {@link CourseAreaDTO}'s original {@link UUID identity} so that saving an event does not orphan references.
 */
class CourseAreaRow extends Composite implements HasValue<CourseAreaDTO> {
    private final UUID id;
    private final SuggestBox nameBox;
    private final DoubleBox latitudeBox;
    private final DoubleBox longitudeBox;
    private final DoubleBox radiusBox;
    private final FlowPanel geometryPanel;
    private boolean geometryExpanded;

    CourseAreaRow(final CourseAreaDTO courseArea, final StringMessages stringMessages,
            final MultiWordSuggestOracle nameOracle, final Consumer<CourseAreaRow> onRemove) {
        final ManagementConsoleResources appResources = ManagementConsoleResources.INSTANCE;
        this.id = courseArea.getId() != null ? courseArea.getId() : UUID.randomUUID();
        final FlowPanel container = new FlowPanel();
        container.addStyleName(CourseAreasInputResources.INSTANCE.style().courseAreaRow());
        // Header line: name field (auto-suggesting typical course area names), geometry toggle and remove control
        final FlowPanel headerLine = new FlowPanel();
        headerLine.addStyleName(appResources.style().flexContainer());
        nameBox = new SuggestBox(nameOracle);
        nameBox.setValue(courseArea.getName() != null ? courseArea.getName() : "");
        nameBox.getValueBox().getElement().setPropertyString("placeholder", stringMessages.enterCourseAreaName());
        nameBox.addStyleName(appResources.style().flexItemAutoWidth());
        headerLine.add(nameBox);
        final Anchor geometryToggle = new Anchor(stringMessages.latitude() + " / " + stringMessages.longitude());
        geometryToggle.addStyleName(appResources.style().flexItemFixedWidth());
        geometryToggle.addStyleName(CourseAreasInputResources.INSTANCE.style().geometryToggle());
        headerLine.add(geometryToggle);
        final Anchor removeAnchor = new Anchor();
        removeAnchor.addStyleName(appResources.icons().icon());
        removeAnchor.addStyleName(appResources.icons().iconClose());
        removeAnchor.addStyleName(appResources.style().flexItemFixedWidth());
        removeAnchor.addClickHandler(e -> onRemove.accept(this));
        headerLine.add(removeAnchor);
        container.add(headerLine);
        // Geometry line: collapsible latitude / longitude / radius inputs
        geometryPanel = new FlowPanel();
        geometryPanel.addStyleName(appResources.style().flexContainer());
        latitudeBox = createGeometryBox(stringMessages.latitude(), geometryPanel);
        longitudeBox = createGeometryBox(stringMessages.longitude(), geometryPanel);
        radiusBox = createGeometryBox(stringMessages.radiusInMeters(), geometryPanel);
        final Position centerPosition = courseArea.getCenterPosition();
        if (centerPosition != null) {
            latitudeBox.setValue(centerPosition.getLatDeg());
            longitudeBox.setValue(centerPosition.getLngDeg());
        }
        final Distance radius = courseArea.getRadius();
        if (radius != null) {
            radiusBox.setValue(radius.getMeters());
        }
        container.add(geometryPanel);
        // Geometry is revealed automatically when the area already carries coordinates
        geometryExpanded = centerPosition != null || radius != null;
        applyGeometryVisibility();
        geometryToggle.addClickHandler(e -> {
            geometryExpanded = !geometryExpanded;
            applyGeometryVisibility();
        });
        initWidget(container);
    }

    private DoubleBox createGeometryBox(final String labelText, final FlowPanel target) {
        final FlowPanel field = new FlowPanel();
        field.addStyleName(ManagementConsoleResources.INSTANCE.style().flexItemAutoWidth());
        final InlineLabel label = new InlineLabel(labelText);
        label.addStyleName(ManagementConsoleResources.INSTANCE.style().label());
        field.add(label);
        final DoubleBox box = new DoubleBox();
        box.addStyleName(CourseAreasInputResources.INSTANCE.style().geometryBox());
        field.add(box);
        target.add(field);
        return box;
    }

    private void applyGeometryVisibility() {
        geometryPanel.setVisible(geometryExpanded);
    }

    @Override
    public CourseAreaDTO getValue() {
        final Double latitude = latitudeBox.getValue();
        final Double longitude = longitudeBox.getValue();
        final Position centerPosition = latitude == null || longitude == null ? null
                : new DegreePosition(latitude, longitude);
        final Double radiusInMeters = radiusBox.getValue();
        final Distance radius = radiusInMeters == null ? null : new MeterDistance(radiusInMeters);
        return new CourseAreaDTO(id, nameBox.getValue(), centerPosition, radius);
    }

    String getName() {
        return nameBox.getValue();
    }

    void focusName() {
        nameBox.setFocus(/* focused */ true);
    }

    @Override
    public void setValue(final CourseAreaDTO value) {
        setValue(value, /* fireEvents */ false);
    }

    @Override
    public void setValue(final CourseAreaDTO value, final boolean fireEvents) {
        nameBox.setValue(value.getName() != null ? value.getName() : "");
        final Position centerPosition = value.getCenterPosition();
        latitudeBox.setValue(centerPosition != null ? centerPosition.getLatDeg() : null);
        longitudeBox.setValue(centerPosition != null ? centerPosition.getLngDeg() : null);
        final Distance radius = value.getRadius();
        radiusBox.setValue(radius != null ? radius.getMeters() : null);
    }

    @Override
    public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<CourseAreaDTO> handler) {
        return null;
    }
}
