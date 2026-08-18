package com.sap.sse.gwt.adminconsole;

import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.celltable.ImagesBarCell;

/**
 * Action cell for replica table rows providing a "Drop connection" action.
 */
public class ReplicaImagesBarCell extends ImagesBarCell {

    public static final String ACTION_DROP = "drop";

    private final StringMessages stringMessages;

    public ReplicaImagesBarCell(final StringMessages stringMessages) {
        this.stringMessages = stringMessages;
    }

    @Override
    protected Iterable<ImageSpec> getImageSpecs() {
        return java.util.Arrays.asList(
                new ImageSpec(ACTION_DROP, stringMessages.dropReplicaConnection(), IconResources.INSTANCE.removeIcon()));
    }
}
