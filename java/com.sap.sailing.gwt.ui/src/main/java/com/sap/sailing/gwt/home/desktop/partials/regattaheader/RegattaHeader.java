package com.sap.sailing.gwt.home.desktop.partials.regattaheader;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.eventview.RegattaMetadataDTO;
import com.sap.sailing.gwt.home.communication.eventview.RegattaMetadataDTO.RaceDataInfo;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;

public class RegattaHeader extends Composite {

    private static RegattaHeaderUiBinder uiBinder = GWT.create(RegattaHeaderUiBinder.class);

    interface RegattaHeaderUiBinder extends UiBinder<Widget, RegattaHeader> {
    }
    
    @UiField RegattaHeaderResources local_res;
    
    @UiField AnchorElement headerBodyUi;
    @UiField AnchorElement headerArrowUi;
    @UiField AnchorElement dataIndicatorsUi;
    @UiField DivElement gpsDataIndicatorUi;
    @UiField DivElement windDataIndicatorUi;
    @UiField DivElement videoDataIndicatorUi;
    @UiField DivElement audioDataIndicatorUi;

    public RegattaHeader(RegattaMetadataDTO regattaMetadata, boolean showStateMarker) {
        initWidget(uiBinder.createAndBindUi(this));
        headerBodyUi.appendChild(new RegattaHeaderBody(regattaMetadata, showStateMarker).getElement());
        this.initDataIndicators(regattaMetadata.getRaceDataInfo());
    }
    
    public void setRegattaNavigation(PlaceNavigation<?> placeNavigation) {
        headerArrowUi.getStyle().clearDisplay();
        dataIndicatorsUi.addClassName(local_res.css().regattaheader_indicators_next_to_arrow());
        placeNavigation.configureAnchorElement(headerBodyUi);
        placeNavigation.configureAnchorElement(headerArrowUi);
    }
    
    public void setRegattaRacesNavigation(PlaceNavigation<?> placeNavigation) {
        placeNavigation.configureAnchorElement(dataIndicatorsUi);
    }
    
    private void initDataIndicators(RaceDataInfo raceDataInfo) {
        String disabledStyle = local_res.css().regattaheader_indicator_disabled();
        UIObject.setStyleName(gpsDataIndicatorUi, disabledStyle, !raceDataInfo.hasGPSData());
        UIObject.setStyleName(windDataIndicatorUi, disabledStyle, !raceDataInfo.hasWindData());
        UIObject.setStyleName(videoDataIndicatorUi, disabledStyle, !raceDataInfo.hasVideoData());
        UIObject.setStyleName(audioDataIndicatorUi, disabledStyle, !raceDataInfo.hasAudioData());
    }

    
}
