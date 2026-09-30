package com.sap.sailing.gwt.ui.shared.racemap;

import com.google.gwt.canvas.dom.client.Context2d;
import com.sap.sailing.domain.common.FixType;
import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.MeterDistance;

/**
 * A class for course fix graphics based on SVG graphics drawn to a HTML5 canvas
 * The SVG files for the drawing can be found in the package com.sap.sailing.gwt.ui.svg.fixes
 * A general description how to convert SVG files to 'drawing commands' can be found at http://wiki.sapsailing.com/wiki/howto/development/boatgraphicssvg
 * @author Jonas Dann
 */

public class FixVectorGraphics {
    protected Distance fixHeight;
    protected Distance fixWidth;
    
    private final String color;
    private final FixType type;
    
    private static final double doublePi = 2 * Math.PI;

    private double anchorPointX = 0.44;
    private double anchorPointY = 1.67;
    
    private final static String DEFAULT_FIX_COLOR = "#f9ac00";
    private final static String DEFAULT_FIX_BG_COLOR = "#f0f0f0";
    
    public FixVectorGraphics(FixType type, String color) {
        this.type = type;
        this.color = color;
        this.fixHeight = new MeterDistance(2.1);
        this.fixWidth = new MeterDistance(1.5);
    }
    
    public void drawMarkToCanvas(Context2d ctx, double width, double height, double scaleFactor) {
        ctx.save();
        ctx.clearRect(0,  0,  width, height);
        ctx.translate(width / 2.0, height / 2.0);
        ctx.scale(scaleFactor, scaleFactor);
        ctx.translate(-anchorPointX * 100,- anchorPointY * 100);
        String markColor = color != null ? color : DEFAULT_FIX_COLOR; 
        drawFix(ctx, markColor);
        ctx.restore();
    }

    protected void drawFix(Context2d ctx, String color) {
        switch(type) {
            case BUOY:
                drawBuoyFix(ctx, color);
                break;
            default:
                break;
        }
    }
    
    protected void drawBuoyFix(Context2d ctx, String color) {
        ctx.setStrokeStyle("rgba(0,0,0,0)");
        
        ctx.save();
        ctx.beginPath();
        ctx.moveTo(0,0);
        ctx.lineTo(150,0);
        ctx.lineTo(150,300);
        ctx.lineTo(0,300);
        ctx.closePath();
        ctx.save();
        ctx.restore();
        
        ctx.save();
        ctx.restore();
        
        ctx.save();
        ctx.setFillStyle(color);
        ctx.transform(12.5,0,0,12.5,-578,22.2);
        ctx.beginPath();
        ctx.arc(49.7,12.8,2.66,0,doublePi,true);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();
        ctx.restore();
        
        ctx.save();
        ctx.setFillStyle(DEFAULT_FIX_BG_COLOR);
        ctx.transform(12.5,0,0,12.5,-578,22.2);
        ctx.beginPath();
        ctx.arc(49.7,11.3,3.37,0,doublePi,true);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();
        ctx.restore();
        
        ctx.save();
        ctx.setFillStyle(color);
        ctx.transform(12.5,0,0,12.5,-578,22.2);
        ctx.beginPath();
        ctx.arc(49.6,10.6,2.66,0,doublePi,true);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();
        ctx.restore();
        ctx.restore();
    }
    
    public Distance getFixHeight() {
        return fixHeight;
    }

    public Distance getFixWidth() {
        return fixWidth;
    }

    public FixType getType() {
        return type;
    }
    
    public String getColor() {
        return color;
    }
}
