package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.shared.components.ComponentLifecycle;
import com.sap.sse.security.ui.client.premium.PaywallResolver;

/**
 * This lifecycle corresponds with the RaceMap
 *
 */
public class RaceMapLifecycle implements ComponentLifecycle<RaceMapSettings> {
    public static final String ID = "rm";

    private final StringMessages stringMessages;
    private final PaywallResolver paywallResolver;
    
    public RaceMapLifecycle(StringMessages stringMessages, PaywallResolver paywallResolver) {
        this.stringMessages = stringMessages;
        this.paywallResolver = paywallResolver;
    }
    
    @Override
    public RaceMapSettingsDialogComponent getSettingsDialogComponent(RaceMapSettings settings) {
        return new RaceMapSettingsDialogComponent(settings, stringMessages,
                /* isSimulationEnabled: enable simulation because we don't know the boat class
                 * here yet and therefore cannot reasonably judge whether polar data is
                 * available; if in doubt, rather enable selecting it */ true, paywallResolver);
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
                settings.isShowSatelliteLayer());
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
                settings.isShowSatelliteLayer());
    }
}
