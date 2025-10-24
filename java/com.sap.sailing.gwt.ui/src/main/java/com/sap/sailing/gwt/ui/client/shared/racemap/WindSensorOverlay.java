package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.Point;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.WindSourceTypeFormatter;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindTrackInfoDTO;
import com.sap.sailing.gwt.ui.shared.racemap.CanvasOverlayV3;

/**
 * A google map overlay based on a HTML5 canvas for drawing a wind sensor (as an rotating arrow)
 * The wind sensor symbol will be rotated according to the wind data.
 */
public class WindSensorOverlay extends CanvasOverlayV3 {
    /**
     * The current wind track used to draw the wind sensor.
     */
    private WindTrackInfoDTO windTrackInfoDTO;

    /**
     * The current wind source used to draw the wind sensor.
     */
    private WindSource windSource;

    private final ImageTransformer transformer;

    private final StringMessages stringMessages;
    private int canvasWidth;
    private int canvasHeight;

    private final NumberFormat numberFormat = NumberFormat.getFormat("0.0");
    
    public WindSensorOverlay(MapWidget map, int zIndex, RaceMapImageManager raceMapImageManager, StringMessages stringMessages, CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);
        this.stringMessages = stringMessages;
        canvasWidth = 28;
        canvasHeight = 28;

        if(getCanvas() != null) {
            getCanvas().setWidth(String.valueOf(canvasWidth));
            getCanvas().setHeight(String.valueOf(canvasHeight));
            getCanvas().setCoordinateSpaceWidth(canvasWidth);
            getCanvas().setCoordinateSpaceHeight(canvasHeight);
        }
        transformer = raceMapImageManager.getWindSensorIconTransformer();
    }
    
    @Override
    protected void draw() {
        boolean hasValidWind = false;
        if (getMapProjection() != null && windTrackInfoDTO != null && windTrackInfoDTO.windFixes.size() > 0) {
            WindDTO windDTO = windTrackInfoDTO.windFixes.get(0);
            Position position = windDTO.position;
            // Attention: sometimes there is no valid position for the wind source available -> ignore the wind in this case
            if (position != null) {
                double rotationDegOfWindSymbol = windDTO.dampenedTrueWindBearingDeg;
                transformer.drawToCanvas(getCanvas(), coordinateSystem.mapDegreeBearing(rotationDegOfWindSymbol), 1.0,
                        /* transparency based on confidence; down to 0.01 (1%) we draw fully opaque (1.0); below that we'll fade out */
                        windDTO.confidence == null ? null : Math.min(1.0, 1000*windDTO.confidence));
                setLatLngPosition(coordinateSystem.toLatLng(windDTO.position));
                Point sensorPositionInPx = getMapProjection().fromLatLngToDivPixel(getLatLngPosition());
                setCanvasPosition(sensorPositionInPx.getX() - canvasWidth / 2, sensorPositionInPx.getY() - canvasHeight / 2);
                String title = stringMessages.wind() + " ("+ WindSourceTypeFormatter.format(windSource, stringMessages) + "): "; 
                title += Math.round(windDTO.dampenedTrueWindFromDeg) + " " + stringMessages.degreesShort()+ ",  ";
                title += numberFormat.format(windDTO.dampenedTrueWindSpeedInKnots) + " " + stringMessages.knotsUnit();
                getCanvas().setTitle(title);
                hasValidWind = true;
            }
        }
        if (!hasValidWind) {
            setLatLngPosition(null);
        }
        getCanvas().setVisible(hasValidWind);
    }

    public WindTrackInfoDTO getWindTrackInfoDTO() {
        return windTrackInfoDTO;
    }

    public void setWindInfo(WindTrackInfoDTO windTrackInfoDTO, WindSource windSource) {
        this.windTrackInfoDTO = windTrackInfoDTO;
        this.windSource = windSource;
    }

    public WindSource getWindSource() {
        return windSource;
    }
}
