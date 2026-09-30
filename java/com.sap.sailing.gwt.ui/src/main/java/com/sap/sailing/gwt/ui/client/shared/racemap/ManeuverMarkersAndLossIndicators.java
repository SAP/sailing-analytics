package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.overlays.Marker;
import com.google.gwt.maps.client.overlays.MarkerOptions;
import com.google.gwt.maps.client.overlays.Polyline;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.maneuverdetection.impl.ManeuverDetectorImpl;
import com.sap.sailing.gwt.ui.actions.GetManeuversForCompetitorsAction;
import com.sap.sailing.gwt.ui.client.ManeuverTypeFormatter;
import com.sap.sailing.gwt.ui.client.NauticalSideFormatter;
import com.sap.sailing.gwt.ui.client.NumberFormatterFactory;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.ManeuverDTO;
import com.sap.sailing.gwt.ui.shared.SpeedWithBearingDTO;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.Timer.PlayStates;

/**
 * Encapsulates the diamond-shaped maneuver markers that can be displayed on a competitor's
 * tail, as well as the optional maneuver loss indicators that may be shown for a maneuver
 * marker.<p>
 * 
 * Invariants:
 * 
 * <ul>
 * <li>When a maneuver loss indicator is shown then the corresponding maneuver marker is
 * visible as well.</li>
 * <li>When adding a maneuver marker to the map display, the {@link RaceMapSettings#isShowManeuverLossVisualization()} setting
 * decides whether a maneuver loss indicator is added to the map display as well.</li>
 * </ul>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class ManeuverMarkersAndLossIndicators {
    private static final String MANEUVERLOSSLINES_PROJECTEDLINES_COLOR = "#ffff00";
    private static final String MANEUVERLOSSLINES_EXTRAPOLATEDLINES_COLOR = "#ffffff";
    private static final String MANEUVERLOSSLINES_RED = "#ff0000";
    private static final String MANEUVERLOSSLINES_GREEN = "#00ff00";
    private static final double LOWLIGHTED_LINE_OPACITY = 0.5;
    private static final int HIGHLIGHTED_LINE_STROKEWEIGHT = 2;
    private static final NumberFormat numberFormatOneDecimal = NumberFormatterFactory.getDecimalFormat(1);

    private final RaceMap raceMap;
    private final SailingServiceAsync sailingService;
    private final StringMessages stringMessages;
    private final ErrorReporter errorReporter;
    
    /**
     * Keys are the competitor IDs as string
     */
    private Map<String, List<ManeuverDTO>> lastManeuverResult;
    
    /**
     * markers displayed in response to
     * {@link SailingServiceAsync#getDouglasPoints(String, String, Map, Map, double, AsyncCallback)}
     */
    private final Map<Triple<String, Date, ManeuverType>, Marker> maneuverMarkers;
    
    /** The Map where the polylines for the specific maneuver are stored. */
    private final Map<Triple<String, Date, ManeuverType>, Set<Polyline>> maneuverLossLinesMap;
    
    /** The Map where the info overlays for a maneuver loss visualization are stored. */
    private final Map<Triple<String, Date, ManeuverType>, SmallTransparentInfoOverlay> maneuverLossInfoOverlayMap;
    
    private final AsyncActionsExecutor asyncActionsExecutor;

    public ManeuverMarkersAndLossIndicators(RaceMap raceMap, SailingServiceAsync sailingService,
            ErrorReporter errorReporter, StringMessages stringMessages, AsyncActionsExecutor asyncActionsExecutor) {
        this.raceMap = raceMap;
        this.asyncActionsExecutor = asyncActionsExecutor;
        this.sailingService = sailingService;
        this.errorReporter = errorReporter;
        this.stringMessages = stringMessages;
        this.maneuverMarkers = new HashMap<>();
        this.maneuverLossLinesMap = new HashMap<>();
        this.maneuverLossInfoOverlayMap = new HashMap<>();
    }

    public void getAndShowManeuvers(RegattaAndRaceIdentifier race, Map<String, TimeRange> timeRange) {
        asyncActionsExecutor.execute(new GetManeuversForCompetitorsAction(sailingService, race, timeRange),
                new AsyncCallback<Map<String, List<ManeuverDTO>>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Error obtaining maneuvers: " + caught.getMessage(), true /*silentMode */);
            }

            @Override
            public void onSuccess(Map<String, List<ManeuverDTO>> result) {
                lastManeuverResult = result;
                removeAllManeuverMarkers();
                if (raceMap.getTimer().getPlayState() != PlayStates.Playing) {
                    showManeuvers(result);
                }
            }
        });
    }

    /**
     * Removes the visualizations of maneuvers from the map. The maneuver objects as received from the server are remembered
     * (see {@link #lastManeuverResult}) and may be restored by a subsequent call to {@link #showManeuvers(Map)} as it is, e.g.,
     * called by {@link #updateManeuverMarkersAfterSettingsChanged()}.
     */
    void removeAllManeuverMarkers() {
        // clone entry set to avoid concurrent modification exception
        for (final Entry<Triple<String, Date, ManeuverType>, Marker> keyAndMarker : new HashSet<>(maneuverMarkers.entrySet())) {
            removeManeuverMarker(keyAndMarker.getKey(), keyAndMarker.getValue());
        }
    }

    private void removeManeuverMarker(Triple<String, Date, ManeuverType> key, Marker marker) {
        marker.setMap((MapWidget) null);
        removeManeuverLossLinesAndInfoOverlayForManeuver(key);
        maneuverMarkers.remove(key);
    }

    private void showManeuvers(Map<String, List<ManeuverDTO>> maneuversForCompetitorIdsAsStrings) {
        if (raceMap.getMap() != null && maneuversForCompetitorIdsAsStrings != null) {
            for (final Entry<String, List<ManeuverDTO>> e : maneuversForCompetitorIdsAsStrings.entrySet()) {
                final String competitorIdAsString = e.getKey();
                final List<ManeuverDTO> maneuversForCompetitor = e.getValue();
                for (ManeuverDTO maneuver : maneuversForCompetitor) {
                    if (raceMap.getSettings().isShowManeuverType(maneuver.getType())) {
                        createAndAddMarkerOfManeuver(maneuver, competitorIdAsString);
                    }
                }
            }
        }
    }

    /**
     * Creates a maneuver marker. Additionally, if the {@link RaceMapSettings#isShowManeuverLossVisualization()} setting is {@code true},
     * a maneuver loss indicator is shown.
     */
    private void createAndAddMarkerOfManeuver(ManeuverDTO maneuver, String competitorIdAsString) {
        LatLng latLng = raceMap.getCoordinateSystem().toLatLng(maneuver.getPosition());
        Marker maneuverMarker = raceMap.getRaceMapImageManager().getManeuverIconsForTypeAndDirectionIndicatingColor()
                .get(new Pair<ManeuverType, ManeuverColor>(maneuver.getType(), ManeuverColor.getManeuverColor(maneuver)));
        MarkerOptions options = MarkerOptions.newInstance();
        options.setIcon(maneuverMarker.getIcon_MarkerImage());
        Marker marker = Marker.newInstance(options);
        marker.setPosition(latLng);
        marker.setTitle(ManeuverTypeFormatter.format(maneuver.getType(), stringMessages));
        marker.setZindex(RaceMapOverlaysZIndexes.MANEUVERMARK_ZINDEX);
        marker.addClickHandler(event -> {
            LatLng where = raceMap.getCoordinateSystem().toLatLng(maneuver.getPosition());
            Widget content = getInfoWindowContent(maneuver, competitorIdAsString);
            raceMap.getManagedInfoWindow().openAtPosition(content, where);
        });
        final Triple<String, Date, ManeuverType> key = createManeuverKey(maneuver, competitorIdAsString);
        maneuverMarkers.put(key, marker);
        marker.setMap(raceMap.getMap());
        // maneuver loss visualization: if the setting is active, show loss visualization for the maneuver by default
        if (raceMap.getSettings().isShowManeuverLossVisualization() && maneuver.getManeuverLoss() != null) {
            createManeuverLossLinesAndInfoOverlays(maneuver, competitorIdAsString);
        }
    }

    private Triple<String, Date, ManeuverType> createManeuverKey(ManeuverDTO maneuver, String competitorIdAsString) {
        return new Triple<>(competitorIdAsString, maneuver.getTimePoint(), maneuver.getType());
    }

    final LineInfoProvider middleManeuverAngleLineInfoProvider = new LineInfoProvider() {
        @Override
        public String getLineInfo() {
            return stringMessages.middleManeuverAngle();
        }

        @Override
        public boolean isShowInfoOverlayWithHelplines() {
            return false;
        }
    };
    final LineInfoProvider extrapolatedLineInfoProvider = new LineInfoProvider() {
        @Override
        public String getLineInfo() {
            return stringMessages.extrapolatedManeuverStartPosition();
        }
        @Override
        public boolean isShowInfoOverlayWithHelplines() {
            return false;
        }
    };
    final LineInfoProvider projectedExtrapolatedLineInfoProvider = new LineInfoProvider() {
        @Override
        public String getLineInfo() {
            return stringMessages.projectedExtrapolatedManeuverStartPosition();
        }
        @Override
        public boolean isShowInfoOverlayWithHelplines() {
            return false;
        }
    };
    final LineInfoProvider projectedManeuverEndLineInfoProvider = new LineInfoProvider() {
        @Override
        public String getLineInfo() {
            return stringMessages.projectedManeuverEndPosition();
        }
        @Override
        public boolean isShowInfoOverlayWithHelplines() {
            return false;
        }
    };
    final LineInfoProvider maneuverLossLineInfoProvider = new LineInfoProvider() {
        @Override
        public String getLineInfo() {
            return stringMessages.maneuverLoss();
        }
        @Override
        public boolean isShowInfoOverlayWithHelplines() {
            return false;
        }
    };
    final LineInfoProvider bearingAtManeuverEndPositionLineInfoProvider = new LineInfoProvider() {
        @Override
        public String getLineInfo() {
            return stringMessages.bearingAtManeuverEndPosition();
        }
        @Override
        public boolean isShowInfoOverlayWithHelplines() {
            return false;
        }
    };
    
    /**
     * Removes the polylines and the info overlay visualizing one maneuver from the map. Also removes the identifying
     * Triple<competitor.getIdAsString(), maneuver.getTimePoint(), maneuver.getType()> from the corresponding
     * {@link #maneuverLossLinesMap} and {@link #maneuverLossInfoOverlayMap}.
     */
    private void removeManeuverLossLinesAndInfoOverlayForManeuver(ManeuverDTO maneuver, String competitorIdAsString) {
        Triple<String, Date, ManeuverType> key = createManeuverKey(maneuver, competitorIdAsString);
        removeManeuverLossLinesAndInfoOverlayForManeuver(key);
    }

    private void removeManeuverLossLinesAndInfoOverlayForManeuver(Triple<String, Date, ManeuverType> key) {
        if (maneuverLossLinesMap.get(key) != null) {
            for (Polyline p : maneuverLossLinesMap.get(key)) {
                p.setMap((MapWidget) null);
            }
            maneuverLossLinesMap.remove(key);
        }
        final SmallTransparentInfoOverlay overlay;
        if ((overlay=maneuverLossInfoOverlayMap.remove(key)) != null) {
            overlay.removeFromMap();
        }
    }

    /**
     * Creates the ManeuverLoss Lines as calculated in {@link ManeuverDetectorImpl#getManeuverLossInMeters()} and
     * {@link SmallTransparentInfoOverlay}s attached to the {@link Polyline}s visualizing the maneuver. Stores them in
     * {@link #maneuverLossInfoOverlayMap} and {@link #maneuverLossLinesMap} with the identifier
     * Triple<competitor.getIdAsString(), maneuver.getTimePoint(), maneuver.getType()>.
     */
    private void createManeuverLossLinesAndInfoOverlays(ManeuverDTO maneuver, String competitorIdAsString) {
        final Set<Polyline> maneuverLossLines = new HashSet<>();
        Bearing bearingBefore = maneuver.getManeuverLoss().getSpeedWithBearingBefore().getBearing();
        Bearing middleManeuverAngle = new DegreeBearingImpl(maneuver.getManeuverLoss().getMiddleManeuverAngle());
        Distance extrapolationOfManeuverStartPoint = 
                maneuver.getManeuverLoss().getSpeedWithBearingBefore().travel(maneuver.getManeuverLoss().getManeuverDuration());
        Position extrapolatedManeuverStartPosition = maneuver.getManeuverLoss().getManeuverStartPosition()
                .translateRhumb(bearingBefore, extrapolationOfManeuverStartPoint);
        Position intersectionMiddleManeuverAngleWithExtrapolationOfManeuverStartPoint = maneuver.getManeuverLoss().getManeuverStartPosition()
                .getIntersection(bearingBefore, maneuver.getPosition(), middleManeuverAngle);
        Position projectedExtrapolatedManeuverStartPosition = extrapolatedManeuverStartPosition.projectToLineThrough(
                intersectionMiddleManeuverAngleWithExtrapolationOfManeuverStartPoint, middleManeuverAngle);
        Position projectedManeuverEndPosition = maneuver.getManeuverLoss().getManeuverEndPosition().projectToLineThrough(
                        intersectionMiddleManeuverAngleWithExtrapolationOfManeuverStartPoint, middleManeuverAngle);
        Position startOfBearingAtManeuverEndPositionLineOnMiddleManeuverAngleLine = maneuver.getManeuverLoss()
                .getManeuverEndPosition()
                .getIntersection(new DegreeBearingImpl(maneuver.getSpeedWithBearingAfter().bearingInDegrees),
                        intersectionMiddleManeuverAngleWithExtrapolationOfManeuverStartPoint, middleManeuverAngle);
        maneuverLossLines.add(raceMap.showOrRemoveOrUpdateLine(null, true,
                startOfBearingAtManeuverEndPositionLineOnMiddleManeuverAngleLine,
                maneuver.getManeuverLoss().getManeuverEndPosition(), bearingAtManeuverEndPositionLineInfoProvider,
                MANEUVERLOSSLINES_EXTRAPOLATEDLINES_COLOR, RaceMap.STANDARD_LINE_STROKEWEIGHT, LOWLIGHTED_LINE_OPACITY));
        String color = maneuver.getManeuverLoss().getDistanceLost().compareTo(Distance.NULL) > 0
                ? color = MANEUVERLOSSLINES_RED : MANEUVERLOSSLINES_GREEN;
        maneuverLossLines.add(raceMap.showOrRemoveOrUpdateLine(null, true,
                intersectionMiddleManeuverAngleWithExtrapolationOfManeuverStartPoint, projectedManeuverEndPosition,
                middleManeuverAngleLineInfoProvider, MANEUVERLOSSLINES_EXTRAPOLATEDLINES_COLOR,
                RaceMap.STANDARD_LINE_STROKEWEIGHT, LOWLIGHTED_LINE_OPACITY));
        maneuverLossLines.add(raceMap.showOrRemoveOrUpdateLine(null, true,
                maneuver.getManeuverLoss().getManeuverStartPosition(), extrapolatedManeuverStartPosition,
                extrapolatedLineInfoProvider, MANEUVERLOSSLINES_EXTRAPOLATEDLINES_COLOR, RaceMap.STANDARD_LINE_STROKEWEIGHT,
                LOWLIGHTED_LINE_OPACITY));
        maneuverLossLines.add(raceMap.showOrRemoveOrUpdateLine(null, true, extrapolatedManeuverStartPosition,
                projectedExtrapolatedManeuverStartPosition, projectedExtrapolatedLineInfoProvider,
                MANEUVERLOSSLINES_PROJECTEDLINES_COLOR, RaceMap.STANDARD_LINE_STROKEWEIGHT, RaceMap.LOWLIGHTED_TAIL_OPACITY));
        maneuverLossLines.add(raceMap.showOrRemoveOrUpdateLine(null, true, maneuver.getManeuverLoss().getManeuverEndPosition(),
                projectedManeuverEndPosition, projectedManeuverEndLineInfoProvider,
                MANEUVERLOSSLINES_PROJECTEDLINES_COLOR, RaceMap.STANDARD_LINE_STROKEWEIGHT, RaceMap.LOWLIGHTED_TAIL_OPACITY));
        maneuverLossLines.add(raceMap.showOrRemoveOrUpdateLine(null, true, projectedExtrapolatedManeuverStartPosition,
                projectedManeuverEndPosition, maneuverLossLineInfoProvider, color, HIGHLIGHTED_LINE_STROKEWEIGHT,
                RaceMap.STANDARD_LINE_OPACITY));
        final Triple<String, Date, ManeuverType> key = createManeuverKey(maneuver, competitorIdAsString);
        maneuverLossLinesMap.put(key, maneuverLossLines);
        StringBuilder sb = new StringBuilder();
        sb.append(stringMessages.maneuverLoss() + ": " + numberFormatOneDecimal.format(maneuver.getManeuverLoss().getDistanceLost().getMeters())
                + stringMessages.metersUnit() + '\n');
        if (maneuver.getType() == ManeuverType.TACK) {
            sb.append(stringMessages.tackAngle() + ": ");
        } else if (maneuver.getType() == ManeuverType.JIBE) {
            sb.append(stringMessages.jibeAngle() + ": ");
        } else {
            sb.append(stringMessages.maneuverAngle() + ": ");
        }
        sb.append(numberFormatOneDecimal.format(Math.abs(maneuver.getDirectionChangeInDegrees()))
                + stringMessages.degreesUnit());
        CssColor greyWithTransparency = CssColor.make("rgba(255,255,255,0.5)");
        SmallTransparentInfoOverlay maneuverLossInfoOverlay = new SmallTransparentInfoOverlay(raceMap.getMap(),
                RaceMapOverlaysZIndexes.INFO_OVERLAY_ZINDEX, sb.toString(), raceMap.getCoordinateSystem(), greyWithTransparency);
        maneuverLossInfoOverlay.setPosition(projectedManeuverEndPosition.translateGreatCircle(middleManeuverAngle,
                maneuver.getManeuverLoss().getDistanceLost().scale(0.5)), /* transition time */ -1);
        maneuverLossInfoOverlay.draw();
        maneuverLossInfoOverlayMap.put(key, maneuverLossInfoOverlay);
    }

    Widget getInfoWindowContent(ManeuverDTO maneuver, String competitorIdAsString) {
        SpeedWithBearingDTO before = maneuver.getSpeedWithBearingBefore();
        SpeedWithBearingDTO after = maneuver.getSpeedWithBearingAfter();
        VerticalPanel vPanel = new VerticalPanel();
        vPanel.add(raceMap.createInfoWindowLabelAndValue(stringMessages.maneuverType(),
                ManeuverTypeFormatter.format(maneuver.getType(), stringMessages)));
        vPanel.add(raceMap.createInfoWindowLabelAndValue(stringMessages.time(),
                DateTimeFormat.getFormat(PredefinedFormat.TIME_FULL).format(maneuver.getTimePoint())));
        vPanel.add(raceMap.createInfoWindowLabelAndValue(stringMessages.directionChange(),
                ((int) Math.round(maneuver.getDirectionChangeInDegrees())) + " " + stringMessages.degreesShort() + " ("
                        + ((int) Math.round(before.bearingInDegrees)) + " " + stringMessages.degreesShort() + " -> "
                        + ((int) Math.round(after.bearingInDegrees)) + " " + stringMessages.degreesShort() + ")"));
        vPanel.add(raceMap.createInfoWindowLabelAndValue(stringMessages.speedChange(),
                NumberFormat.getDecimalFormat().format(after.speedInKnots - before.speedInKnots) + " "
                        + stringMessages.knotsUnit() + " ("
                        + NumberFormat.getDecimalFormat().format(before.speedInKnots) + " " + stringMessages.knotsUnit()
                        + " -> " + NumberFormat.getDecimalFormat().format(after.speedInKnots) + " "
                        + stringMessages.knotsUnit() + ")"));
        vPanel.add(raceMap.createInfoWindowLabelAndValue(stringMessages.maxTurningRate(),
                NumberFormat.getDecimalFormat().format(maneuver.getMaxTurningRateInDegreesPerSecond()) + " "
                        + stringMessages.degreesPerSecondUnit()));
        vPanel.add(raceMap.createInfoWindowLabelAndValue(stringMessages.avgTurningRate(),
                NumberFormat.getDecimalFormat().format(maneuver.getAvgTurningRateInDegreesPerSecond()) + " "
                        + stringMessages.degreesPerSecondUnit()));
        if (maneuver.getType() != ManeuverType.BEAR_AWAY && maneuver.getType() != ManeuverType.HEAD_UP) {
            vPanel.add(raceMap.createInfoWindowLabelAndValue(stringMessages.lowestSpeed(),
                    NumberFormat.getDecimalFormat().format(maneuver.getLowestSpeedInKnots()) + " "
                            + stringMessages.knotsUnit()));
        }
        if (maneuver.getManeuverLoss() != null) {
            Widget maneuverLossWidget = raceMap.createInfoWindowLabelAndValue(stringMessages.maneuverLoss(),
                    numberFormatOneDecimal.format(maneuver.getManeuverLoss().getDistanceLost().getMeters()) + " " + stringMessages.metersUnit());
            CheckBox maneuverLossLinesCheckBox = new CheckBox(stringMessages.show());
            Triple<String, Date, ManeuverType> t = createManeuverKey(maneuver, competitorIdAsString);
            maneuverLossLinesCheckBox.setValue(maneuverLossLinesMap.containsKey(t));
            maneuverLossLinesCheckBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> event) {
                    if (event.getValue()) {
                        createManeuverLossLinesAndInfoOverlays(maneuver, competitorIdAsString);
                    } else {
                        removeManeuverLossLinesAndInfoOverlayForManeuver(maneuver, competitorIdAsString);
                    }
                }
            });
            HorizontalPanel hPanel = new HorizontalPanel();
            hPanel.add(maneuverLossWidget);
            hPanel.add(maneuverLossLinesCheckBox);
            vPanel.add(hPanel);
        }
        if (maneuver.getMarkPassingTimePoint() != null) {
            vPanel.add(
                    raceMap.createInfoWindowLabelAndValue(stringMessages.markPassing(),
                            DateTimeFormat.getFormat(PredefinedFormat.TIME_FULL).format(maneuver.getMarkPassingTimePoint())
                                    + (maneuver.getMarkPassingSide() == null ? ""
                                            : " (" + NauticalSideFormatter.format(maneuver.getMarkPassingSide(), stringMessages)
                                                    + ")")));
        }
        return vPanel;
    }

    public void updateManeuverMarkersAfterSettingsChanged() {
        // TODO is it really necessary to remove them all first...?
        if (lastManeuverResult != null) {
            removeAllManeuverMarkers();
            showManeuvers(lastManeuverResult);
        }
    }

    /**
     * Removes all maneuver displays from the map and also clears the structure holding the maneuvers
     * last received from the server so that upon the next request to show maneuvers without a previous
     * update from the server no maneuvers will be displayed.<p>
     * 
     * @see #getAndShowManeuvers(RegattaAndRaceIdentifier, Map)
     * @see #removeAllManeuverMarkers()
     */
    void clearAllManeuverMarkers() {
        removeAllManeuverMarkers();
        lastManeuverResult = null;
    }
}
