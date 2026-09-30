package com.sap.sailing.gwt.ui.client;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.LineCap;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.ui.Composite;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

/**
 * WindIndicator Widget allows visualizing the wind direction and other wind related information.
 */
public class WindIndicator extends Composite {

    private static final int CIRCLE_RADIUS_RATIO = 12;
    private static final double STROKE_LENGTH_RATIO = 2.2;
    private static final double RATIO_OF_STROKE_TO_USE_FOR_TICKS = 0.7;
    private static final double TICK_LENGTH_RATIO = 15;
    private static final String RGB_WHITE = "rgb(255,255,255)";
    private static final String RGB_BLACK = "rgb(0,0,0)";
    private static final int MAX_NUMBER_OF_TICKS = 6;

    private Canvas canvas;
    
    private SpeedWithBearing windFrom = new KnotSpeedWithBearingImpl(0, new DegreeBearingImpl(0));
    
    /**
     * cloud coverage from 0.0 to 1.0
     */
    private double cloudCoverage = 0.0;

    /**
     * Create a new WindIndiator
     */
    public WindIndicator() {
        canvas = Canvas.createIfSupported();
        this.initWidget(canvas);
        this.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if (event.isAttached()) {
                    WindIndicator.this.updateRendering();
                }
            }
        });
        setSize("75px", "75px");
    }

    /**
     * Set the direction from where the wind blows
     * 
     * @param fromDeg
     */
    public void setWindFrom(SpeedWithBearing windFrom) {
        this.windFrom = windFrom;
        this.updateRendering();
    }

    /**
     * Get the currently set direction from where the wind blows
     * 
     * @return
     */
    public double getFromDeg() {
        return windFrom.getBearing().getDegrees();
    }

    /**
     * Set the cloud coverage from 0.0 to 1.0
     * 
     * @param cloudCoverage
     */
    public void setCloudCoverage(double cloudCoverage) {
        this.cloudCoverage = cloudCoverage;
        this.updateRendering();
    }

    /**
     * get the currently set cloud coverage
     * 
     * @return
     */
    public double getCloudCoverage() {
        return cloudCoverage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.google.gwt.user.client.ui.UIObject#setSize(java.lang.String, java.lang.String)
     */
    @Override
    public void setSize(String width, String height) {
        super.setSize(width, height);
        canvas.setSize(width, height);
    }

    /**
     * Updates the rendering
     */
    private void updateRendering() {

        int minSize = Math.min(canvas.getCoordinateSpaceWidth(), canvas.getCoordinateSpaceHeight());
        canvas.setCoordinateSpaceWidth(minSize);
        canvas.setCoordinateSpaceHeight(minSize);

        Context2d ctx = canvas.getContext2d();

        // draw the line
        ctx.setLineWidth(2);
        ctx.setLineCap(LineCap.SQUARE);
        ctx.beginPath();
        ctx.moveTo(minSize / 2, minSize / 2);
        double dirRad = windFrom.getBearing().getRadians();
        double strokeTipX = minSize/2 + (((double) minSize) / STROKE_LENGTH_RATIO)*Math.cos(dirRad);
        double strokeTipY = minSize/2 + (((double) minSize) / STROKE_LENGTH_RATIO)*Math.sin(dirRad);
        ctx.lineTo(strokeTipX, strokeTipY);
        ctx.stroke();
        for (int i=0; i<MAX_NUMBER_OF_TICKS; i++) {
            drawTick(ctx, i, dirRad);
        }
        // move back to center
        ctx.moveTo(minSize / 2, minSize / 2);
        // draw the circle
        ctx.setFillStyle(RGB_WHITE);
        ctx.setStrokeStyle(RGB_BLACK);
        // ctx.beginPath();
        ctx.arc(minSize / 2, minSize / 2, minSize / CIRCLE_RADIUS_RATIO, 0, 2 * Math.PI, true);
        ctx.stroke();
        ctx.fill();
        // draw the cloud coverage
        ctx.setFillStyle(RGB_BLACK);
        ctx.beginPath();
        ctx.moveTo(minSize / 2, minSize / 2);
        double cc = Math.max(0.0, Math.min(1.0, cloudCoverage));
        ctx.arc(minSize / 2, minSize / 2, minSize / CIRCLE_RADIUS_RATIO, -0.5 * Math.PI, (-0.5 + cc * 2) * Math.PI, false);
        ctx.fill();

        setTitle();
    }

    private void setTitle() {
        int speedInKnotsTimes10 = (int) (windFrom.getKnots()*10);
        int forceTimes10 = (int) (windFrom.getBeaufort()*10);
        int intFromDeg = (int) getFromDeg();
        setTitle(""+speedInKnotsTimes10/10+"."+speedInKnotsTimes10%10+"kts ("+
                forceTimes10/10+"."+forceTimes10%10+"bft) from "+(intFromDeg<10?"00":intFromDeg<100?"0":"")+intFromDeg+" deg");
    }

    private void drawTick(Context2d ctx, int i, double dirRad) {
        if (hasTick(i)) {
            double[] tickStart = getTickStart(i, dirRad);
            ctx.moveTo(tickStart[0], tickStart[1]);
            double[] tickOffset = getTickOffset(i, dirRad);
            ctx.lineTo(tickStart[0]+tickOffset[0], tickStart[1]+tickOffset[1]);
            ctx.stroke();
        }
    }

    private double[] getTickOffset(int i, double dirRad) {
        double dirRadPlus90Deg = dirRad + Math.PI/2.;
        int minSize = Math.min(canvas.getCoordinateSpaceWidth(), canvas.getCoordinateSpaceHeight());
        double singleLength = minSize / TICK_LENGTH_RATIO;
        int zeroOneOrTwo = getTickLength(i);
        double length = singleLength * zeroOneOrTwo;
        return new double[] { length*Math.cos(dirRadPlus90Deg), length*Math.sin(dirRadPlus90Deg) };
    }

    private int getTickLength(int i) {
        int bft = (int) (windFrom.getBeaufort()+0.5); // round
        int zeroOneOrTwo = (bft == 1 ? (i == 1 ? 1 : 0) : 2*(i+1)<=bft ? 2 : 2*i+1 == bft ? 1 : 0);
        return zeroOneOrTwo;
    }

    private double[] getTickStart(int i, double dirRad) {
        double minSize = (double) Math.min(canvas.getCoordinateSpaceWidth(), canvas.getCoordinateSpaceHeight());
        double radius = minSize / STROKE_LENGTH_RATIO - i * minSize / 
                STROKE_LENGTH_RATIO*RATIO_OF_STROKE_TO_USE_FOR_TICKS/MAX_NUMBER_OF_TICKS;
        double tickStartX = minSize/2 + radius*Math.cos(dirRad);
        double tickStartY = minSize/2 + radius*Math.sin(dirRad);
        return new double[] { tickStartX, tickStartY };
    }

    private boolean hasTick(int i) {
        return getTickLength(i) > 0;
    }
}
