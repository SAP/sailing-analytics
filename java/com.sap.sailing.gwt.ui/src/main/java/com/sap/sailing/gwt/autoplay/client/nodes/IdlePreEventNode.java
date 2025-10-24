package com.sap.sailing.gwt.autoplay.client.nodes;

import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.events.AutoPlayHeaderEvent;
import com.sap.sailing.gwt.autoplay.client.nodes.base.FiresPlaceNode;
import com.sap.sailing.gwt.autoplay.client.places.screens.preevent.IdlePreEventPlace;

public class IdlePreEventNode extends FiresPlaceNode {
    private final AutoPlayClientFactory cf;

    public IdlePreEventNode(AutoPlayClientFactory cf) {
        super(IdlePreEventNode.class.getName());
        this.cf = cf;
    }

    @Override
    public void onStart() {
        String eventName = cf.getAutoPlayCtxSignalError().getEvent().getName();
        getBus().fireEvent(new AutoPlayHeaderEvent(eventName, ""));
        setPlaceToGo(new IdlePreEventPlace());
        firePlaceChangeAndStartTimer();
    }
}
