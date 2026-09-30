package com.sap.sailing.gwt.ui.shared.racemap;

import com.google.gwt.canvas.dom.client.CanvasGradient;
import com.google.gwt.canvas.dom.client.Context2d;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.gwt.ui.shared.CoursePositionsDTO;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Color;
import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.MeterDistance;

/**
 * A class for course mark graphics based on SVG graphics drawn to a HTML5 canvas
 * The SVG files for the drawing can be found in the package com.sap.sailing.gwt.ui.svg.buoys
 * A general description how to convert SVG files to 'drawing commands' can be found at http://wiki.sapsailing.com/wiki/howto/development/boatgraphicssvg
 * @author Frank
 *
 */
public abstract class AbstractMarkVectorGraphics implements MarkVectorGraphics {
    protected Distance markHeightInMeters;
    protected Distance markWidthInMeters;
    
    protected final String color;
    protected final String shape;
    protected final String pattern;
    protected final MarkType type;
    
    protected static final double doublePi = 2 * Math.PI;

    protected double anchorPointX = 0.44;
    protected double anchorPointY = 1.67;
    
    protected final static String DEFAULT_MARK_COLOR = "#f9ac00";
    protected final static String DEFAULT_MARK_BG_COLOR = "#f0f0f0";

    public AbstractMarkVectorGraphics(MarkType type, Color color, String shape, String pattern) {
        this.type = type;
        this.color = color == null ? null : color.getAsHtml();
        this.shape = shape;
        this.pattern = pattern;
        this.markHeightInMeters = new MeterDistance(2.1);
        this.markWidthInMeters = new MeterDistance(1.5);
    }
    
    public void drawMarkToCanvas(Context2d ctx, boolean isSelected, double width, double height, double scaleFactor) {
        ctx.save();
        ctx.clearRect(0,  0,  width, height);
        ctx.translate(width / 2.0, height / 2.0);
        ctx.scale(scaleFactor, scaleFactor);
        ctx.translate(-anchorPointX * 100,- anchorPointY * 100);
        String markColor = color != null ? color : DEFAULT_MARK_COLOR; 
        drawMark(ctx, isSelected, markColor);
        ctx.restore();
    }
    
    public Distance getMarkHeight() {
        return markHeightInMeters;
    }

    public Distance getMarkWidth() {
        return markWidthInMeters;
    }

    protected void drawMark(Context2d ctx, boolean isSelected, String color) {
        if (isSelected) {
            drawMarkSelection(ctx);
        }
        this.drawMarkBody(ctx, isSelected, color);
    }
	
