package com.sap.sailing.gwt.ui.client.shared.controls;


/**
 * Simple renderer, that uses the objects toString method to render.
 */
public class SimpleObjectRenderer<T> extends AbstractObjectRenderer<T> {

    @Override
    protected String convertObjectToString(T object) {
        return object.toString();
    }

}
