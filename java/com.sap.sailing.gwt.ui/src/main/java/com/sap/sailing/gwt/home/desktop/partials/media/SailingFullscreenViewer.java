package com.sap.sailing.gwt.home.desktop.partials.media;

import java.util.Collection;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.shared.SharedHomeResources;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.fullscreen.FullscreenContainer;
import com.sap.sailing.gwt.home.shared.places.event.EventDefaultPlace;
import com.sap.sailing.gwt.ui.shared.EventLinkDTO;
import com.sap.sailing.gwt.ui.shared.SailingImageDTO;
import com.sap.sse.gwt.client.controls.carousel.ImageCarousel.FullscreenViewer;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

/**
 * Fullscreen viewer for the image gallery that shows the current image in a big view and lists all images on the bottom
 * as small slider. If a {@link DesktopPlacesNavigator navigator} is provided, a link to the related event of the
 * currently selected image is shown in the upper right corner.
 */
public class SailingFullscreenViewer extends FullscreenContainer<SailingGalleryPlayer> implements
        FullscreenViewer<SailingImageDTO> {
    
    private static final String IS_AUTOPLAYING_STYLE = SailingFullscreenViewerResources.INSTANCE.css().is_autoplaying();
    
    private final EventNavigationHandler eventNavigationHandler;
    private final Anchor eventLinkControl = new Anchor();
    private final Image autoRefreshControl = new Image(SharedHomeResources.INSTANCE.reload().getSafeUri());
    private SailingGalleryPlayer player = null;

    private final TakedownNoticeService takedownNoticeService;

    /**
     * Creates a new {@link SailingFullscreenViewer} without navigation to the respectively related event.
     * @param takedownNoticeService TODO
     * 
     * @see #SailingFullscreenViewer(DesktopPlacesNavigator, TakedownNoticeService)
     */
    public SailingFullscreenViewer(TakedownNoticeService takedownNoticeService) {
        this(null, takedownNoticeService);
    }

    /**
     * Creates a new {@link SailingFullscreenViewer} including navigation to the related event of the currently selected
     * image using via the provided {@link DesktopPlacesNavigator navigator}.
     * 
     * @param navigator
     *            {@link DesktopPlacesNavigator} to navigate to the respectively related event page
     */
    public SailingFullscreenViewer(final DesktopPlacesNavigator navigator, TakedownNoticeService takedownNoticeService) {
        SailingFullscreenViewerResources.INSTANCE.css().ensureInjected();
        this.takedownNoticeService = takedownNoticeService;
        eventNavigationHandler = new EventNavigationHandler(navigator);
        if (eventNavigationHandler.isNavigationConfigured()) {
            eventLinkControl.addStyleName(SharedResources.INSTANCE.mainCss().buttonarrowrightwhite());
            eventLinkControl.setTabIndex(-1);
            eventLinkControl.setTarget("_blank");
            addToolbarAction(eventLinkControl);
        }
        autoRefreshControl.addClickHandler(event -> SailingFullscreenViewer.this.toggleAutoplay());
        addToolbarAction(autoRefreshControl);
    }

    public void show(SailingImageDTO selected, Collection<SailingImageDTO> images) {
        showContent(player = new SailingGalleryPlayer(selected, images, takedownNoticeService));
        if (eventNavigationHandler.isNavigationConfigured()) {
            player.addSelectionChangeHandler(eventNavigationHandler);
            player.addClickHandler(eventNavigationHandler);
        }
        autoRefreshControl.setStyleName(IS_AUTOPLAYING_STYLE, player.isAutoplaying());
        SelectionChangeEvent.fire(player);
    }
    
    private void toggleAutoplay() {
        if (player != null) {
            player.toggleAutoplay();
            autoRefreshControl.setStyleName(IS_AUTOPLAYING_STYLE, player.isAutoplaying());
        }
    }

    private class EventNavigationHandler implements ClickHandler, SelectionChangeEvent.Handler {

        private final DesktopPlacesNavigator navigator;
        private PlaceNavigation<EventDefaultPlace> eventNavigation;

        private EventNavigationHandler(DesktopPlacesNavigator navigator) {
            this.navigator = navigator;
        }

        @Override
        public void onSelectionChange(SelectionChangeEvent event) {
            final EventLinkDTO eventLink = player.getSelectedImage().getEventLink();
            final String evnetId = String.valueOf(eventLink.getId()), baseURL = eventLink.getBaseURL();
            eventNavigation = navigator.getEventNavigation(evnetId, baseURL, eventLink.isOnRemoteServer());
            eventLinkControl.setText(eventLink.getDisplayName());
            eventLinkControl.setHref(eventNavigation.getSafeTargetUrl());
        }

        @Override
        public void onClick(ClickEvent event) {
            player.focus();
            Window.open(eventNavigation.getTargetUrl(), "_blank", null);
        }

        private boolean isNavigationConfigured() {
            return navigator != null;
        }
    }
}
