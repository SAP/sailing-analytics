package com.sap.sailing.gwt.home.communication.user.profile;

import java.util.ArrayList;
import java.util.Collection;

import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sse.gwt.shared.DTO;

/**
 * {@link DTO} implementation representing the favorite competitor preferences.
 */
public class FavoriteCompetitorsDTO implements DTO {
   
    private ArrayList<SimpleCompetitorWithIdDTO> selectedCompetitors = new ArrayList<>();
    private boolean notifyAboutResults;

    protected FavoriteCompetitorsDTO() {}
    
    public FavoriteCompetitorsDTO(Collection<SimpleCompetitorWithIdDTO> selectedCompetitors,
            boolean notifyAboutResults) {
        this.selectedCompetitors.addAll(selectedCompetitors);
        this.notifyAboutResults = notifyAboutResults;
    }
    
    public Collection<SimpleCompetitorWithIdDTO> getSelectedCompetitors() {
        return selectedCompetitors;
    }
    
    public boolean isNotifyAboutResults() {
        return notifyAboutResults;
    }

}
