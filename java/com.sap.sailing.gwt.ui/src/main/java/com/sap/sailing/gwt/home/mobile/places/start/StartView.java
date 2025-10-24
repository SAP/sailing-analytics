package com.sap.sailing.gwt.home.mobile.places.start;

import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.EventLinkAndMetadataDTO;
import com.sap.sailing.gwt.home.communication.start.EventQuickfinderDTO;
import com.sap.sailing.gwt.home.mobile.app.MobilePlacesNavigator;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.anniversary.AnniversariesView;
import com.sap.sse.security.ui.client.UserService;

public interface StartView {

    Widget asWidget();
    
    void setQuickFinderValues(Collection<EventQuickfinderDTO> events);
    
    public interface Presenter {
        MobilePlacesNavigator getNavigator();
        
        PlaceNavigation<?> getEventNavigation(EventQuickfinderDTO event);

        UserService getUserService();
    }

    void setFeaturedEvents(List<? extends EventLinkAndMetadataDTO> list);

    AnniversariesView getAnniversariesView();
}

