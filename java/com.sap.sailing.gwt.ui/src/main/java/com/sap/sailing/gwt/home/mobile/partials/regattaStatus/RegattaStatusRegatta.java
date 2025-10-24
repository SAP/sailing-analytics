package com.sap.sailing.gwt.home.mobile.partials.regattaStatus;

import static com.sap.sailing.gwt.home.desktop.partials.racelist.RaceListDataUtil.getFleetName;

import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.BoatClassImageResolver;
import com.sap.sailing.gwt.common.client.BoatClassImageResources;
import com.sap.sailing.gwt.home.communication.event.LabelType;
import com.sap.sailing.gwt.home.communication.event.LiveRaceDTO;
import com.sap.sailing.gwt.home.communication.eventview.RegattaMetadataDTO;
import com.sap.sailing.gwt.home.mobile.partials.section.IsMobileSection;
import com.sap.sailing.gwt.home.mobile.partials.section.MobileSection;
import com.sap.sailing.gwt.home.mobile.partials.sectionHeader.SectionHeaderContent;
import com.sap.sailing.gwt.home.mobile.partials.sectionHeader.SectionHeaderDataIndicators;
import com.sap.sailing.gwt.home.mobile.places.event.EventViewBase.Presenter;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.regattalist.RegattaListView.RegattaListItem;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;

public class RegattaStatusRegatta extends Composite implements IsMobileSection, RegattaListItem {

    private static RegattaStatusRegattaUiBinder uiBinder = GWT.create(RegattaStatusRegattaUiBinder.class);

    interface RegattaStatusRegattaUiBinder extends UiBinder<Widget, RegattaStatusRegatta> {
    }
    
    @UiField MobileSection itemContainerUi;
    @UiField SectionHeaderContent headerUi;
    private final Presenter presenter;

    public RegattaStatusRegatta(final RegattaMetadataDTO regatta, final Presenter presenter) {
        this.presenter = presenter;
        initWidget(uiBinder.createAndBindUi(this));
        initRegattaHeader(regatta, presenter.getRegattaOverviewNavigation(regatta.getId()));
        headerUi.initAdditionalWidget(new SectionHeaderDataIndicators(regatta.getRaceDataInfo()));
    }

    @Override
    public MobileSection getMobileSection() {
        return itemContainerUi;
    }
    
    public void addRaces(Set<LiveRaceDTO> races) {
        final PaywallResolver paywallResolver = new PaywallResolverImpl(presenter.getUserService(), 
                presenter.getSubscriptionServiceFactory());
        races.stream()
                .map(race -> new RegattaStatusRace(race, presenter::getRaceViewerURL, r -> presenter
                        .getMapAndWindChartUrl(r.getLeaderboardName(), r.getRaceName(), getFleetName(race)), paywallResolver))
                .forEach(itemContainerUi::addContent);
        headerUi.setLabelType(LabelType.LIVE);
    }

    private void initRegattaHeader(RegattaMetadataDTO regatta, final PlaceNavigation<?> placeNavigation) {
        headerUi.setImageUrl(getBootClassIcon(regatta.getBoatClass()).getSafeUri().asString());
        headerUi.setSectionTitle(regatta.getDisplayName());
        headerUi.setClickAction(placeNavigation);
    }
    
    private ImageResource getBootClassIcon(String bootClass) {
        if (bootClass != null) {
            ImageResource image = BoatClassImageResolver.getBoatClassIconResource(bootClass);
            if (image != null) {
                return image;
            }
        }
        return BoatClassImageResources.INSTANCE.genericBoatClass();
    }

    @Override
    public void doFilter(boolean filter) {
        setVisible(!filter);
    }

}
