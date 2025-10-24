package com.sap.sailing.gwt.home.communication.user.profile;

import java.util.ArrayList;
import java.util.Collection;

import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sse.gwt.shared.DTO;

/**
 * {@link DTO} implementation representing the favorite boat class preferences.
 */
public class FavoriteBoatClassesDTO implements DTO {
   
    private ArrayList<BoatClassDTO> selectedBoatClasses = new ArrayList<>();
    private boolean notifyAboutUpcomingRaces;
    private boolean notifyAboutResults;

    protected FavoriteBoatClassesDTO() {}
    
    public FavoriteBoatClassesDTO(Collection<BoatClassDTO> selectedBoatClasses, boolean notifyAboutUpcomingRaces,
            boolean notifyAboutResults) {
        this.selectedBoatClasses.addAll(selectedBoatClasses);
        this.notifyAboutUpcomingRaces = notifyAboutUpcomingRaces;
        this.notifyAboutResults = notifyAboutResults;
    }
    
    public Collection<BoatClassDTO> getSelectedBoatClasses() {
        return selectedBoatClasses;
    }
    
    public boolean isNotifyAboutUpcomingRaces() {
        return notifyAboutUpcomingRaces;
    }
    
    public boolean isNotifyAboutResults() {
        return notifyAboutResults;
    }

}
