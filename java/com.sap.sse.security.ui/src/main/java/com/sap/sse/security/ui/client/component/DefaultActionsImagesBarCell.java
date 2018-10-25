package com.sap.sse.security.ui.client.component;

import java.util.Arrays;

import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.celltable.ImagesBarCell;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.i18n.StringMessages;

public class DefaultActionsImagesBarCell extends ImagesBarCell {

    public static final String ACTION_DELETE = DefaultActions.DELETE.name();
    public static final String ACTION_UPDATE = DefaultActions.UPDATE.name();
    public static final String ACTION_CHANGE_OWNERSHIP = DefaultActions.CHANGE_OWNERSHIP.name();

    private final StringMessages stringMessages;

    public DefaultActionsImagesBarCell(final StringMessages stringMessages) {
        this.stringMessages = stringMessages;
    }

    @Override
    protected Iterable<ImageSpec> getImageSpecs() {
        return Arrays.asList(getUpdateImageSpec(), getDeleteImageSpec(), getChangeOwnershipImageSpec());
    }

    /**
     * @return {@link ImageSpec} for {@link DefaultActions#UPDATE update} action
     */
    protected ImageSpec getUpdateImageSpec() {
        return new ImageSpec(ACTION_DELETE, stringMessages.actionEdit(), IconResources.INSTANCE.editIcon());
    }

    /**
     * @return {@link ImageSpec} for {@link DefaultActions#DELETE delete} action
     */
    protected ImageSpec getDeleteImageSpec() {
        return new ImageSpec(ACTION_DELETE, stringMessages.actionRemove(), IconResources.INSTANCE.removeIcon());
    }

    /**
     * @return {@link ImageSpec} for {@link DefaultActions#CHANGE_OWNERSHIP change ownership} action
     */
    protected ImageSpec getChangeOwnershipImageSpec() {
        return new ImageSpec(ACTION_CHANGE_OWNERSHIP, stringMessages.actionChangeOwnership(),
                IconResources.INSTANCE.changeOwnershipIcon());
    }

}