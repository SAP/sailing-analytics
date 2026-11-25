package com.sap.sailing.gwt.managementconsole.places.event.edit;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RequiresResize;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.managementconsole.mvp.View;
import com.sap.sailing.gwt.ui.shared.EventDTO;

public interface EditEventView extends View<EditEventView.Presenter>, RequiresResize {

    void populateForm(EventDTO event);

    void setSaving(boolean saving);

    interface Presenter extends com.sap.sailing.gwt.managementconsole.mvp.Presenter {
        void saveEvent(String name, String description, String venue, Date startDate, Date endDate,
                List<CourseAreaDTO> courseAreas, boolean isPublic);
        void loadLocalEvents(AsyncCallback<List<EventDTO>> callback);
        void loadRemoteEvents(String baseUrl, String bearerTokenOrNull, AsyncCallback<List<EventDTO>> callback);
        void cancelEdit();
    }

}
