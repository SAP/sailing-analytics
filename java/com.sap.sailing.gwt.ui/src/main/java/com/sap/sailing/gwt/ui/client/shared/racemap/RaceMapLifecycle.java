package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.shared.components.ComponentLifecycle;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;

/**
 * This lifecycle corresponds with the RaceMap
 *
 */
public class RaceMapLifecycle implements ComponentLifecycle<RaceMapSettings> {
    public static final String ID = "rm";

    private final StringMessages stringMessages;
    private final PaywallResolverImpl paywallResolver;
    private final SecuredDTO raceDTO;
    
    public RaceMapLifecycle(StringMessages stringMessages, PaywallResolverImpl paywallResolver, SecuredDTO raceDTO) {
        this.stringMessages = stringMessages;
        this.paywallResolver = paywallResolver;
        this.raceDTO = raceDTO;
    }

    public SecuredDTO getRaceDTO() {
        return raceDTO;
    }

    @Override
    public RaceMapSettingsDialogComponent getSettingsDialogComponent(RaceMapSettings settings) {
        return new RaceMapSettingsDialogComponent(settings, stringMessages,
                /* hasPolar */ true, paywallResolver, raceDTO);
    }

    @Override
    public RaceMapSettings createDefaultSettings() {
        return new RaceMapSettings();
    }

    @Override
    public String getLocalizedShortName() {
        return stringMessages.map();
    }

    @Override
    public String getComponentId() {
        return ID;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public RaceMapSettings extractUserSettings(RaceMapSettings settings) {
        RaceMapSettings defaultSettings = createDefaultSettings();
        return new RaceMapSettings(settings.getZoomSettings(), settings.getHelpLinesSettings(),
                settings.getTransparentHoverlines(), settings.getHoverlineStrokeWeight(),
                settings.getTailLengthInMilliseconds(), settings.isWindUp(), defaultSettings.getBuoyZoneRadius(),
                settings.isShowOnlySelectedCompetitors(), settings.isShowSelectedCompetitorsInfo(),
                settings.isShowWindStreamletColors(), settings.isShowWindStreamletOverlay(),
                settings.isShowSimulationOverlay(), settings.isShowMapControls(), settings.getManeuverTypesToShow(),
                settings.isShowDouglasPeuckerPoints(), settings.isShowEstimatedDuration(),
                settings.getStartCountDownFontSizeScaling(), settings.isShowManeuverLossVisualization(),
                settings.isShowSatelliteLayer(), settings.isShowWindLadder());
    }

    @Override
    public RaceMapSettings extractDocumentSettings(RaceMapSettings settings) {
        RaceMapSettings defaultSettings = createDefaultSettings();
        return new RaceMapSettings(settings.getZoomSettings(), settings.getHelpLinesSettings(),
                settings.getTransparentHoverlines(), settings.getHoverlineStrokeWeight(),
                settings.getTailLengthInMilliseconds(), settings.isWindUp(), defaultSettings.getBuoyZoneRadius(),
                settings.isShowOnlySelectedCompetitors(), settings.isShowSelectedCompetitorsInfo(),
                settings.isShowWindStreamletColors(), settings.isShowWindStreamletOverlay(),
                settings.isShowSimulationOverlay(), settings.isShowMapControls(), settings.getManeuverTypesToShow(),
                settings.isShowDouglasPeuckerPoints(), settings.isShowEstimatedDuration(),
                settings.getStartCountDownFontSizeScaling(), settings.isShowManeuverLossVisualization(),
                settings.isShowSatelliteLayer(), settings.isShowWindLadder());
    }
}
