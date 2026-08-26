package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sse.gwt.client.controls.datetime.DateTimeInput.Accuracy;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.VideoDTO;

public class EventCreateDialog extends EventDialog {

    public EventCreateDialog(Collection<EventDTO> existingEvents, List<LeaderboardGroupDTO> availableLeaderboardGroups, 
            SailingServiceWriteAsync sailingServiceWrite, StringMessages stringMessages, DialogCallback<EventDTO> callback) {
        super(new EventParameterValidator(stringMessages, existingEvents), sailingServiceWrite, stringMessages,
                availableLeaderboardGroups, /* leaderboardGroups */ Collections.<LeaderboardGroupDTO>emptyList(), existingEvents, callback);
        nameEntryField = createTextBox(null);
        nameEntryField.setVisibleLength(50);
        nameEntryField.ensureDebugId("NameTextBox");
        descriptionEntryField = createTextArea(null);
        descriptionEntryField.setCharacterWidth(50);
        descriptionEntryField.setVisibleLines(2);
        descriptionEntryField.getElement().getStyle().setProperty("resize", "none");
        descriptionEntryField.ensureDebugId("DescriptionTextArea");
        venueEntryField = createTextBox(null);
        venueEntryField.setVisibleLength(35);
        venueEntryField.ensureDebugId("VenueTextBox");
        final Date now = new Date();
        startDateBox = createDateTimeBox(now, Accuracy.MINUTES);
        startDateBox.ensureDebugId("StartDateTimeBox");
        endDateBox = createDateTimeBox(now, Accuracy.MINUTES);
        endDateBox.ensureDebugId("EndDateTimeBox");
        isPublicCheckBox = createCheckbox("");
        isPublicCheckBox.setValue(false);
        isPublicCheckBox.ensureDebugId("IsPublicCheckBox");
        baseURLEntryField = createTextBox(null);
        baseURLEntryField.setVisibleLength(50);
        imagesListComposite.fillImages(Collections.<ImageDTO>emptyList());
        videosListComposite.fillVideos(Collections.<VideoDTO>emptyList());
        // add default course area
        courseAreaList.setValue(Collections.singletonList(new CourseAreaDTO(UUID.randomUUID(), "Default")));
    }
}
