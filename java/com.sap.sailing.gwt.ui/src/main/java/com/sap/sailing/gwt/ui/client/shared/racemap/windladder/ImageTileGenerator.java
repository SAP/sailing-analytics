package com.sap.sailing.gwt.ui.client.shared.racemap.windladder;

import java.util.logging.Logger;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;

public class ImageTileGenerator implements WindLadderTileGenerator {
    private static final Logger logger = Logger.getLogger(ImageTileGenerator.class.getName());

    protected final Canvas canvas;
    protected boolean isReady = false;

    public ImageTileGenerator(ImageResource resource, Runnable runWhenReady) {
        canvas = Canvas.createIfSupported();
        if (canvas != null) {
            Image image = new Image(resource);
            image.addLoadHandler(new LoadHandler() {
                @Override
                public void onLoad(LoadEvent event) {
                    canvas.setCoordinateSpaceWidth(image.getWidth());
                    canvas.setCoordinateSpaceHeight(image.getHeight());
                    Context2d ctx = canvas.getContext2d();
                    ctx.drawImage(ImageElement.as(image.getElement()), 0, 0);
                    RootPanel.get().remove(image);
                    isReady = true;
                    logger.info("Wind ladder image loaded successfully");
                    if (runWhenReady != null) {
                        runWhenReady.run();
                    }
                }
            });
            image.setVisible(false);
            RootPanel.get().add(image);
        }
    }

    @Override
    public CanvasElement getTile() {
        return canvas.getCanvasElement();
    }

    public boolean getReady() {
        return isReady;
    }

    public int getWidth() {
        return canvas.getCoordinateSpaceWidth();
    }

    public int getHeight() {
        return canvas.getCoordinateSpaceHeight();
    }
}
