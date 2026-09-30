package com.sap.sailing.dashboards.gwt.client.widgets.windbot.compass;

import java.util.Iterator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.Position;

/**
 * @author Alexander Ries (D062114)
 *
 */
public class LocationPointerCompass extends Composite implements HasWidgets, LocationPointerCompassAngleDistanceListener {

    private static LocationPointerCompasssUiBinder uiBinder = GWT.create(LocationPointerCompasssUiBinder.class);

    interface LocationPointerCompasssUiBinder extends UiBinder<Widget, LocationPointerCompass> {
    }
    
    @UiField
    Image compassNeedle;

    /**
     * The Label shows the distance the users device is away from the location it points to.
     * */
    @UiField
    DivElement distanceToPointetLocation;

    /**
     * The Label shows the direction in degrees from north the users device is away from the location it points to.
     * */
    @UiField
    DivElement angleToPointetLocation;

    private double angleOffset;
    /**
     * The {@link LocationPointerCompassAngleDistance} notifies the class about updates about angle and distance
     * changes.
     * */
    private LocationPointerCompassAngleDistance locationPointerCompassAngleDistance;

    public LocationPointerCompass() {
        LocationPointerCompassRessources.INSTANCE.gss().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        compassNeedle.setResource(LocationPointerCompassRessources.INSTANCE.compass());
        locationPointerCompassAngleDistance = new LocationPointerCompassAngleDistance();
        locationPointerCompassAngleDistance.addListener(this);
    }

    private void updateNeedleAngle(double newDirection) {
        // Adapt degrees to image angle
        synchronized (this) {
            double newAngle = (newDirection-angleOffset)%360;
            String winddirectionformatted = NumberFormat.getFormat("#0").format(newAngle);
            compassNeedle.getElement().getStyle().setProperty("transform", "rotate(" + winddirectionformatted + "deg)");
            compassNeedle.getElement().getStyle()
                    .setProperty("webkitTransform", "rotate(" + winddirectionformatted + "deg)");
            String winddirectionFormattedForLabel = NumberFormat.getFormat("#0").format((newAngle < 0) ? 360+newAngle : newAngle);
            angleToPointetLocation.setInnerHTML(winddirectionFormattedForLabel+"°");
        }
    }

    private void updateDistanceToPointedLocationLabel(double distance, double angle) {
        String distanceFormatted = NumberFormat.getFormat("#0").format(distance);
        distanceToPointetLocation.setInnerHTML(distanceFormatted + " m");
    }

    public void windBotPositionChanged(Position positionDTO) {
        locationPointerCompassAngleDistance.windBotPositionChanged(positionDTO);
    }
    
    @Override
    public void angleChanged(double angle) {
        updateNeedleAngle(angle);
    }

    @Override
    public void angleAndDistanceChanged(double angle, double distance) {
        updateNeedleAngle(angle);
        updateDistanceToPointedLocationLabel(distance, angle);
    }

    @Override
    public void setAngleOffset(double offset) {
        angleOffset = offset;
    }
    
    @Override
    public void add(Widget w) {
        throw new UnsupportedOperationException("The method add(Widget w) is not supported.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("The method clear() is not supported.");
    }

    @Override
    public Iterator<Widget> iterator() {
        return null;
    }

    @Override
    public boolean remove(Widget w) {
        return false;
    }
}
