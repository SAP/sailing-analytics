package com.sap.sailing.gwt.home.client.place.event.multiregatta;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.home.client.place.event.AbstractEventActivity;
import com.sap.sailing.gwt.home.client.place.event.EventClientFactory;
import com.sap.sailing.gwt.home.client.place.event.EventView;
import com.sap.sailing.gwt.home.client.place.event.EventView.PlaceCallback;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;

public class EventMultiregattaActivity extends AbstractEventActivity<AbstractMultiregattaEventPlace> implements EventMultiregattaView.Presenter {

    private EventMultiregattaView currentView = new TabletAndDesktopEventView();

    public EventMultiregattaActivity(AbstractMultiregattaEventPlace place, EventClientFactory clientFactory,
            DesktopPlacesNavigator homePlacesNavigator) {
        super(place, clientFactory, homePlacesNavigator);
    }

    @Override
    public void start(final AcceptsOneWidget panel, final EventBus eventBus) {
        currentView.registerPresenter(this);
        panel.setWidget(currentView);
        currentView.navigateTabsTo(currentPlace);
    }
    
    @Override
    public boolean needsSelectionInHeader() {
        return false;
    }
    
    @Override
    public void forPlaceSelection(PlaceCallback callback) {
    }
    
    @Override
    public boolean showRegattaMetadata() {
        return false;
    }

    @Override
    protected EventView<AbstractMultiregattaEventPlace, ?> getView() {
        return currentView;
    }

}
