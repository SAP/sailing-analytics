package com.sap.sse.security.ui.client.component;

import java.util.Arrays;

import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.celltable.ImagesBarCell;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.UserActions;
import com.sap.sse.security.ui.client.i18n.StringMessages;

public class DefaultActionsImagesBarCell extends ImagesBarCell {

    public static final String ACTION_DELETE = DefaultActions.DELETE.name();
    public static final String ACTION_UPDATE = DefaultActions.UPDATE.name();
    public static final String ACTION_CHANGE_OWNERSHIP = DefaultActions.CHANGE_OWNERSHIP.name();
    public static final String ACTION_MIGRATE_GROUP_OWNERSHIP_HIERARCHY = "MIGRATE_GROUP_OWNERSHIP_HIERARCHY";
    public static final String ACTION_CHANGE_ACL = DefaultActions.CHANGE_ACL.name();
    public static final String ACTION_MANAGE_LOCK = UserActions.MANAGE_LOCK.name();

    protected final StringMessages stringMessages;

    public DefaultActionsImagesBarCell(final StringMessages stringMessages) {
        this.stringMessages = stringMessages;
    }

    @Override
    protected Iterable<ImageSpec> getImageSpecs() {
        return Arrays.asList(getUpdateImageSpec(), getDeleteImageSpec(), getChangeOwnershipImageSpec(),
                getChangeACLImageSpec(), getResetLockImageSpec());
    }

    /**
     * @return {@link ImageSpec} for {@link DefaultActions#UPDATE update} action
     */
    protected ImageSpec getUpdateImageSpec() {
        return new ImageSpec(ACTION_UPDATE, stringMessages.actionEdit(), IconResources.INSTANCE.editIcon());
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
    
    /**
     * @return {@link ImageSpec} for {@link DefaultActions#CHANGE_OWNERSHIP change ownership} action
     */
    protected ImageSpec getMigrateGroupOwnershipForHierarchyImageSpec() {
        return new ImageSpec(ACTION_MIGRATE_GROUP_OWNERSHIP_HIERARCHY, stringMessages.migrateGroupOwner(),
                IconResources.INSTANCE.changeMigrateOwnershipIcon());
    }

    /**
     * @return {@link ImageSpec} for {@link DefaultActions#CHANGE_ACL change acl} action
     */
    protected ImageSpec getChangeACLImageSpec() {
        return new ImageSpec(ACTION_CHANGE_ACL, stringMessages.actionChangeACL(),
                IconResources.INSTANCE.changeACLIcon());
    }

    /**
     * @return {@link ImageSpec} for {@link DefaultActions#CHANGE_ACL reset lock} action
     */
    protected ImageSpec getResetLockImageSpec() {
        return new ImageSpec(ACTION_MANAGE_LOCK, stringMessages.resetLock(),
                IconResources.INSTANCE.resetLockIcon());
    }

}