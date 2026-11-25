package com.sap.sailing.gwt.managementconsole.places.event.edit;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.managementconsole.partials.inputs.courseareas.CourseAreasInput;
import com.sap.sailing.gwt.managementconsole.places.UiUtils;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.VenueDTO;
import com.sap.sse.gwt.client.controls.datetime.DateAndTimeInput;
import com.sap.sse.gwt.client.controls.datetime.DateTimeInput.Accuracy;

public class EditEventViewImpl extends Composite implements EditEventView {

    interface EditEventViewUiBinder extends UiBinder<Widget, EditEventViewImpl> {}
    private static EditEventViewUiBinder uiBinder = GWT.create(EditEventViewUiBinder.class);

    @UiField
    Button saveEventButton;
    @UiField
    Anchor back;
    @UiField
    Element headerContainer;
    @UiField
    ScrollPanel scrollContainer;
    @UiField(provided = true)
    DateAndTimeInput startDateInput;
    @UiField(provided = true)
    DateAndTimeInput endDateInput;
    @UiField
    CourseAreasInput courseAreasInput;
    @UiField
    InputElement venueInput, nameInput, isPublicCheckbox;
    @UiField
    TextArea descriptionInput;

    private Presenter presenter;

    public EditEventViewImpl() {
        startDateInput = new DateAndTimeInput(Accuracy.MINUTES);
        endDateInput = new DateAndTimeInput(Accuracy.MINUTES);
        initWidget(uiBinder.createAndBindUi(this));
        back.addClickHandler(e -> presenter.cancelEdit());
        saveEventButton.addClickHandler(e -> validateAndSaveEvent());
    }

    @Override
    public void populateForm(EventDTO event) {
        nameInput.setValue(event.getName() != null ? event.getName() : "");
        descriptionInput.setValue(event.getDescription() != null ? event.getDescription() : "");

        VenueDTO venue = event.getVenue();
        venueInput.setValue(venue != null && venue.getName() != null ? venue.getName() : "");

        startDateInput.setValue(event.startDate);
        endDateInput.setValue(event.endDate);
        isPublicCheckbox.setChecked(event.isPublic);
        // Populate course areas, preserving their identity and geometry
        final List<CourseAreaDTO> courseAreas = venue != null && venue.getCourseAreas() != null
                ? venue.getCourseAreas() : new ArrayList<CourseAreaDTO>();
        courseAreasInput.setValue(courseAreas);
    }

    private void validateAndSaveEvent() {
        if (validate()) {
            presenter.saveEvent(
                    nameInput.getValue(),
                    descriptionInput.getValue(),
                    venueInput.getValue(),
                    startDateInput.getValue(),
                    endDateInput.getValue(),
                    courseAreasInput.getValue(),
                    isPublicCheckbox.isChecked());
        }
    }

    private boolean validate() {
        return UiUtils.isNotBlank(nameInput.getValue())
                && UiUtils.isNotBlank(venueInput.getValue());
    }

    @Override
    public void setSaving(final boolean saving) {
        saveEventButton.setEnabled(!saving);
    }

    @Override
    public void setPresenter(final Presenter presenter) {
        this.presenter = presenter;
        courseAreasInput.setDataProvider(new CourseAreasInput.DataProvider() {
            @Override
            public void getLocalEvents(final AsyncCallback<List<EventDTO>> callback) {
                presenter.loadLocalEvents(callback);
            }
            @Override
            public void getRemoteEvents(final String baseUrl, final String bearerTokenOrNull,
                    final AsyncCallback<List<EventDTO>> callback) {
                presenter.loadRemoteEvents(baseUrl, bearerTokenOrNull, callback);
            }
        });
    }

    @Override
    public void onResize() {
        final int scrollContainerHeight = getElement().getOffsetHeight() - headerContainer.getOffsetHeight();
        scrollContainer.getElement().getStyle().setHeight(scrollContainerHeight, Unit.PX);
    }

}
