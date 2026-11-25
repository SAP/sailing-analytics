package com.sap.sailing.gwt.managementconsole.places.event.edit;

import java.util.UUID;

import com.google.gwt.place.shared.Prefix;
import com.sap.sailing.gwt.managementconsole.places.AbstractManagementConsolePlace;

public class EditEventPlace extends AbstractManagementConsolePlace {

    private final UUID eventId;

    public EditEventPlace(final UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getEventId() {
        return eventId;
    }

    @Prefix("event/edit")
    public static class Tokenizer extends AbstractManagementConsolePlace.UUIDTokenizer<EditEventPlace> {
        public Tokenizer() {
            super(EditEventPlace::new, EditEventPlace::getEventId);
        }
    }

}
