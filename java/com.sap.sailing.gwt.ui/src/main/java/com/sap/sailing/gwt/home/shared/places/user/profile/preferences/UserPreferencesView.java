package com.sap.sailing.gwt.home.shared.places.user.profile.preferences;

import com.google.gwt.user.client.ui.IsWidget;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionPresenter;

/**
 * Interface for the user preferences UI. To support desktop as well as mobile version, an
 * {@link #setEdgeToEdge(boolean) edge-to-edge} flag can be provided, in order to meet the respective layout
 * requirements.
 */
public interface UserPreferencesView extends IsWidget {
   
    /**
     * Defines whether or not the {@link UserPreferencesView} should be optimized to fill the whole display width.
     * 
     * @param edgeToEdge
     *            <code>true</code> if this view is used in an edge-to-edge layout (usually in mobile version),
     *            <code>false</code> otherwise
     */
    public void setEdgeToEdge(boolean edgeToEdge);
    
    /**
     * Presenter interface for the user preferences UI, providing methods to load preferences and to access the
     * required {@link SuggestedMultiSelectionPresenter}s.
     */
    public interface Presenter {
        void loadPreferences();

        BoatClassSelectionPresenter getFavoriteBoatClassesDataProvider();

        CompetitorSelectionPresenter getFavoriteCompetitorsDataProvider();

        MiscPreferencesPresenter getMiscPresenter();
    }

}
