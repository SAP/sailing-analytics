package com.sap.sailing.gwt.ui.simulator;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.sap.sailing.gwt.ui.shared.WindFieldDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldGenParamsDTO;
import com.sap.sailing.gwt.ui.simulator.racemap.FullCanvasOverlay;
import com.sap.sailing.gwt.ui.simulator.streamlets.RectField;
import com.sap.sailing.gwt.ui.simulator.streamlets.SimulatorField;
import com.sap.sailing.gwt.ui.simulator.streamlets.SimulatorJSBundle;
import com.sap.sailing.gwt.ui.simulator.streamlets.Swarm;
import com.sap.sse.gwt.client.player.Timer;

/**
 * A Google Maps overlay based on an HTML5 canvas for drawing a wind field. The overlay covers the whole map and displays
 * the wind objects inside it.
 * 
 * @author Nidhi Sawhney (D054070)
 * 
 */
public class WindStreamletsCanvasOverlay extends FullCanvasOverlay implements TimeListenerWithStoppingCriteria {
    /** The wind field that is to be displayed in the overlay */
    private WindFieldDTO windFieldDTO;
    private final WindFieldGenParamsDTO windParams;
    private final SimulatorMap simulatorMap;
    private StreamletParameters streamletPars;

    private boolean visible = false;
    private final Timer timer;
    private Date endDate;

    private boolean macroWeather;
    private Swarm swarm;

    public WindStreamletsCanvasOverlay(SimulatorMap simulatorMap, int zIndex, final Timer timer, final StreamletParameters streamletPars,
            final WindFieldGenParamsDTO windParams) {
        super(simulatorMap.getMap(), zIndex);
        this.simulatorMap = simulatorMap;
        this.timer = timer;
        this.windParams = windParams;
        this.streamletPars = streamletPars;
        this.macroWeather = this.simulatorMap.getMainPanel().macroWeather;
        windFieldDTO = null;
        getCanvas().getElement().setId("swarm-display");
    }

    public WindFieldDTO getWindFieldDTO() {
        return this.windFieldDTO;
    }

    public WindFieldGenParamsDTO getWindParams() {
        return this.windParams;
    }


    public void startStreamlets() {
        if (swarm == null) {
            final SimulatorField field = new SimulatorField(getWindFieldDTO(), getWindParams(), streamletPars);
            setCanvasSettings();
            this.swarm = new Swarm(this, map, timer, field, streamletPars);
        }
        this.swarm.start(40);
    }

    public void stopStreamlets() {
        if (swarm != null) {
            this.swarm.stop();
        }
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setWindField(final WindFieldDTO windFieldDTO) {
        this.windFieldDTO = windFieldDTO; 
        this.swarm = null;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public void setVisible(boolean isVisible) {
        if (getCanvas() != null) {
            if (isVisible) {
                this.startStreamlets();
                if (timer.getTime().compareTo(this.windParams.getStartTime()) != 0) {
                    this.timeChanged(timer.getTime(), null);
                }
                this.visible = isVisible;
            } else {
                this.stopStreamlets();
                this.visible = isVisible;
            }
        }
    }

    @Override
    public void addToMap() {
        if (timer != null) {
            timer.addTimeListener(this);
        }
    }

    @Override
    public void removeFromMap() {
        if (timer != null) {
            timer.removeTimeListener(this);
        }
        this.setVisible(false);
    }

    @Override
    protected void drawCenterChanged() {
    }

    @Override
    protected void draw() {
        super.draw();
        if (mapProjection != null) {
            if ((macroWeather) && (swarm == null)) {
                macroWeather = false;
                SimulatorJSBundle bundle = GWT.create(SimulatorJSBundle.class);
                String jsonStr = bundle.windStreamletsDataJS().getText();
                RectField f = RectField.read(jsonStr.substring(19, jsonStr.length() - 1), false, streamletPars);
                map.setZoom(5);
                map.panTo(f.getCenter());
                this.swarm = new Swarm(this, map, timer, f, streamletPars);
                this.swarm.start(/* animationIntervalMillis */ 40);
            }
        }
    }

    public Swarm getSwarm() {
        return this.swarm;
    }

    @Override
    public void timeChanged(final Date newDate, Date oldDate) {
    }

    @Override
    public boolean shallStop() {
        if (timer.getTime().getTime() >= this.endDate.getTime()) {
            return true;
        } else {
            return false;
        }
    }
}
