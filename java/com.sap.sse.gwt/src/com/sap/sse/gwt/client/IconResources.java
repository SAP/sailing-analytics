package com.sap.sse.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ClientBundle.Source;

public interface IconResources extends ClientBundle {
    
    public static final IconResources INSTANCE = GWT.create(IconResources.class);

    @Source("images/change-acl.png")
    ImageResource changeACLIcon();

    @Source("images/change-ownership.png")
    ImageResource changeOwnershipIcon();

    @Source("images/migrate-change-ownership.png")
    ImageResource changeMigrateOwnershipIcon();

    @Source("images/remove.png")
    ImageResource removeIcon();
    
    @Source("images/edit.png")
    ImageResource editIcon();
    
    @Source("images/certificates.png")
    ImageResource updateCertificatesIcon();
    
    @Source("images/boat_registrations.png")
    ImageResource boatRegistrations();
}
