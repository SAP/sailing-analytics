package com.sap.sailing.gwt.ui.simulator.streamlets;

import java.util.Date;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.user.client.Timer;
import com.sap.sailing.domain.common.Bounds;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.BoundsImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.gwt.ui.simulator.StreamletParameters;
import com.sap.sailing.gwt.ui.simulator.racemap.FullCanvasOverlay;
import com.sap.sse.gwt.client.player.TimeListener;

public class Swarm implements TimeListener {
    private final FullCanvasOverlay fullcanvas;
    private final Canvas canvas;
    private final MapWidget map;
    private StreamletParameters parameters; 

    private Timer loopTimer;
    private Mercator projection;
    private final VectorField field;
    
    private boolean zoomChanged = false;
    private Vector diffPx;
    
    /**
     * The number of particles to show. After {@link #updateBounds()} has been run, this also reflects the size of the
     * {@link #particles} array. Note that since elements in {@link #particles} can be <code>null</code>, this number
     * not necessarily represents the exact number of particles visible.
     */
    private int nParticles;
    
    /**
     * The particles shown in this swarm. Elements in the array may be <code>null</code>.
     */
    private Particle[] particles;
    
    /**
     * Tells if nothing of this swarm is currently visible on the {@link #map}. This is the case when there is no
     * intersection between the {@link #field vector field's} {@link VectorField#getFieldCorners() bounds} and the
     * visible map area.
     */
    private boolean swarmOffScreen = false;
    
    private int swarmPause = 0;
    private boolean swarmContinue = true;
    private Bounds visibleBoundsOfField;
    private Date timePoint;
    private double cosineOfAverageLatitude;

    public Swarm(FullCanvasOverlay fullcanvas, MapWidget map, com.sap.sse.gwt.client.player.Timer timer,
            VectorField vectorField, StreamletParameters streamletPars) {
        this.field = vectorField;
        this.fullcanvas = fullcanvas;
        this.canvas = fullcanvas.getCanvas();
        this.map = map;
        timer.addTimeListener(this);
        this.parameters = streamletPars;
        timePoint = timer.getTime();
        cosineOfAverageLatitude = 1.0; // default to equator
        diffPx = new Vector(0, 0);
    }

    public void start(int animationIntervalMillis) {
        fullcanvas.setCanvasSettings();
        projection = new Mercator(fullcanvas, map);
        projection.calibrate();
        updateBounds();
        particles = this.createParticles();
        swarmContinue = true;
        startLoop(animationIntervalMillis);
    }

    public void stop() {
        swarmContinue = false;
    }

    private Particle createParticle() {
        Particle particle = null;
        boolean done = false;
        int attempts = 10;
        // try a few times to create a particle at a random position until the weight is high enough for it to be displayed
        while (!done && attempts-- > 0) {
            particle = new Particle();
            particle.currentPosition = getRandomPosition();
            if (field.inBounds(particle.currentPosition)) {
                Vector v = field.getVector(particle.currentPosition, timePoint);
                double weight = field.getParticleWeight(particle.currentPosition, v);
                if (weight >= Math.random()) {
                    if (v == null || v.length() == 0) {
                        particle.stepsToLive = 0;
                    } else {
                        particle.stepsToLive = 1 + (int) Math.round(Math.random() * 40);
                    }
                    particle.currentPixelCoordinate = projection.latlng2pixel(particle.currentPosition);
                    particle.previousPixelCoordinate = particle.currentPixelCoordinate;
                    particle.v = v;
                    done = true;
                } else {
                    particle = null;
                }
            } else {
                particle = null; // out of bounds
            }
        }
        return particle;
    }
    
    private Position getRandomPosition() {
        final Position result;
        double rndY = Math.random();
        double rndX = Math.random();
        double latDeg = rndY * this.visibleBoundsOfField.getSouthWest().getLatDeg() + (1 - rndY) * this.visibleBoundsOfField.getNorthEast().getLatDeg();
        double lngDeg = rndX * this.visibleBoundsOfField.getSouthWest().getLngDeg() + (1 - rndX) * this.visibleBoundsOfField.getNorthEast().getLngDeg();
        result = new DegreePosition(latDeg, lngDeg);
        return result;
    }

    private Particle[] createParticles() {
        Particle[] newParticles = new Particle[nParticles];
        for (int idx = 0; idx < newParticles.length; idx++) {
            newParticles[idx] = this.createParticle();
        }
        return newParticles;
    }

    public void onBoundsChanged(boolean zoomChanged, int swarmPause) {
        this.zoomChanged |= zoomChanged;
        if (this.zoomChanged) {
            projection.clearCanvas();
        }
        this.swarmPause = swarmPause;
    }

