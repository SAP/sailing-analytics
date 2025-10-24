package com.sap.sailing.gwt.home.mobile.places;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sap.sailing.gwt.home.communication.event.EventAndLeaderboardReferenceWithStateDTO;
import com.sap.sailing.gwt.home.communication.event.EventMetadataDTO;
import com.sap.sailing.gwt.home.communication.eventview.RegattaMetadataDTO;
import com.sap.sailing.gwt.home.mobile.partials.quickfinder.Quickfinder;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventReferenceDTO;

public class QuickfinderPresenter {
    private static final StringMessages MSG = StringMessages.INSTANCE;
    
    public static QuickfinderPresenter getForRegattaLeaderboards(Quickfinder quickfinder,
            final RegattaLeaderboardNavigationProvider navigator,
            Map<String, Set<RegattaMetadataDTO>> regattasByLeaderboardGroupName) {
        return new QuickfinderPresenter(quickfinder, MSG.resultsQuickfinder(), new RegattaPlaceNaviationProvider() {
            @Override
            public PlaceNavigation<?> getPlaceNavigation(String regattaId) {
                return navigator.getRegattaMiniLeaderboardNavigation(regattaId);
            }
        }, regattasByLeaderboardGroupName);
    }
    
    public static QuickfinderPresenter getForRegattaRaces(Quickfinder quickfinder,
            final RegattaRacesNavigationProvider navigator,
            Map<String, Set<RegattaMetadataDTO>> regattasByLeaderboardGroupName) {
        return new QuickfinderPresenter(quickfinder, MSG.racesQuickfinder(), new RegattaPlaceNaviationProvider() {
            @Override
            public PlaceNavigation<?> getPlaceNavigation(String regattaId) {
                return navigator.getRegattaRacesNavigation(regattaId);
            }
        }, regattasByLeaderboardGroupName);
    }
    
    public static QuickfinderPresenter getForRegattaOverview(Quickfinder quickfinder,
            final RegattaOverviewNavigationProvider navigator,
            Map<String, Set<RegattaMetadataDTO>> regattasByLeaderboardGroupName) {
        return new QuickfinderPresenter(quickfinder, MSG.regattaQuickfinder(), new RegattaPlaceNaviationProvider() {
            @Override
            public PlaceNavigation<?> getPlaceNavigation(String regattaId) {
                return navigator.getRegattaOverviewNavigation(regattaId);
            }
        }, regattasByLeaderboardGroupName);
    }
    
    public static <T extends EventAndLeaderboardReferenceWithStateDTO> QuickfinderPresenter getForSeriesLeaderboards(
            Quickfinder quickfinder, String seriesName, final SeriesLeaderboardNavigationProvider navigator,
            Collection<T> eventsOfSeries) {
        return new QuickfinderPresenter(quickfinder, MSG.resultsQuickfinder(), new SeriesEventPlaceNaviationProvider<T>() {
            @Override
            public PlaceNavigation<?> getPlaceNavigation(T event) {
                return navigator.getMiniLeaderboardNavigation(event.getId(), event.getLeaderboardName());
            }
            
            @Override
            public PlaceNavigation<?> getOverallPlaceNavigation() {
                return navigator.getMiniOverallLeaderboardNavigation();
            }
        }, seriesName, eventsOfSeries);
    }
    
    public static <T extends EventReferenceDTO> QuickfinderPresenter getForSeriesEventRaces(Quickfinder quickfinder,
            String seriesName, final SeriesEventRacesNavigationProvider navigator, Collection<T> eventsOfSeries) {
        return new QuickfinderPresenter(quickfinder, MSG.racesQuickfinder(), new SeriesEventPlaceNaviationProvider<T>() {
            @Override
            public PlaceNavigation<?> getPlaceNavigation(T event) {
                return navigator.getSeriesEventRacesNavigation(event.getId());
            }
            
            @Override
            public PlaceNavigation<?> getOverallPlaceNavigation() {
                return null;
            }
        }, seriesName, eventsOfSeries);
    }
    
    public static <T extends EventAndLeaderboardReferenceWithStateDTO> QuickfinderPresenter getForSeriesEventOverview(Quickfinder quickfinder, String seriesName,
            final SeriesEventLeaderboardOverviewNavigationProvider navigator, Collection<T> eventsOfSeries) {
        return new QuickfinderPresenter(quickfinder, MSG.eventQuickfinder(), new SeriesEventPlaceNaviationProvider<T>() {
            @Override
            public PlaceNavigation<?> getPlaceNavigation(T event) {
                return navigator.getSeriesEventLeaderboardOverviewNavigation(event.getId(), event.getLeaderboardName());
            }
            
            @Override
            public PlaceNavigation<?> getOverallPlaceNavigation() {
                return null;
            }
        }, seriesName, eventsOfSeries);
    }
    
    private <T extends EventReferenceDTO> QuickfinderPresenter(Quickfinder quickfinder, String placeholder,
            SeriesEventPlaceNaviationProvider<T> provider, String seriesName, Collection<T> eventsOfSeries) {
        if (eventsOfSeries == null) {
            quickfinder.removeFromParent();
            return;
        }
        quickfinder.addPlaceholderItem(placeholder);
        PlaceNavigation<?> overallPlaceNavigation = provider.getOverallPlaceNavigation();
        if (overallPlaceNavigation != null) {
            quickfinder.addItemToGroup(seriesName, MSG.overallLeaderboardSelection(), overallPlaceNavigation);
        }
        for (T eventOfSeries : eventsOfSeries) {
            String displayName = eventOfSeries.getDisplayName();
            if(eventOfSeries instanceof EventMetadataDTO) {
                displayName = ((EventMetadataDTO) eventOfSeries).getLocationOrDisplayName();
            }
            quickfinder.addItemToGroup(seriesName, displayName, provider.getPlaceNavigation(eventOfSeries));
        }
    }
    
    private QuickfinderPresenter(Quickfinder quickfinder, String placeholder, RegattaPlaceNaviationProvider provider, 
            Map<String, Set<RegattaMetadataDTO>> regattasByLeaderboardGroupName) {
        if (regattasByLeaderboardGroupName == null) {
            quickfinder.removeFromParent();
            return;
        }
        quickfinder.addPlaceholderItem(placeholder);
        for (Entry<String, Set<RegattaMetadataDTO>> entry : regattasByLeaderboardGroupName.entrySet()) {
            String leaderboardGroupName = entry.getKey() == null ? MSG.regattas() : entry.getKey();
            for (RegattaMetadataDTO regattaMetadata : entry.getValue()) {
                quickfinder.addItemToGroup(leaderboardGroupName, regattaMetadata.getDisplayName(),
                        provider.getPlaceNavigation(regattaMetadata.getId()));
            }
        }
    }
    
    private interface SeriesEventPlaceNaviationProvider<T> {
        PlaceNavigation<?> getOverallPlaceNavigation();

        PlaceNavigation<?> getPlaceNavigation(T event);
    }
    
    private interface RegattaPlaceNaviationProvider {
        PlaceNavigation<?> getPlaceNavigation(String regattaId);
    }
}
