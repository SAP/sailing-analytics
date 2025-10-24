package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sap.sailing.gwt.ui.client.MediaServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sse.gwt.client.controls.datetime.DateTimeInput.Accuracy;

public class EventEditDialog extends EventDialog {
    public EventEditDialog(EventDTO event, Collection<EventDTO> otherExistingEvents, List<LeaderboardGroupDTO> availableLeaderboardGroups,
            SailingServiceWriteAsync sailingServiceWrite, StringMessages stringMessages, DialogCallback<EventDTO> callback, MediaServiceAsync mediaService) {
        super(new EventParameterValidator(stringMessages, otherExistingEvents), sailingServiceWrite, stringMessages, availableLeaderboardGroups, event.getLeaderboardGroups(), callback);
        nameEntryField = createTextBox(event.getName());
        nameEntryField.setVisibleLength(50);
        descriptionEntryField = createTextArea(event.getDescription());
        descriptionEntryField.setCharacterWidth(50);
        descriptionEntryField.setVisibleLines(2);
        descriptionEntryField.getElement().getStyle().setProperty("resize", "none");
        venueEntryField = createTextBox(event.getVenue().getName());
        venueEntryField.setVisibleLength(35);
        startDateBox = createDateTimeBox(event.startDate, Accuracy.MINUTES);
        endDateBox = createDateTimeBox(event.endDate, Accuracy.MINUTES);
        isPublicCheckBox = createCheckbox("");
        isPublicCheckBox.setValue(event.isPublic);
        baseURLEntryField = createTextBox(event.getBaseURL());
        baseURLEntryField.setVisibleLength(50);
        id = event.id;
        courseAreaNameList.setValue(new ArrayList<>(event.getVenue().getCourseAreas()));
        List<String> leaderboardGroupNames = new ArrayList<>();
        for(LeaderboardGroupDTO leaderboardGroupDTO: event.getLeaderboardGroups()) {
            leaderboardGroupNames.add(leaderboardGroupDTO.getName());
        }
        leaderboardGroupList.setValue(leaderboardGroupNames);
        imagesListComposite.fillImages(event.getImages());
        videosListComposite.fillVideos(event.getVideos());
        windFinderSpotCollectionIdsComposite.setValue(event.getWindFinderReviewedSpotsCollectionIds());
        externalLinksComposite.fillExternalLinks(event);
    }
}
