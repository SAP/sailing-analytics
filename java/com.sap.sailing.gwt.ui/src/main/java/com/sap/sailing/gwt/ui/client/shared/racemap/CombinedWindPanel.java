package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.client.EntryPointLinkFactory;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.WindSourceTypeFormatter;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindTrackInfoDTO;
import com.sap.sse.gwt.client.dialog.ConfirmationDialog;
import com.sap.sse.gwt.client.shared.settings.DummyOnSettingsStoredCallback;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserStatusEventHandler;
import com.sap.sse.security.ui.client.premium.PaywallResolver;

public class CombinedWindPanel extends FlowPanel {
    
    private final ImageTransformer transformer;
    private final StringMessages stringMessages;
    
    private final RaceMapImageManager raceMapResources;
    private final Label textLabel;

    private WindTrackInfoDTO windTrackInfoDTO;
    private WindSource windSource;
    
    private Canvas canvas;
    
    private RaceMapStyle raceMapStyle;
    private final CoordinateSystem coordinateSystem;
    
    public CombinedWindPanel(final RaceMap map, RaceMapImageManager theRaceMapResources, RaceMapStyle raceMapStyle,
            StringMessages stringMessages, CoordinateSystem coordinateSystem, PaywallResolver paywallResolver, SecuredDTO raceDTO) {
        this.stringMessages = stringMessages;
        this.coordinateSystem = coordinateSystem;
        this.raceMapResources = theRaceMapResources;
        this.raceMapStyle = raceMapStyle;
        addStyleName(raceMapStyle.raceMapIndicatorPanel());
        addStyleName(raceMapStyle.combinedWindPanel());
        // premium feature control
        if (paywallResolver.hasPermission(SecuredDomainType.TrackedRaceActions.VIEWSTREAMLETS, raceDTO)) {
            addStyleName(raceMapStyle.premiumActive());
        } else {
            addStyleName(raceMapStyle.premiumReady());
        }
        // TODO: create custom confirm dialog with list of available plans or if no plan available a text hint that there is no plan
        // integrate this functions as reusable in paywallResolver
        // TODO: use something like eventBus to get logIn event (see paywalResolver.registerUserStatusEventHandler())
        ConfirmationDialog subscribeDialog = ConfirmationDialog.create(stringMessages.subscriptionSuggestionTitle(),
                stringMessages.pleaseSubscribeToUseSpecific(stringMessages.streamletsOverlayFeature()),
                stringMessages.takeMeToSubscriptions(), stringMessages.cancel(),
                () -> paywallResolver.getUnlockingSubscriptionPlans(SecuredDomainType.TrackedRaceActions.VIEWSTREAMLETS, raceDTO,
                        (unlockingPlans) -> Window
                                .open(EntryPointLinkFactory.createSubscriptionPageLink(unlockingPlans), "_blank", "")));
        transformer = raceMapResources.getCombinedWindIconTransformer();
        transformer.scale(0.9);
        canvas = transformer.getCanvas();
        canvas.addStyleName(this.raceMapStyle.raceMapIndicatorPanelCanvas());
        add(canvas);
        paywallResolver.registerUserStatusEventHandler(new UserStatusEventHandler() {
            @Override
            public void onUserStatusChange(UserDTO user, boolean preAuthenticated) {
                final boolean hasPermission = paywallResolver.hasPermission(SecuredDomainType.TrackedRaceActions.VIEWSTREAMLETS, raceDTO);
                if (hasPermission) {
                    removeStyleName(raceMapStyle.premiumReady());
                    addStyleName(raceMapStyle.premiumActive());
                } else {
                    removeStyleName(raceMapStyle.premiumActive());
                    addStyleName(raceMapStyle.premiumReady());
                }
                updateSettings(map, hasPermission, paywallResolver, raceDTO);
            }
        });
        canvas.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final boolean hasPermission = paywallResolver.hasPermission(SecuredDomainType.TrackedRaceActions.VIEWSTREAMLETS, raceDTO);
                updateSettings(map, hasPermission, paywallResolver, raceDTO);
                if(!hasPermission) {
                    subscribeDialog.center();
                    subscribeDialog.show();
                }
            }
        });
        textLabel = new Label("");
        textLabel.addStyleName(this.raceMapStyle.raceMapIndicatorPanelTextLabel());
        add(textLabel);
    }

    private void updateSettings(final RaceMap map, boolean hasPermission, PaywallResolver paywallResolver, SecuredDTO raceDTO) {
        final RaceMapSettings oldRaceMapSettings = map.getSettings();
        // when off, turn on; when on and no color, turn on color; when on with color, turn off; Only clickable, if permissions granted
        final boolean newShowStreamletsOverlaySetting = (oldRaceMapSettings.isShowWindStreamletOverlay() ?
                oldRaceMapSettings.isShowWindStreamletColors() ? false : true : true) && hasPermission;
        final boolean newShowWindStreamletColors = (oldRaceMapSettings.isShowWindStreamletOverlay()
                ? !oldRaceMapSettings.isShowWindStreamletColors()
                : false) && hasPermission;
        final RaceMapSettings newRaceMapSettings = new RaceMapSettings.RaceMapSettingsBuilder(oldRaceMapSettings,
                raceDTO, paywallResolver)
                        .withShowWindStreamletOverlay(newShowStreamletsOverlaySetting)
                        .withShowWindStreamletColors(newShowWindStreamletColors).build();
        if (map.getComponentContext() != null
                && map.getComponentContext().isStorageSupported(map)) {
            map.getComponentContext().storeSettingsForContext(map, newRaceMapSettings,
                    new DummyOnSettingsStoredCallback());
        }
        map.updateSettings(newRaceMapSettings);
    }

    protected void redraw() {
        if (windTrackInfoDTO != null) {
            if (!windTrackInfoDTO.windFixes.isEmpty()) {
                WindDTO windDTO = windTrackInfoDTO.windFixes.get(0);
                double speedInKnots = windDTO.dampenedTrueWindSpeedInKnots;
                double windFromDeg = windDTO.dampenedTrueWindFromDeg;
                NumberFormat numberFormat = NumberFormat.getFormat("0.0");
                double rotationDegOfWindSymbol = windDTO.dampenedTrueWindBearingDeg;
                transformer.drawTransformedImage(coordinateSystem.mapDegreeBearing(rotationDegOfWindSymbol), 1.0);
                String title = stringMessages.wind() + ": " +  Math.round(windFromDeg) + " " 
                        + stringMessages.degreesShort() + " (" + WindSourceTypeFormatter.format(windSource, stringMessages) + ")" +
                        + '\n'+ stringMessages.clickToToggleWindStreamlets();
                canvas.setTitle(title);
                textLabel.setText(numberFormat.format(speedInKnots) + " " + stringMessages.knotsUnit());
                if (!isVisible()) {
                    setVisible(true);
                }
            } else {
                setVisible(false);
            }
        }
    }
    
    public void setWindInfo(WindTrackInfoDTO windTrackInfoDTO, WindSource windSource) {
        this.windTrackInfoDTO = windTrackInfoDTO;
        this.windSource = windSource;
    }
}
