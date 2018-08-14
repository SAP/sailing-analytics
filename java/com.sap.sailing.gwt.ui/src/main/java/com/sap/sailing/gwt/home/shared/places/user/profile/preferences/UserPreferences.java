package com.sap.sailing.gwt.home.shared.places.user.profile.preferences;

import java.util.Collection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelection;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionBoatClassDataProvider;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionCompetitorDataProvider;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.StringMessages;

/**
 * Implementation of {@link UserPreferencesView} where users can change their preferred selections and notifications.
 */
public class UserPreferences extends Composite implements UserPreferencesView {

    private static UserPreferencesUiBinder uiBinder = GWT.create(UserPreferencesUiBinder.class);

    interface UserPreferencesUiBinder extends UiBinder<Widget, UserPreferences> {
    }
    
    interface Style extends CssResource {
        String edgeToEdge();
    }
    
    @UiField Style style;
    @UiField SharedResources res;
    @UiField(provided = true) SuggestedMultiSelection<SimpleCompetitorWithIdDTO> favoriteCompetitorsSelctionUi;
    @UiField(provided = true) SuggestedMultiSelection<BoatClassDTO> favoriteBoatClassesSelctionUi;
    @UiField DivElement notificationsTextUi;

    public UserPreferences(UserPreferencesView.Presenter presenter, FlagImageResolver flagImageResolver) {
        favoriteCompetitorsSelctionUi = new CompetitorDisplayImpl(
                presenter.getFavoriteCompetitorsDataProvider(), flagImageResolver).selectionUi;
        favoriteBoatClassesSelctionUi = new BoatClassDisplayImpl(
                presenter.getFavoriteBoatClassesDataProvider()).selectionUi;
        initWidget(uiBinder.createAndBindUi(this));
        // TODO hide notificationsTextUi if the user's mail address is already verified
    }
    
    public void setEdgeToEdge(boolean edgeToEdge) {
        favoriteBoatClassesSelctionUi.setStyleName(style.edgeToEdge(), edgeToEdge);
        favoriteCompetitorsSelctionUi.setStyleName(style.edgeToEdge(), edgeToEdge);
        favoriteBoatClassesSelctionUi.getElement().getParentElement().removeClassName(res.mediaCss().column());
        favoriteCompetitorsSelctionUi.getElement().getParentElement().removeClassName(res.mediaCss().column());
    }
    
    private class CompetitorDisplayImpl implements SuggestedMultiSelectionCompetitorDataProvider.Display {
        private final SuggestedMultiSelection<SimpleCompetitorWithIdDTO> selectionUi;
        private final HasEnabled notifyAboutResultsUi;
        
        private CompetitorDisplayImpl(final SuggestedMultiSelectionCompetitorDataProvider dataProvider, FlagImageResolver flagImageResolver) {
            selectionUi = SuggestedMultiSelection.forCompetitors(dataProvider, StringMessages.INSTANCE.favoriteCompetitors(), flagImageResolver);
            notifyAboutResultsUi = selectionUi.addNotificationToggle(dataProvider::setNotifyAboutResults,
                    StringMessages.INSTANCE.notificationAboutNewResults());
            dataProvider.addDisplay(this);
        }
        
        @Override
        public void setSelectedItems(Collection<SimpleCompetitorWithIdDTO> selectedItems) {
            selectionUi.setSelectedItems(selectedItems);
        }

        @Override
        public void setNotifyAboutResults(boolean notifyAboutResults) {
            notifyAboutResultsUi.setEnabled(notifyAboutResults);
        }
    }
    
    private class BoatClassDisplayImpl implements SuggestedMultiSelectionBoatClassDataProvider.Display {
        private final SuggestedMultiSelection<BoatClassDTO> selectionUi;
        private final HasEnabled notifyAboutUpcomingRacesUi;
        private final HasEnabled notifyAboutResultsUi;
        
        private BoatClassDisplayImpl(final SuggestedMultiSelectionBoatClassDataProvider dataProvider) {
            selectionUi = SuggestedMultiSelection.forBoatClasses(dataProvider, StringMessages.INSTANCE.favoriteBoatClasses());
            notifyAboutUpcomingRacesUi = selectionUi.addNotificationToggle(dataProvider::setNotifyAboutUpcomingRaces,
                    StringMessages.INSTANCE.notificationAboutUpcomingRaces());
            notifyAboutResultsUi = selectionUi.addNotificationToggle(dataProvider::setNotifyAboutResults,
                    StringMessages.INSTANCE.notificationAboutNewResults());
            dataProvider.addDisplay(this);
        }
        
        @Override
        public void setSelectedItems(Collection<BoatClassDTO> selectedItems) {
            selectionUi.setSelectedItems(selectedItems);
        }
        
        @Override
        public void setNotifyAboutUpcomingRaces(boolean notifyAboutUpcomingRaces) {
            notifyAboutUpcomingRacesUi.setEnabled(notifyAboutUpcomingRaces);
        }
        
        @Override
        public void setNotifyAboutResults(boolean notifyAboutResults) {
            notifyAboutResultsUi.setEnabled(notifyAboutResults);
        }
    }

}
