package com.sap.sailing.gwt.home.mobile.places.start;

import java.util.Collection;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.EventLinkAndMetadataDTO;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.home.communication.start.EventQuickfinderDTO;
import com.sap.sailing.gwt.home.mobile.partials.anniversary.MobileAnniversaries;
import com.sap.sailing.gwt.home.mobile.partials.quickfinder.Quickfinder;
import com.sap.sailing.gwt.home.mobile.partials.stage.Stage;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.anniversary.AnniversariesView;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class StartViewImpl extends Composite implements StartView {
    private static StartPageViewUiBinder uiBinder = GWT.create(StartPageViewUiBinder.class);

    interface StartPageViewUiBinder extends UiBinder<Widget, StartViewImpl> {
    }
    
    private final StringMessages i18n = StringMessages.INSTANCE;

    private Presenter currentPresenter;

    @UiField(provided = true)
    Stage stage;
    @UiField
    MobileAnniversaries anniversariesUi;
    @UiField Quickfinder quickFinderUi;
    @UiField AnchorElement showAllEventsUi;

    public StartViewImpl(Presenter presenter) {
        this.currentPresenter = presenter;
        stage = new Stage(presenter.getNavigator(), true);
        initWidget(uiBinder.createAndBindUi(this));
        presenter.getNavigator().getEventsNavigation().configureAnchorElement(showAllEventsUi);
    }
    
    @Override
    public void setFeaturedEvents(List<? extends EventLinkAndMetadataDTO> list) {
        stage.setFeaturedEvents(list, currentPresenter.getUserService());
    }

    @Override
    public void setQuickFinderValues(Collection<EventQuickfinderDTO> events) {
        // TODO change message: recent events
        quickFinderUi.addPlaceholderItem(i18n.events());
        
        for (EventQuickfinderDTO event : events) {
            String group;
            EventState state = event.getState();
            if(state == EventState.FINISHED) {
                group = i18n.searchResultSortNewest();
            } else if(state == EventState.RUNNING) {
                group = i18n.live();
            } else {
                group = i18n.seriesHeaderUpcoming();
            }
            PlaceNavigation<?> eventPlaceNavigation = currentPresenter.getEventNavigation(event);
            quickFinderUi.addItemToGroup(group, event.getDisplayName(), eventPlaceNavigation);
        }
    }

    @Override
    public AnniversariesView getAnniversariesView() {
        return this.anniversariesUi;
    }
}