    private void updateBounds() {
        Bounds fieldBounds = this.field.getFieldCorners();
        final BoundsImpl mapBounds = new BoundsImpl(
                new DegreePosition(map.getBounds().getSouthWest().getLatitude(), map.getBounds().getSouthWest().getLongitude()),
                new DegreePosition(map.getBounds().getNorthEast().getLatitude(), map.getBounds().getNorthEast().getLongitude()));
        swarmOffScreen = !fieldBounds.intersects(mapBounds);
        visibleBoundsOfField = fieldBounds.intersect(mapBounds);
        Vector boundsSWpx = this.projection.latlng2pixel(visibleBoundsOfField.getSouthWest());
        Vector boundsNEpx = this.projection.latlng2pixel(visibleBoundsOfField.getNorthEast());
        double boundsWidthpx = Math.abs(boundsNEpx.x - boundsSWpx.x);
        double boundsHeightpx = Math.abs(boundsSWpx.y - boundsNEpx.y);
        this.nParticles = (int)Math.round(Math.sqrt(boundsWidthpx * boundsHeightpx) * this.field.getParticleFactor() * this.parameters.swarmScale);
        cosineOfAverageLatitude = Math.cos((visibleBoundsOfField.getSouthWest().getLatRad()+visibleBoundsOfField.getNorthEast().getLatRad())/2);
    };

    //native void log(String message) /*-{
    //    console.log( message );
    //}-*/;
    
    private void startLoop(final int animationIntervalMillis) {
        // Create animation-loop based on timer timeout
        loopTimer = new com.google.gwt.user.client.Timer() {
            public void run() {
                Date time0 = new Date();
                if (swarmPause > 1) {
                    swarmPause--;
                } else if (swarmPause == 1) {
                    fullcanvas.setCanvasSettings();
                    projection.calibrate();
                    updateBounds();
                    if (zoomChanged) {
                        diffPx = new Vector(0,0);
                        particles = createParticles();
                        zoomChanged = false;
                    } else {
                        diffPx = fullcanvas.getDiffPx();
                    }
                    swarmPause = 0;
                }
                if ((!swarmOffScreen) && (swarmPause == 0)) {
                    execute(diffPx);
                    diffPx = new Vector(0, 0);
                }
                Date time1 = new Date();
                if (swarmContinue) {
                    // wait at least 10ms for the next iteration; try to get one iteration done every animationIntervalMillis if possible
                    long timeDelta = time1.getTime() - time0.getTime();
                    //log("fps: "+(1000.0/timeDelta));
                    loopTimer.schedule((int) Math.max(10, animationIntervalMillis - timeDelta));
                } else {
                    projection.clearCanvas();
                }
            }
        };
        loopTimer.schedule(animationIntervalMillis);
    }

    private void drawSwarm() {
        Context2d ctxt = canvas.getContext2d();
        ctxt.setGlobalAlpha(0.08);
        ctxt.setGlobalCompositeOperation("destination-out");
        ctxt.setFillStyle("black");
        ctxt.fillRect(0, 0, canvas.getOffsetWidth(), canvas.getOffsetHeight());
        ctxt.setGlobalAlpha(1.0);
        ctxt.setGlobalCompositeOperation("source-over");
        ctxt.setFillStyle("white");
        for (int idx = 0; idx < particles.length; idx++) {
            Particle particle = particles[idx];
            if (particle == null || particle.stepsToLive == 0) {
                continue;
            }
            double particleSpeed = particle.v == null ? 0 : particle.v.length();
            ctxt.setLineWidth(field.getLineWidth(particleSpeed));
            ctxt.setStrokeStyle(field.getColor(particleSpeed));
            ctxt.beginPath();
            ctxt.moveTo(particle.previousPixelCoordinate.x, particle.previousPixelCoordinate.y);
            ctxt.lineTo(particle.currentPixelCoordinate.x, particle.currentPixelCoordinate.y);
            ctxt.stroke();
        }
    }

    /**
     * Moves each particle by its vector {@link Particle#v} multiplied by the speed which is 0.01 times the
     * {@link VectorField#getMotionScale(int)} at the map's current zoom level.
     */
    private boolean execute(Vector diffPx) {
        double speed = field.getMotionScale(map.getZoom());
        for (int idx = 0; idx < particles.length; idx++) {
            Particle particle = particles[idx];
            if (particle != null && particle.stepsToLive > 0 && particle.v != null) {
                // move the particle one step in the direction and with the speed indicated by particle.v and
                // update its currentPosition, currentPixelCoordinate and previousPixelCoordinate fields;
                // also, its particle.v field is updated based on its new position from the vector field
                particle.previousPixelCoordinate = particle.currentPixelCoordinate;
                if (diffPx.x != 0) {
                    particle.previousPixelCoordinate.x += diffPx.x;
                }
                if (diffPx.y != 0) {
                    particle.previousPixelCoordinate.y += diffPx.y;
                }
                double latDeg = particle.currentPosition.getLatDeg() + speed * particle.v.y;
                double lngDeg = particle.currentPosition.getLngDeg() + speed * particle.v.x / cosineOfAverageLatitude;
                particle.currentPosition = new DegreePosition(latDeg, lngDeg);
                particle.currentPixelCoordinate = projection.latlng2pixel(particle.currentPosition);
                particle.stepsToLive--;
                if ((particle.stepsToLive > 0) && (this.field.inBounds(particle.currentPosition))) {
                    particle.v = field.getVector(particle.currentPosition, timePoint);
                } else {
                    particle.v = null;
                }
            } else {
                // particle timed out (age became 0) or was never created (e.g., weight too low); try to create a new one
                particles[idx] = this.createParticle();
            }
        }
        drawSwarm();
        return true;
    }

    public VectorField getField() {
        return field;
    }

    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        timePoint = newTime;
    }
}