    protected void drawMarkSelection(Context2d ctx) {
        ctx.save();

        this.setUpScaleAndTranslateForMarkSelection(ctx);

        CanvasGradient g1 = ctx.createLinearGradient(77.8, 188, 165, 219);
        g1.addColorStop(0, "rgba(240, 240, 240, 1)");
        g1.addColorStop(1, "rgba(240, 240, 240, 0)");
        ctx.setFillStyle(g1);
        ctx.beginPath();
        ctx.moveTo(170, 181);
        ctx.translate(44.000168893718424, 180.79369636814624);
        ctx.rotate(0);
        ctx.scale(1, 1);
        ctx.arc(0, 0, 126, 0.0016373311430805408, 0.5254904360229328, false);
        ctx.scale(1, 1);
        ctx.rotate(0);
        ctx.translate(-44.000168893718424, -180.79369636814624);
        ctx.lineTo(44.2, 181);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();

        CanvasGradient g2 = ctx.createLinearGradient(51, 203, 77.3, 302);
        g2.addColorStop(0, "rgba(255, 255, 255, 1)");
        g2.addColorStop(1, "rgba(255, 255, 255, 0)");
        ctx.setFillStyle(g2);
        ctx.beginPath();
        ctx.moveTo(107, 290);
        ctx.translate(43.79222318749837, 181.0010231680087);
        ctx.rotate(0);
        ctx.scale(1, 1);
        ctx.arc(0, 0, 126, 1.0452923752792398, 1.5667663411841573, false);
        ctx.scale(1, 1);
        ctx.rotate(0);
        ctx.translate(-43.79222318749837, -181.0010231680087);
        ctx.lineTo(44.2, 181);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();

        CanvasGradient g3 = ctx.createLinearGradient(26.8, 197, -46.5, 269);
        g3.addColorStop(0, "rgba(255, 255, 255, 1)");
        g3.addColorStop(1, "rgba(255, 255, 255, 0)");
        ctx.setFillStyle(g3);
        ctx.beginPath();
        ctx.moveTo(-18.3, 290);
        ctx.translate(43.72636289026038, 180.32443158749652);
        ctx.rotate(0);
        ctx.scale(1, 1);
        ctx.arc(0, 0, 126, 2.0854951576078884, 2.611791628699119, false);
        ctx.scale(1, 1);
        ctx.rotate(0);
        ctx.translate(-43.72636289026038, -180.32443158749652);
        ctx.lineTo(44.2, 181);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();

        CanvasGradient g4 = ctx.createLinearGradient(18.7, 172, -76.3, 140);
        g4.addColorStop(0, "rgba(255, 255, 255, 1)");
        g4.addColorStop(1, "rgba(255, 255, 255, 0)");
        ctx.setFillStyle(g4);
        ctx.beginPath();
        ctx.moveTo(-81.8, 179);
        ctx.translate(44.19920011832349, 178.55103503179944);
        ctx.rotate(0);
        ctx.scale(1, 1);
        ctx.arc(0, 0, 126, 3.1380294320163484, 3.670248678643417, false);
        ctx.scale(1, 1);
        ctx.rotate(0);
        ctx.translate(-44.19920011832349, -178.55103503179944);
        ctx.lineTo(44.2, 179);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();

        CanvasGradient g5 = ctx.createLinearGradient(37.9, 147, 13.6, 58.6);
        g5.addColorStop(0, "rgba(255, 255, 255, 1)");
        g5.addColorStop(1, "rgba(255, 255, 255, 0)");
        ctx.setFillStyle(g5);
        ctx.beginPath();
        ctx.moveTo(-18.7, 71.8);
        ctx.translate(44.160311222571984, 180.99973110315514);
        ctx.rotate(0);
        ctx.scale(1, 1);
        ctx.arc(0, 0, 126, -2.0931154263055687, -1.5728622903484606, false);
        ctx.scale(1, 1);
        ctx.rotate(0);
        ctx.translate(-44.160311222571984, -180.99973110315514);
        ctx.lineTo(44.199999999999996, 181);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();

        CanvasGradient g6 = ctx.createLinearGradient(80.8, 139, 134, 91.9);
        g6.addColorStop(0, "rgba(240, 240, 240, 1)");
        g6.addColorStop(1, "rgba(240, 240, 240, 0)");
        ctx.setFillStyle(g6);
        ctx.beginPath();
        ctx.moveTo(108, 72.1);
        ctx.translate(43.50304481630927, 180.3411325330301);
        ctx.rotate(0);
        ctx.scale(1, 1);
        ctx.arc(0, 0, 126, -1.0334238189446499, -0.5175711746742696, false);
        ctx.scale(1, 1);
        ctx.rotate(0);
        ctx.translate(-43.50304481630927, -180.3411325330301);
        ctx.lineTo(44.2, 181);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();

        ctx.translate(-49.5, 17.2);
        ctx.restore();
    }

    protected void setUpScaleAndTranslateForMarkSelection(Context2d ctx) {
    	ctx.scale(1.2,1.2);
        ctx.translate(-10,-30);
    }
    
    protected abstract void drawMarkBody(Context2d  ctx, boolean isSelected, String color);

    @Override
    public Bearing getRotationInDegrees(CoursePositionsDTO coursePositionsDTO) {
        // by default, no rotation for marks
        return null;
    }
}
